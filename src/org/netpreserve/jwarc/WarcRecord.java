/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;

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

    static String formatId(UUID recordId) {
        return recordId == null ? null : "<urn:uuid:" + recordId + ">";
    }

    static String formatId(URI recordId) {
        return recordId == null ? null : "<" + recordId + ">";
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
     * Digest created from "WARC-Block-Digest" header containing digest values that
     * were calculated by applying hash functions to this content body during
     * creation of the WARC file.
     */
    public Optional<WarcDigest> blockDigest() {
        return headers().sole("WARC-Block-Digest").map(WarcDigest::new);
    }

    /**
     * Digest created while reading the WARC record body if the WARC reader is
     * configured to calculate digests and a "WARC-Block-Digest" header is present,
     * see {@link WarcReader#calculateBlockDigest()}.
     * 
     * Note: Calling this method will consume the record body exhaustively, it
     * should be called after the record block or payload has been processed.
     */
    public Optional<WarcDigest> calculatedBlockDigest() throws IOException {
        if (body() instanceof DigestingMessageBody) {
            body().consume();
            return Optional.of(new WarcDigest(((DigestingMessageBody) body()).getDigest()));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id() + "]";
    }

    @FunctionalInterface
    public interface Constructor<R extends WarcRecord> {
        R construct(MessageVersion version, MessageHeaders headers, MessageBody body);
    }

    @SuppressWarnings("unchecked")
    public abstract static class AbstractBuilder<R extends WarcRecord, B extends AbstractBuilder<R, B>> extends Message.AbstractBuilder<R, B> {
        private Instant date;

        public AbstractBuilder(String type) {
            super(MessageVersion.WARC_1_0);
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

        /**
         * The instant that data capture for this record began. Note that WARC/1.0 does not support subsecond precision
         * and the supplied date will be truncated accordingly. Set version to WARC/1.1 or newer if you need millisecond
         * precision.
         */
        public B date(Instant date) {
            this.date = date;
            return (B) this;
        }

        public B blockDigest(String algorithm, String value) {
            return blockDigest(new WarcDigest(algorithm, value));
        }

        public B blockDigest(WarcDigest digest) {
            return addHeader("WARC-Block-Digest", digest.prefixedBase32());
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

        protected R build(Constructor<R> constructor) {
            if (date != null) {
                if (version.equals(MessageVersion.WARC_1_0)) {
                    date = date.truncatedTo(SECONDS);
                }
                setHeader("WARC-Date", date.toString());
            }
            MessageHeaders headers = new MessageHeaders(headerMap);
            return constructor.construct(version, headers, makeBody());
        }
    }
}
