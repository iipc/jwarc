/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.netpreserve.jwarc.*;

/**
 * Writes CDX records.
 */
public class CdxWriter implements Closeable {
    private final Writer writer;
    private CdxFormat format = CdxFormat.CDX11;
    private boolean postAppend = false;
    private Predicate<WarcRecord> recordFilter;
    private Consumer<String> warningHandler;

    public CdxWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Writes a CDX header line.
     */
    public void writeHeaderLine() throws IOException {
        writer.write(" CDX ");
        writer.write(format.legend());
        writer.write('\n');
    }

    /**
     * Writes a CDX record.
     */
    public void write(WarcCaptureRecord capture, String filename, long position, long length) throws IOException {
        write(capture, filename, position, length, null);
    }

    /**
     * Writes a CDX record with an encoded request body appended to the urlkey field.
     * @see CdxRequestEncoder#encode(HttpRequest)
     */
    public void write(WarcCaptureRecord capture, String filename,
                       long position, long length, String encodedRequest) throws IOException {
        if (recordFilter != null && !recordFilter.test(capture)) return;
        String urlKey = null;
        if (encodedRequest != null) {
            String rawUrlKey = capture.target() + (capture.target().contains("?") ? '&' : '?') + encodedRequest;
            urlKey = URIs.toNormalizedSurt(rawUrlKey);
        }
        writer.write(format.format(capture, filename, position, length, urlKey));
        writer.write('\n');
    }

    /**
     * Processes a list of WARC files writing CDX records for each response or resource record.
     */
    public void process(List<Path> warcFiles, boolean useAbsolutePaths) throws IOException {
        for (Path file : warcFiles) {
            try (WarcReader reader = new WarcReader(file)) {
                String filename = (useAbsolutePaths ? file.toAbsolutePath() : file.getFileName()).toString();
                reader.onWarning(message -> emitWarning(filename, reader.position(), message));
                process(reader, filename);
            }
        }
    }

    /**
     * Writes CDX records for each response or resource record in a WARC file.
     */
    public void process(WarcReader reader, String filename) throws IOException {
        WarcRecord record = reader.next().orElse(null);
        while (record != null) {
            try {
                if (((record instanceof WarcResponse || record instanceof WarcResource) &&
                        ((WarcCaptureRecord) record).payload().isPresent()
                        || record instanceof WarcRevisit)) {
                    long position = reader.position();
                    WarcCaptureRecord capture = (WarcCaptureRecord) record;
                    URI id = record.version().getProtocol().equals("ARC") ? null : record.id();

                    // Ensure the HTTP header is parsed before advancing to the next record.
                    // Calling .payload() will do this for response records but as revisits don't have a payload
                    // we need to do it ourselves.
                    if (record instanceof WarcRevisit && record.contentType().base().equals(MediaType.HTTP)) {
                        ((WarcRevisit) record).http();
                    }

                    // advance to the next record, so we can calculate the length
                    record = reader.next().orElse(null);
                    long length = reader.position() - position;

                    // skip records without a date, this often occurs in old ARC files with a corrupt date field
                    if (!capture.headers().first("WARC-Date").isPresent()) {
                        emitWarning(filename, position, "Skipping record due to missing or invalid date");
                        continue;
                    }

                    String encodedRequest = null;
                    if (postAppend) {
                        // check for a corresponding request record
                        while (encodedRequest == null && record instanceof WarcCaptureRecord
                                && ((WarcCaptureRecord) record).concurrentTo().contains(id)) {
                            if (record instanceof WarcRequest) {
                                HttpRequest httpRequest = ((WarcRequest) record).http();
                                encodedRequest = CdxRequestEncoder.encode(httpRequest);
                            }
                            record = reader.next().orElse(null);
                        }
                    }

                    write(capture, filename, position, length, encodedRequest);
                } else {
                    record = reader.next().orElse(null);
                }
            } catch (ParsingException e) {
                emitWarning(filename, reader.position(), "ParsingException: " + e.getMessage());
                record = reader.next().orElse(null);
            }
        }
    }

    private void emitWarning(String filename, long position, String message) {
        if (warningHandler == null) return;
        warningHandler.accept(filename + " (offset " + position + ") " + message);
    }

    /**
     * Records that don't match this filter will not be emitted. Accepts all records if null.
     */
    public void setRecordFilter(Predicate<WarcRecord> recordFilter) {
        this.recordFilter = recordFilter;
    }

    /**
     * Sets the CDX output format to use.
     *
     * @see CdxFormat.Builder
     */
    public void setFormat(CdxFormat format) {
        this.format = format;
    }

    /**
     * Appends an encoded version of the request to the URL key for POST and PUT requests.
     */
    public void setPostAppend(boolean postAppend) {
        this.postAppend = postAppend;
    }

    /**
     * Sets a handler for warnings emitted during processing. Setting it to null disables warnings.
     */
    public void onWarning(Consumer<String> warningHandler) {
        this.warningHandler = warningHandler;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}