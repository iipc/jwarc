/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpResponse extends HttpMessage {
    private final int status;
    private final String reason;

    HttpResponse(int status, String reason, ProtocolVersion version, Headers headers, BodyChannel body) {
        super(version, headers, body);
        this.status = status;
        this.reason = reason;
    }

    public static HttpResponse parse(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        ParseHandler handler = new ParseHandler();
        HttpParser parser = new HttpParser(handler);
        parser.responseOnly();
        parser.parse(channel, buffer);
        Headers headers = new Headers(handler.headerMap);
        long contentLength = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
        BodyChannel body = new BodyChannel(channel, buffer, contentLength);
        return new HttpResponse(handler.status, handler.reason, handler.version, headers, body);
    }

    /**
     * The 3 digit response status code.
     */
    public int status() {
        return status;
    }

    /**
     * The resposne status reason phrase.
     */
    public String reason() {
        return reason;
    }

    private static class ParseHandler implements HttpParser.Handler {
        Map<String,List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private ProtocolVersion version;
        private String name;
        private int status;
        private String reason;

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
        public void reason(String reason) {
            this.reason = reason;
        }

        @Override
        public void status(int status) {
            this.status = status;
        }

        @Override
        public void target(String target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void method(String method) {
            throw new UnsupportedOperationException();
        }
    }
}
