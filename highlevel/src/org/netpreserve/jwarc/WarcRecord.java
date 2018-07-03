/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;
import org.netpreserve.jwarc.lowlevel.WarcHeaderHandler;
import org.netpreserve.jwarc.lowlevel.WarcHeaderParser;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarcRecord extends Message {
    private static final Map<String, Constructor> constructors = new ConcurrentHashMap<>();
    private final WarcBody warcBody;

    static {
        constructors.put("continuation", WarcContinuation::new);
        constructors.put("conversion", WarcConversion::new);
        constructors.put("metadata", WarcMetadata::new);
        constructors.put("request", WarcRequest::new);
        constructors.put("resource", WarcResource::new);
        constructors.put("response", WarcResponse::new);
        constructors.put("warcinfo", Warcinfo::new);
    }

    WarcRecord(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
        this.warcBody = body;
    }

    @Override
    public WarcBody body() {
        return warcBody;
    }

    /**
     * The globally unique identifier for this record.
     */
    public URI id() {
        return URI.create(headers().sole("WARC-Record-ID").get());
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
     * In the first segment of any record that is completed in one or more later {@link WarcContinuation} records, this
     * parameter is mandatory. Its value there is "1". In a {@link WarcContinuation} record, this parameter is also
     * mandatory. Its value is the sequence number of the current segment in the logical whole record, increasing by
     * 1 in each next segment.
     */
    public Optional<Long> segmentNumber() {
        return headers().sole("WARC-Segment-Number").map(Long::valueOf);
    }


    public static WarcRecord parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        Handler handler = new Handler();
        WarcHeaderParser parser = new WarcHeaderParser(handler);

        while (true) {
            parser.parse(buffer);
            if (parser.isFinished()) break;
            if (parser.isError()) throw new ParsingException("invalid warc file");
            buffer.compact();
            int n = channel.read(buffer);
            if (n < 0) throw new EOFException();
            buffer.flip();
        }

        Headers headers = new Headers(handler.headerMap);
        WarcBody body = new WarcBody(headers, channel, buffer);
        String type = headers.sole("WARC-Type").orElse("unknown");
        return constructors.getOrDefault(type, WarcRecord::new).construct(handler.version, headers, body);
    }

    public static WarcRecord parse(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        return parse(channel, buffer);
    }

    @FunctionalInterface
    public interface Constructor {
        WarcRecord construct(ProtocolVersion version, Headers headers, WarcBody body);
    }

    private static class Handler implements WarcHeaderHandler {
        private Map<String,List<String>> headerMap = new TreeMap<>();
        private String name;
        private ProtocolVersion version;

        @Override
        public void version(ProtocolVersion version) {
            this.version = version;
        }

        @Override
        public void name(String name) {
            this.name = name;
        }

        @Override
        public void value(String value) {
            headerMap.computeIfAbsent(name, name -> new ArrayList<>()).add(value);
        }
    }

    public static abstract class Builder<R extends WarcRecord, B extends Builder<R, B>> extends Message.Builder<R, B> {
        public B recordId(String recordId) {
            return header("WARC-Record-ID", recordId);
        }

        public B date(Instant date) {
            return header("WARC-Date", date.toString());
        }

        public B blockDigest(String algorithm, String value) {
            return blockDigest(new Digest(algorithm, value));
        }

        public B blockDigest(Digest digest) {
            return header("WARC-Block-Digest", digest.toPrefixedBase32());
        }

        public B truncated(TruncationReason truncationReason) {
            return header("WARC-Truncated", truncationReason.name().toLowerCase());
        }
        public B segmentNumber(long segmentNumber) {
            return header("WARC-Segment-Number", String.valueOf(segmentNumber));
        }
    }
}
