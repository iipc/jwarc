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
    WarcRecord(MessageVersion version, MessageHeaders headers, MessageBody body) {
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
     * The reason why this record was truncated or {@link WarcTruncationReason#NOT_TRUNCATED}.
     */
    public WarcTruncationReason truncated() {
        return headers().sole("WARC-Truncated")
                .map(value -> WarcTruncationReason.valueOf(value.toUpperCase()))
                .orElse(WarcTruncationReason.NOT_TRUNCATED);
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
    public Optional<WarcDigest> blockDigest() {
        return headers().sole("WARC-Block-Digest").map(WarcDigest::new);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id() + "]";
    }

    @FunctionalInterface
    public interface Constructor<R extends WarcRecord> {
        R construct(MessageVersion version, MessageHeaders headers, MessageBody body);
    }

    public abstract static class AbstractBuilder<R extends WarcRecord, B extends AbstractBuilder<R, B>> extends Message.AbstractBuilder<R, B> {
        private ReadableByteChannel bodyChannel;

        public AbstractBuilder(String type) {
            super(MessageVersion.WARC_1_1);
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
            return blockDigest(new WarcDigest(algorithm, value));
        }

        public B blockDigest(WarcDigest digest) {
            return addHeader("WARC-Block-Digest", digest.toPrefixedBase32());
        }

        public B truncated(WarcTruncationReason truncationReason) {
            if (truncationReason.equals(WarcTruncationReason.NOT_TRUNCATED)) {
                headerMap.remove("WARC-Truncated");
                return (B) this;
            }
            return addHeader("WARC-Truncated", truncationReason.name().toLowerCase());
        }

        public B segmentNumber(long segmentNumber) {
            return addHeader("WARC-Segment-Number", String.valueOf(segmentNumber));
        }

        public B body(MediaType contentType, byte[] contentBytes) {
            return body(contentType, Channels.newChannel(new ByteArrayInputStream(contentBytes)), contentBytes.length);
        }

        public B body(MediaType contentType, ReadableByteChannel channel, long length) {
            setHeader("Content-Type", contentType.toString());
            setHeader("Content-Length", Long.toString(length));
            this.bodyChannel = channel;
            return (B) this;
        }

        protected R build(Constructor<R> constructor) {
            MessageHeaders headers = new MessageHeaders(headerMap);
            long contentLength = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
            return constructor.construct(version, headers, new MessageBody(bodyChannel, ByteBuffer.allocate(0), contentLength));
        }
    }
}
