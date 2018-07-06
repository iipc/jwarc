/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.WarcHeaderParser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WarcRecords {
    private static final Map<String, WarcRecord.Constructor> constructors = new ConcurrentHashMap<>();

    static {
        constructors.put("continuation", WarcContinuation::new);
        constructors.put("conversion", WarcConversion::new);
        constructors.put("metadata", WarcMetadata::new);
        constructors.put("request", WarcRequest::new);
        constructors.put("resource", WarcResource::new);
        constructors.put("response", WarcResponse::new);
        constructors.put("revisit", WarcRevisit::new);
        constructors.put("warcinfo", Warcinfo::new);
    }

    public static WarcRecord parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        MapBuildingHandler handler = new MapBuildingHandler();
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

    public static WarcRecord parse(InputStream stream) throws IOException {
        return parse(Channels.newChannel(stream));
    }

}
