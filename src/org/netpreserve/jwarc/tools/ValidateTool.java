/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


public class ValidateTool {

    private final static MediaType DNS = MediaType.parse("text/dns");

    private static class Logger {
        protected Optional<StringBuilder> sb;
        private static final ThreadLocal<String> currentFilename = new ThreadLocal<>();

        public Logger() {
            this.sb = Optional.of(new StringBuilder());
        }

        public void setCurrentFilename(String filename) {
            currentFilename.set(filename);
        }

        public void clearCurrentFilename() {
            currentFilename.remove();
        }

        private String getPrefix() {
            String filename = currentFilename.get();
            return filename != null ? filename + ": " : "";
        }

        public void log(String form) {
            sb.ifPresent(s -> s.append("    ").append(form).append('\n'));
        }
        public void log(String form, Object... args) {
            sb.ifPresent(s -> s.append("    ").append(String.format(form, args)).append('\n'));
        }
        public void error(String form, Object... args) {
            if (sb.isPresent()) {
                log("ERROR: " + form, args);
            } else {
                System.err.println(getPrefix() + "ERROR: " + String.format(form, args));
            }
        }
        public void exception(String message, Exception e) {
            if (sb.isPresent()) {
                log("ERROR: %s: %s", message, e);
            } else {
                System.err.println(getPrefix() + "ERROR: " + message + ": " + e);
            }
        }
        public String print() {
            String res = "";
            if (sb.isPresent()) {
                res = sb.get().toString();
                sb.get().setLength(0);
            }
            return res;
        }
    }

    private static class NonVerboseLogger extends Logger {
        public NonVerboseLogger() {
            this.sb = Optional.empty();
        }
    }

    private Logger logger;
    private boolean verbose;
    private HeaderValidator headerValidator;

    public ValidateTool(boolean verbose) {
        this.verbose = verbose;
        if (verbose) {
            logger = new Logger();
        } else {
            logger = new NonVerboseLogger();
        }
    }

    private static long readBody(MessageBody body, Consumer<ByteBuffer> consumer) throws IOException {
        long size = 0;
        int i = 0;
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while ((i = body.read(buffer)) > -1) {
            size += i;
            buffer.flip();
            consumer.accept(buffer);
            buffer.compact();
        }
        return size;
    }

    private static void validateDigest(WarcDigest digestExpected, WarcDigest digestCalculated, long size)
            throws DigestException {
        if (!digestExpected.equals(digestCalculated)) {
            throw new java.security.DigestException("Failed to validate digest: expected " + digestExpected + ", got "
                    + digestCalculated + " (on " + size + " bytes)");
        }
    }

    private static void validateDigest(MessageBody body, WarcDigest digest, AtomicLong consumedBytes)
            throws IOException, NoSuchAlgorithmException, DigestException {
        MessageDigest md = digest.getDigester();
        long size = readBody(body, b -> md.update(b));
        consumedBytes.set(size);
        WarcDigest dig = new WarcDigest(md);
        validateDigest(digest, dig, size);
    }

    private boolean validateCapture(WarcRecord record) throws IOException {
        boolean valid = true;
        int contentLength = -1;
        String targetUri = ((WarcTargetRecord) record).target();
        logger.log(targetUri);
        if (record instanceof WarcResponse && record.contentType().equals(MediaType.HTTP_RESPONSE)) {
            HttpResponse http = ((WarcResponse) record).http();
            logger.log("%s %d %s", http.version(), http.status(), http.reason());
            Optional<String> contentLengthHeader = http.headers().first("content-length");
            if (contentLengthHeader.isPresent()) {
                try {
                    contentLength = Integer.parseInt(contentLengthHeader.get());
                } catch (NumberFormatException e) {
                    logger.error("failed to read HTTP Content-Length header: %s", contentLengthHeader.get());
                    valid = false;
                }
            }
        } else if (record instanceof WarcRequest && record.contentType().equals(MediaType.HTTP_REQUEST)) {
            HttpRequest http = ((WarcRequest) record).http();
            logger.log("%s %s", http.version(), http.method());
        } else if (record instanceof WarcResponse && record.contentType().equals(DNS)) {
            // DNS record
        } else {
            // WarcResource, WarcMetadata, etc. - nothing special to validate
        }
        logger.log("date: %s", record.date());
        Optional<WarcPayload> payload = ((WarcCaptureRecord) record).payload();
        if (payload.isPresent()) {
            MediaType type;
            try {
                type = payload.get().type().base();
                logger.log("payload media type: %s", type);
            } catch (IllegalArgumentException e) {
                logger.exception("Parsing Content-Type failed", e);
                type = MediaType.OCTET_STREAM;
            }
            Optional<WarcDigest> pdigest = payload.get().digest();
            long length = -1;
            if (pdigest.isPresent()) {
                AtomicLong plength = new AtomicLong(length);
                try {
                    validateDigest(payload.get().body(), pdigest.get(), plength);
                    logger.log("payload digest pass");
                } catch (DigestException e) {
                    logger.error("payload digest failed: %s", e.getMessage());
                    valid = false;
                } catch (NoSuchAlgorithmException e) {
                    logger.log("payload digest unknown algorithm: %s", e.getMessage());
                }
                length = plength.get();
            } else {
                length = readBody(payload.get().body(), b -> b.position(b.limit()));
            }
            if (contentLength > 0 && contentLength != length) {
                logger.error("invalid HTTP header Content-Length: %d", contentLength);
                valid = false;
            }
        }
        return valid;
    }

    private boolean validate(WarcReader reader) throws IOException {
        boolean warcValidates = true;
        AtomicBoolean sawWarning = new AtomicBoolean(false);
        reader.onWarning(message -> {
            logger.error(message);
            sawWarning.set(true);
        });
        WarcRecord record = reader.next().orElse(null);
        while (record != null) {
            boolean valid = true;

            if (headerValidator != null) {
                List<String> headerViolations = headerValidator.validate(record.headers());
                headerViolations.forEach(logger::error);
                valid &= headerViolations.isEmpty();
            }

            if (record instanceof WarcCaptureRecord) {
                try {
                    valid = validateCapture(record);
                } catch (IOException e) {
                    // keep going (try at least)
                    logger.exception("Exception during validation", e);
                    valid = false;
                }
            } else {
                /*
                 * no special verification for non-capture record types (WarcContinuation,
                 * WarcConversion), just consume the body
                 */
                record.body().consume();
            }

            if (record.blockDigest().isPresent()) {
                Optional<WarcDigest> calculated = record.calculatedBlockDigest();
                if (calculated.isPresent()) {
                    try {
                        validateDigest(record.blockDigest().get(), calculated.get(),
                                record.body().position());
                        logger.log("block digest pass");
                    } catch (DigestException e) {
                        logger.error("block digest failed: %s", e.getMessage());
                        valid = false;
                    }
                } else {
                    try {
                        record.blockDigest().get().getDigester();
                        logger.log("block digest not calculated (unknown reason)");
                    } catch (NoSuchAlgorithmException e) {
                        logger.log("block digest not calculated: %s", e.getMessage());
                    }
                }
            }

            String recordType = record.type();
            MediaType contentType = record.contentType();
            long position = reader.position();

            if (sawWarning.getAndSet(false)) {
                valid = false;
            }

            record = reader.next().orElse(null);
            long length = reader.position() - position;

            if (verbose) {
                System.out.printf(logger.getPrefix() + "  offset %d (length %d) %s %s\n%s", position, length, recordType, contentType,
                        logger.print());
            } else if (!valid) {
                System.err.printf(logger.getPrefix() + "  offset %d (length %d) %s %s failed\n", position, length, recordType, contentType);
            }

            if (!valid) {
                warcValidates = false;
            }
        }

        if (sawWarning.get()) { // in case of a warning reading the trailer of the last record
            warcValidates = false;
        }

        return warcValidates;
    }

    boolean validate(Path warcFile) {
        logger.setCurrentFilename(warcFile.getFileName().toString());
        try (WarcReader reader = new WarcReader(warcFile)) {
            reader.calculateBlockDigest();
            if (verbose)
                System.out.println("Validating " + warcFile);
            if (!validate(reader)) {
                System.err.println("Failed to validate " + warcFile);
                return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println("Exception validating " + warcFile + ": " + e);
            e.printStackTrace();
            return false;
        } finally {
            logger.clearCurrentFilename();
        }
    }

    private static void usage(int exitValue) {
        System.err.println("");
        System.err.println("ValidateTool [-h] [-v] warc-file...");
        System.err.println("");
        System.err.println("Options:");
        System.err.println("");
        System.err.println(" --no-header-validation\tskips checking headers against WARC standard rules");
        System.err.println(" --forbid-extensions\tdisallows non-standard WARC header fields and values");
        System.err.println(" -j / --threads\tmaximum number of threads to use (default: " + Runtime.getRuntime().availableProcessors() + ")");
        System.err.println(" -h / --help\tshow usage message and exit");
        System.err.println(" -v / --verbose\tlog information about every WARC record to stdout");
        System.err.println("");
        System.err.println("Exit value is 0 if all WARC/ARC files validate, 1 otherwise.");
        System.err.println("Errors and erroneous WARC/ARC records are logged to stderr.");
        System.err.println("");
        System.exit(exitValue);
    }

    public static void main(String[] args) throws Exception {
        int res = 0;
        boolean verbose = false;
        boolean headerValidation = true;
        boolean forbidExtensions = false;
        int threads = Runtime.getRuntime().availableProcessors();
        List<Path> warcFiles = new ArrayList<>();
        if (args.length == 0)
            usage(0);
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--no-header-validation":
                    headerValidation = false;
                    break;
                case "--forbid-extensions":
                    forbidExtensions = true;
                    break;
                case "-h":
                case "--help":
                    usage(0);
                    break;
                case "-j":
                case "--threads":
                    threads = Integer.parseInt(args[++i]);
                    break;
                case "-v":
                case "--verbose":
                    verbose = true;
                    break;
                default:
                    warcFiles.add(Paths.get(args[i]));
            }
        }

        ValidateTool validator = new ValidateTool(verbose);
        if (headerValidation) {
            validator.headerValidator = HeaderValidator.warc_1_1(forbidExtensions);
        }

        ForkJoinPool pool = new ForkJoinPool(threads);
        try {
            res = pool.submit(() -> warcFiles.parallelStream()
                            .map(validator::validate)
                            .anyMatch(valid -> !valid) ? 1 : 0).get();
        } finally {
            pool.shutdown();
        }

        System.exit(res);
    }

}
