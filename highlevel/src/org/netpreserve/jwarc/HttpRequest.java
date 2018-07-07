/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.HttpParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpRequest extends HttpMessage {
    private final String method;
    private final String target;

    HttpRequest(String method, String target, ProtocolVersion version, Headers headers, BodyChannel body) {
        super(version, headers, body);
        this.method = method;
        this.target = target;
    }

    public static HttpRequest parse(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        ParseHandler handler = new ParseHandler();
        HttpParser parser = new HttpParser(handler);
        parser.requestOnly();
        parser.parse(channel, buffer);
        Headers headers = new Headers(handler.headerMap);
        BodyChannel body = new BodyChannel(headers, channel, buffer);
        return new HttpRequest(handler.method, handler.target, handler.version, headers, body);
    }

    private static class ParseHandler implements HttpParser.Handler {
        Map<String,List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private ProtocolVersion version;
        private String name;
        private String method;
        private String target;

        @Override
        public void version(int major, int minor) {
            version = new ProtocolVersion("HTTP", major, minor);
        }

        @Override
        public void name(String name) {
            this.name = name;
        }

        @Override
        public void value(String value) {
            headerMap.computeIfAbsent(name, name -> new ArrayList<>()).add(value);
        }

        @Override
        public void method(String method) {
            this.method = method;
        }

        @Override
        public void reason(String reason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void status(int status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void target(String target) {
            this.target = target;
        }
    }
}
