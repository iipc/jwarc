/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.*;

public class WarcRecord extends Message {
    WarcRecord(ProtocolVersion version, Headers headers, BodyChannel body) {
        super(version, headers, body);
    }

    static URI parseRecordID(String uri) {
        if (uri.startsWith("<") && uri.endsWith(">")) {
            uri = uri.substring(1, uri.length() - 1);
        }
        return URI.create(uri);
    }

    private static String formatId(UUID recordId) {
        return "<urn:uuid:" + recordId + ">";
    }

    static String formatId(URI recordId) {
        return "<" + recordId + ">";
    }

    /**
     * The type of record.
     */
    public String type() {
        return headers().sole("WARC-Type").get();
    }

    /**
     * The globally unique identifier for this record.
     */
    public URI id() {
        return parseRecordID(headers().sole("WARC-Record-ID").get());
    }

    /**
     * The instant that data capture for this record began.
     */
    public Instant date() {
        return Instant.parse(headers().sole("WARC-Date").get());
    }

    /**
     * The reason why this record was truncated or {@link TruncationReason#NOT_TRUNCATED}.
     */
    public TruncationReason truncated() {
        return headers().sole("WARC-Truncated")
                .map(value -> TruncationReason.valueOf(value.toUpperCase()))
                .orElse(TruncationReason.NOT_TRUNCATED);
    }

    /**
     * The current record's relative ordering in a sequence of segmented records.
     * <p>
     * Mandatory for all records in the sequence starting from 1.
     */
    public Optional<Long> segmentNumber() {
        return headers().sole("WARC-Segment-Number").map(Long::valueOf);
    }

    /**
     * Digest values that were calculated by applying hash functions to this content body.
     */
    public Optional<Digest> blockDigest() {
        return headers().sole("WARC-Block-Digest").map(Digest::new);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id() + "]";
    }

    @FunctionalInterface
    public interface Constructor<R extends WarcRecord> {
        R construct(ProtocolVersion version, Headers headers, BodyChannel body);
    }

    public abstract static class Builder<R extends WarcRecord, B extends Builder<R, B>> extends Message.Builder<R, B> {
        private Map<String, List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private ProtocolVersion version = ProtocolVersion.WARC_1_0;
        private ReadableByteChannel bodyChannel;

        public Builder(String type) {
            super();
            setHeader("WARC-Type", type);
            setHeader("Content-Length", "0");
            date(Instant.now());
            recordId(UUID.randomUUID());
        }

        public B recordId(UUID uuid) {
            return setHeader("WARC-Record-ID", formatId(uuid));
        }

        public B recordId(URI recordId) {
            return setHeader("WARC-Record-ID", formatId(recordId));
        }

        public B date(Instant date) {
            return setHeader("WARC-Date", date.toString());
        }

        public B blockDigest(String algorithm, String value) {
            return blockDigest(new Digest(algorithm, value));
        }

        public B blockDigest(Digest digest) {
            return addHeader("WARC-Block-Digest", digest.toPrefixedBase32());
        }

        public B truncated(TruncationReason truncationReason) {
            if (truncationReason.equals(TruncationReason.NOT_TRUNCATED)) {
                headerMap.remove("WARC-Truncated");
                return (B) this;
            }
            return addHeader("WARC-Truncated", truncationReason.name().toLowerCase());
        }

        public B segmentNumber(long segmentNumber) {
            return addHeader("WARC-Segment-Number", String.valueOf(segmentNumber));
        }

        @Override
        public B addHeader(String name, String value) {
            headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
            return (B) this;
        }

        @Override
        public B setHeader(String name, String value) {
            List list = new ArrayList();
            list.add(value);
            headerMap.put(name, list);
            return (B) this;
        }

        public B body(String contentType, byte[] contentBytes) {
            setHeader("Content-Type", contentType);
            setHeader("Content-Length", Long.toString(contentBytes.length));
            this.bodyChannel = Channels.newChannel(new ByteArrayInputStream(contentBytes));
            return (B) this;
        }

        protected R build(Constructor<R> constructor) {
            Headers headers = new Headers(headerMap);
            long contentLength = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
            return constructor.construct(version, headers, new BodyChannel(bodyChannel, ByteBuffer.allocate(0), contentLength));
        }
    }
}
