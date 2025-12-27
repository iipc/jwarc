/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes pages.jsonl records.
 * @see <a href="https://specs.webrecorder.net/wacz/latest/#pages-jsonl">WACZ spec</a>
 */
public class JsonPagesWriter implements Closeable {
    private final Writer writer;
    private boolean headerWritten = false;

    public JsonPagesWriter(Writer writer) throws IOException {
        this.writer = writer;
    }

    public void process(WarcReader reader) throws IOException {
        for (WarcRecord record : reader) {
            if (record instanceof WarcResponse || record instanceof WarcResource) {
                WarcCaptureRecord capture = (WarcCaptureRecord) record;
                MediaType type = capture.payloadType();
                if (type.base().equals(MediaType.HTML) || type.base().equals(MediaType.XHTML)) {
                    addPage(capture.target(), capture.date().toString(), null);
                }
            }
        }
    }

    /**
     * Writes the mandatory header line for pages.jsonl.
     */
    private void writeHeader() throws IOException {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("format", "json-pages-1.0");
        header.put("id", "pages");
        header.put("title", "All Pages");
        Json.write(writer, header);
        writer.write("\n");
    }

    /**
     * Adds a page entry to pages.jsonl.
     *
     * @param url   the URL of the page
     * @param ts    the timestamp in RFC3339 format
     * @param title an optional title for the page
     */
    public void addPage(String url, String ts, String title) throws IOException {
        if (!headerWritten) {
            writeHeader();
            headerWritten = true;
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("url", url);
        page.put("ts", ts);
        if (title != null) page.put("title", title);
        // Add other optional fields (id, text, size) as needed
        Json.write(writer, page);
        writer.write("\n");
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public void flush() throws IOException {
        writer.flush();
    }
}
