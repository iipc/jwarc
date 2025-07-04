/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia
 */

package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;
import org.netpreserve.jwarc.cdx.CdxReader;
import org.netpreserve.jwarc.cdx.CdxRecord;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class DedupeTool {
    private long minimumSize = 256;
    private String cdxServer;
    private boolean verbose;
    private boolean dryRun;
    private boolean quiet;
    private LruCache<WarcDigest, CacheValue> digestCache;

    private static class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LruCache(int maxSize) {
            super(maxSize, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    private static class CacheValue {
        final URI id;
        final String targetUri;
        final Instant date;

        private CacheValue(URI id, String targetUri, Instant date) {
            this.id = id;
            this.targetUri = targetUri;
            this.date = date;
        }
    }

    /**
     * A WritableByteChannel that discards everything written.
     */
    private static class NullWritableByteChannel implements WritableByteChannel {
        private boolean open = true;

        @Override
        public int write(ByteBuffer src) {
            int remaining = src.remaining();
            src.position(src.limit()); // consume all bytes
            return remaining;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }

    public void deduplicateWarcFile(Path infile, Path outfile) throws IOException {
        long totalRecords = 0;
        long deduplicatedRecords = 0;
        long totalSize = 0;
        long savedSize = 0;

        // We create the WarcWriter on demand so that if no records are deduplicated we don't write an empty
        // gzip member at the end of the file.
        WarcWriter writer = null;

        try (FileChannel input = FileChannel.open(infile);
             WarcReader reader = new WarcReader(input);
             FileChannel output = dryRun ? null : FileChannel.open(outfile, WRITE, CREATE, TRUNCATE_EXISTING)) {

            WarcRecord record = reader.next().orElse(null);
            while (record != null) {
                long position = reader.position();
                WarcRevisit revisit = deduplicate(record);

                record = reader.next().orElse(null);
                long length = reader.position() - position;

                totalRecords++;
                totalSize += length;

                if (revisit == null) {
                    if (verbose) {
                        System.out.println((dryRun ? "Would copy " : "Copying") + position + ":" + length);
                    }
                    if (!dryRun) {
                        transferExactly(input, position, length, output);
                    }
                } else {
                    if (verbose) {
                        System.out.println((dryRun ? "Would write" : "Writing") + " revisit for " + position + ":" + length);
                    }
                    deduplicatedRecords++;

                    if (writer == null) {
                        if (dryRun) {
                            writer = new WarcWriter(new NullWritableByteChannel(), reader.compression());
                        } else {
                            writer = new WarcWriter(output, reader.compression());
                        }
                    }
                    long beforePosition = writer.position();
                    writer.write(revisit);
                    long revisitSize = writer.position() - beforePosition;

                    savedSize += (length - revisitSize);
                }
            }
        } finally {
            if (writer != null) writer.close();
        }

        // Print statistics unless quiet mode is enabled
        if (!quiet) {
            double percentage = totalSize > 0 ? (double) savedSize / totalSize * 100 : 0.0;
            String action = dryRun ? "would dedupe" : "deduped";
            System.out.printf("%s: %s %d/%d records, saving %s/%s (%.2f%%)%n",
                outfile != null ? outfile.getFileName() : infile.getFileName(), action, deduplicatedRecords,
                    totalRecords, formatBytes(savedSize), formatBytes(totalSize), percentage);
        }
    }



    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.2fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2fMB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static void transferExactly(FileChannel input, long position, long length, FileChannel output) throws IOException {
        long transferred = 0;
        while (transferred < length) {
            long n = input.transferTo(position + transferred, length - transferred, output);
            if (n <= 0) {
                throw new IOException("FileChannel.transferTo returned " + n);
            }
            transferred += n;
        }
        if (transferred != length) {
            throw new IOException("Expected to transfer " + length + " but actually transferred " + transferred);
        }
    }

    private WarcRevisit deduplicate(WarcRecord record) throws IOException {
        if (!(record instanceof WarcResponse)) return null;
        WarcResponse response = (WarcResponse) record;
        WarcPayload payload = response.payload().orElse(null);
        if (payload == null || payload.body().size() < minimumSize) return null;
        WarcDigest payloadDigest = response.payloadDigest().orElse(null);
        if (payloadDigest == null) return null;

        // if we have the payload digest in the cache, return a revisit pointing to it
        if (digestCache != null) {
            CacheValue cached = digestCache.get(payloadDigest);
            if (cached != null) {
                return newRevisit(response, cached.id, cached.targetUri, cached.date);
            }
        }

        // now check the CDX server
        if (cdxServer != null) {
            CdxRecord match = findMatchingCdxRecord(response, payloadDigest.base32());
            if (match != null) {
                if (digestCache != null) {
                    digestCache.put(payloadDigest, new CacheValue(null, match.target(), match.date()));
                }
                return newRevisit(response, null, match.target(), match.date());
            }
        }

        // we haven't seen this digest before, so cache it
        if (digestCache != null) {
            digestCache.put(payloadDigest, new CacheValue(response.id(), response.target(), response.date()));
        }

        return null;
    }

    private WarcRevisit newRevisit(WarcResponse response, URI refersTo, String refersToTarget, Instant refersToDate) throws IOException {
        return new WarcRevisit.Builder(response.target(), WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_0)
                .date(response.date())
                .refersTo(refersTo, refersToTarget, refersToDate)
                .body(response.contentType(), response.http().serializeHeader())
                .payloadDigest(response.payloadDigest().orElseThrow(AssertionError::new))
                .build();
    }

    private CdxRecord findMatchingCdxRecord(WarcCaptureRecord capture, String digest) throws IOException {
        URL queryUrl = new URL(cdxServer + "?sort=reverse&rows=10&matchType=exact&url=" + URLEncoder.encode(capture.target(), UTF_8.name()));
        try (CdxReader response = new CdxReader(queryUrl.openStream())) {
            for (CdxRecord record : response) {
                if (digest.equalsIgnoreCase(record.digest())) {
                    return record;
                }
            }
        }
        return null;
    }

    public void setCdxServer(String cdxServer) {
        this.cdxServer = cdxServer;
    }

    public static Path determineOutputPath(Path infile) {
        String[] suffixes = new String[]{".warc.gz", ".warc", ".arc.gz", ".arc"};
        String filename = infile.getFileName().toString();
        Path dir = infile.getParent();
        if (dir == null) dir = Paths.get(".");
        for (String suffix : suffixes) {
            if (filename.endsWith(suffix)) {
                String basename = filename.substring(0, filename.length() - suffix.length());
                return dir.resolve(basename + "-dedup" + suffix);
            }
        }
        return dir.resolve(filename + ".dedup");
    }

    public static void main(String[] args) throws IOException {
        DedupeTool dedupeTool = new DedupeTool();
        List<Path> infiles = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i]) {
                    case "--cache-size":
                        dedupeTool.setCacheSize(Integer.parseInt(args[++i]));
                        break;
                    case "--cdx-server":
                        dedupeTool.setCdxServer(args[++i]);
                        break;
                    case "--minimum-size":
                        dedupeTool.setMinimumSize(Long.parseLong(args[++i]));
                        break;
                    case "-h":
                    case "--help":
                        System.out.println("Usage: jwarc dedupe [options] [warc-files...]");
                        System.out.println();
                        System.out.println("Options:");
                        System.out.println("      --cache-size N        Cache N digests for de-duplication (enables cross-URI de-duplication)");
                        System.out.println("      --cdx-server URL      De-deduplicate against a remote CDX server");
                        System.out.println("      --minimum-size BYTES  Minimum payload size to consider de-duplicating (default " + dedupeTool.minimumSize + ")");
                        System.out.println("  -n, --dry-run             Don't write output, just calculate and print deduplication statistics");
                        System.out.println("  -q, --quiet               Don't print deduplication statistics");
                        System.out.println("  -v, --verbose             Verbose output");
                        return;
                    case "-v":
                    case "--verbose":
                        dedupeTool.setVerbose(true);
                        break;
                    case "-n":
                    case "--dry-run":
                        dedupeTool.setDryRun(true);
                        break;
                    case "-q":
                    case "--quiet":
                        dedupeTool.setQuiet(true);
                        break;
                    default:
                        System.err.println("Unrecognized option: " + args[i]);
                        System.err.println("Try `jwarc dedupe --help` for usage information");
                        System.exit(1);
                        return;
                }
            } else {
                infiles.add(Paths.get(args[i]));
            }
        }

        for (Path infile : infiles) {
            try {
                Path outfile = dedupeTool.dryRun ? null : determineOutputPath(infile);
                dedupeTool.deduplicateWarcFile(infile, outfile);
            } catch (IOException e) {
                System.err.println("Failed to deduplicate " + infile + ": " + e.getMessage());
                if (!dedupeTool.quiet) e.printStackTrace(System.err);
                System.exit(1);
                return;
            }
        }
    }

    public void setCacheSize(int cacheSize) {
        digestCache = cacheSize > 0 ? new LruCache<>(cacheSize) : null;
    }

    public void setMinimumSize(long minimumSize) {
        this.minimumSize = minimumSize;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }
}
