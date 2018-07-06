/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.WarcHeaderParser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarcRecords {
    static final Map<String, WarcRecord.Constructor> constructors = new ConcurrentHashMap<>();

    static URI parseURI(String uri) {
        if (uri.startsWith("<") && uri.endsWith(">")) {
            uri = uri.substring(1, uri.length() - 1);
        }
        return URI.create(uri);
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

    static String formatId(UUID recordId) {
        return "<urn:uuid:" + recordId + ">";
    }

    static String formatId(URI recordId) {
        return "<" + recordId + ">";
    }
}
