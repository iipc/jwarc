/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpResponse extends HttpMessage {
    private final int status;
    private final String reason;

    HttpResponse(int status, String reason, MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
        this.status = status;
        this.reason = reason;
    }

    @Override
    void serializeHeaderTo(Appendable output) throws IOException {
        output.append(version().toString());
        output.append(' ');
        output.append(Integer.toString(status));
        output.append(' ');
        output.append(reason);
        output.append("\r\n");
        headers().appendTo(output);
        output.append("\r\n");
    }

    public static HttpResponse parse(ReadableByteChannel channel) throws IOException {
        return parse(channel, null);
    }

    static HttpResponse parse(ReadableByteChannel channel, WritableByteChannel copyTo) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        HttpParser parser = new HttpParser();
        parser.responseOnly();
        parser.parse(channel, buffer, copyTo);
        copyTo.write(buffer);
        MessageHeaders headers = parser.headers();
        long contentLength;
        MessageBody body;
        if (headers.sole("Transfer-Encoding").orElse("").equalsIgnoreCase("chunked")) {
            body = new ChunkedBody(channel, buffer);
        } else {
            if (channel instanceof LengthedBody) {
                LengthedBody lengthed = (LengthedBody) channel;
                contentLength = lengthed.size() - lengthed.position() + buffer.remaining();
            } else {
                contentLength = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
            }
            body = LengthedBody.create(channel, buffer, contentLength);
        }
        return new HttpResponse(parser.status(), parser.reason(), parser.version(), headers, body);
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

    public static class Builder extends HttpMessage.AbstractBuilder<HttpResponse, Builder> {
        private final int status;
        private final String reasonPhrase;

        public Builder(int status, String reasonPhrase) {
            super();
            this.status = status;
            this.reasonPhrase = reasonPhrase;
        }

        @Override
        public HttpResponse build() {
            return new HttpResponse(status, reasonPhrase, version, new MessageHeaders(headerMap), makeBody());
        }
    }
}
