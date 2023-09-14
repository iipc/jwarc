/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class HttpRequest extends HttpMessage {
    private final String method;
    private final String target;

    HttpRequest(String method, String target, MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
        this.method = method;
        this.target = target;
    }

    public String target() {
        return target;
    }

    public String method() {
        return method;
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

    /**
     * Parses a HTTP request while leniently allowing common deviations from the standard.
     */
    public static HttpRequest parse(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        return parse(channel, buffer);
    }

    /**
     * Parses a HTTP request while leniently allowing common deviations from the standard.
     */
    public static HttpRequest parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        return parse(channel, buffer, null);
    }

    /**
     * Parses a HTTP request while strictly rejecting deviations from the standard.
     */
    public static HttpRequest parseStrictly(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        return parse(channel, buffer, null, true);
    }

    static HttpRequest parse(ReadableByteChannel channel, ByteBuffer buffer, WritableByteChannel copyTo) throws IOException {
        return parse(channel, buffer, copyTo, false);
    }

    private static HttpRequest parse(ReadableByteChannel channel, ByteBuffer buffer, WritableByteChannel copyTo, boolean strict) throws IOException {
        HttpParser parser = new HttpParser();
        if (strict) {
            parser.strictRequest();
        } else {
            parser.lenientRequest();
        }
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        parser.parse(channel, buffer, Channels.newChannel(headerBuffer));
        byte[] headerBytes = headerBuffer.toByteArray();
        if (headerBytes.length == 0) throw new EOFException();
        if (copyTo != null) {
            copyTo.write(ByteBuffer.wrap(headerBytes));
            copyTo.write(buffer.duplicate());
        }
        MessageHeaders headers = parser.headers();
        Long contentLength;
        try {
            contentLength = headers.first("Content-Length").map(Long::parseLong).orElse(null);
        } catch (NumberFormatException e) {
            if (strict) throw new IOException("Invalid Content-Length header", e);
            contentLength = null;
        }
        LengthedBody body = LengthedBody.createFromContentLength(channel, buffer, contentLength);
        HttpRequest request = new HttpRequest(parser.method(), parser.target(), parser.version(), headers, body);
        request.serializedHeader = headerBytes;
        return request;
    }

    public static class Builder extends AbstractBuilder<HttpRequest, Builder> {
        private final String method;
        private final String target;

        public Builder(String method, String target) {
            super();
            this.method = method;
            this.target = target;
        }

        /**
         * Create a new HTTP request builder from a URI.
         * <p>
         * The request target will be set to the path and query of the URI.
         * The Host header will be set to the host and port of the URI.
         */
        public Builder(String method, URI uri) {
            this(method,  uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery());
            setHeader("Host", uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort());
        }

        public HttpRequest build() {
            return new HttpRequest(method, target, version, new MessageHeaders(headerMap), makeBody());
        }
    }
}
