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

public class HttpRequest extends HttpMessage {
    private final String method;
    private final String target;

    HttpRequest(String method, String target, MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
        this.method = method;
        this.target = target;
    }

    @Override
    void serializeHeaderTo(Appendable output) throws IOException {
        output.append(method);
        output.append(' ');
        output.append(target);
        output.append(' ');
        output.append(version().toString());
        output.append("\r\n");
        headers().appendTo(output);
        output.append("\r\n");
    }

    public static HttpRequest parse(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        ParseHandler handler = new ParseHandler();
        HttpParser parser = new HttpParser(handler);
        parser.requestOnly();
        parser.parse(channel, buffer);
        MessageHeaders headers = new MessageHeaders(handler.headerMap);
        // FIXME: should use remaining bytes of warc body instead
        long contentLength = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
        MessageBody body = new MessageBody(channel, buffer, contentLength);
        return new HttpRequest(handler.method, handler.target, handler.version, headers, body);
    }

    private static class ParseHandler implements HttpParser.Handler {
        Map<String,List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private MessageVersion version;
        private String name;
        private String method;
        private String target;

        @Override
        public void version(int major, int minor) {
            version = new MessageVersion("HTTP", major, minor);
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

    public static class Builder extends AbstractBuilder<HttpRequest, Builder> {
        private final String method;
        private final String target;

        public Builder(String method, String target) {
            super();
            this.method = method;
            this.target = target;
        }

        public HttpRequest build() {
            return new HttpRequest(method, target, version, new MessageHeaders(headerMap), body);
        }
    }
}
