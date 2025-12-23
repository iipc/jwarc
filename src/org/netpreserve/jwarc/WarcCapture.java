/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a capture event in a WARC file, which consists of one or more sequential WARC records linked by
 * WARC-Concurrent-To headers.
 * <p>
 * EXPERIMENTAL API: May change or be removed without notice.
 */
public class WarcCapture {
    private final WarcCaptureReader reader;
    private final ConcurrentRecordSet concurrentSet;
    private final WarcCaptureRecord mainRecord;
    private final List<WarcCaptureRecord> records;
    private final Warcinfo warcinfo;

    WarcCapture(WarcCaptureReader reader, ConcurrentRecordSet concurrentSet, WarcCaptureRecord mainRecord,
                List<WarcCaptureRecord> records, Warcinfo warcinfo) {
        this.reader = reader;
        this.concurrentSet = concurrentSet;
        this.mainRecord = mainRecord;
        this.records = records;
        this.warcinfo = warcinfo;
    }

    public String target() {
        return mainRecord.target();
    }

    public URI targetURI() {
        return mainRecord.targetURI();
    }

    public Instant date() {
        return mainRecord.date();
    }

    /**
     * Returns the HTTP request method, if available.
     */
    public Optional<String> method() {
        try {
            Optional<WarcRequest> request = request();
            if (request.isPresent()) {
                return Optional.of(request.get().http().method());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<HttpResponse> httpResponse() {
        try {
            if (mainRecord instanceof WarcResponse) {
                return Optional.of(((WarcResponse) mainRecord).http());
            } else if (mainRecord instanceof WarcRevisit) {
                return Optional.of(((WarcRevisit) mainRecord).http());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the HTTP response status code, if available.
     */
    public Optional<Integer> status() {
        return httpResponse().map(HttpResponse::status);
    }

    /**
     * Returns the content type of the payload, if available.
     */
    public Optional<MediaType> contentType() {
        try {
            // TODO: support non-HTTP records
            if (mainRecord instanceof WarcResponse) {
                return ((WarcResponse) mainRecord).http().headers()
                        .first("Content-Type").map(MediaType::parseLeniently);
            } else if (mainRecord instanceof WarcRevisit) {
                return ((WarcRevisit) mainRecord).http().headers()
                        .first("Content-Type").map(MediaType::parseLeniently);
            } else if (mainRecord instanceof WarcResource) {
                return mainRecord.headers().first("Content-Type").map(MediaType::parseLeniently);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the main WARC capture record for the current capture event. This will typically be the resource,
     * response or revisit record. If none of these are present, returns the first record in the capture event.
     */
    public WarcCaptureRecord record() throws IOException {
        return mainRecord;
    }

    /**
     * Returns the list of all WARC records associated with this capture event.
     */
    public List<WarcCaptureRecord> records() throws IOException {
        while (true) {
            WarcCaptureRecord record = reader.readConcurrentTo(concurrentSet);
            if (record == null) break;
            records.add(record);
            // TODO: buffer secondary records
        }
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns the first metadata record associated with the current capture event, if present.
     *
     * @return an {@link Optional} containing the WARC metadata record if found, otherwise an empty {@link Optional}.
     * @throws IOException if an error occurs while reading WARC records.
     */
    public Optional<WarcMetadata> metadata() throws IOException {
        return findRecord(WarcMetadata.class);
    }

    /**
     * Returns the first request record associated with the current capture event, if present.
     *
     * @return an {@link Optional} containing the WARC request record if found, otherwise an empty {@link Optional}.
     * @throws IOException if an error occurs while reading WARC records.
     */
    public Optional<WarcRequest> request() throws IOException {
        return findRecord(WarcRequest.class);
    }

    /**
     * Returns the warcinfo record associated with the current capture event, if present.
     */
    public Optional<Warcinfo> warcinfo() throws IOException {
        return Optional.ofNullable(warcinfo);
    }

    private <T extends WarcCaptureRecord> Optional<T> findRecord(Class<T> type) throws IOException {
        for (WarcCaptureRecord record : records) {
            if (type.isInstance(record)) return Optional.of(type.cast(record));
        }
        while (true) {
            WarcCaptureRecord record = reader.readConcurrentTo(concurrentSet);
            records.add(record);
            // TODO: buffer secondary records
            if (record == null) break;
            if (type.isInstance(record)) return Optional.of(type.cast(record));
        }
        return Optional.empty();
    }
}
