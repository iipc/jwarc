/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Optional;

import static org.netpreserve.jwarc.WarcPayload.createPayload;

public class WarcRequest extends WarcCaptureRecord {

    private HttpRequest http;

    WarcRequest(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * Parses the content body of this record as HTTP request.
     * <p>
     * This is a convenience method for <code>HttpRequest.parse(request.body())</code>.
     */
    public HttpRequest http() throws IOException {
        if (http == null) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            buffer.flip();
            MessageBody body = body();
            if (body.position() != 0) throw new IllegalStateException("http() cannot be called after reading from body");
            try {
                if (body instanceof LengthedBody) {
                    // if we can, save a copy of the raw header and push it back so we don't invalidate body
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    LengthedBody lengthed = (LengthedBody) body;
                    http = HttpRequest.parse(lengthed.discardPushbackOnRead(), buffer, Channels.newChannel(baos));
                    lengthed.pushback(baos.toByteArray());
                } else {
                    http = HttpRequest.parse(body, buffer);
                }
            } catch (ParsingException e) {
                e.recordSource = recordSource;
                throw e;
            }
        }
        return http;
    }

    @Override
    public MediaType payloadType() throws IOException {
        return http().contentType();
    }

    public static class Builder extends AbstractBuilder<WarcRequest, Builder> {
        public Builder(URI targetURI) {
            this(targetURI.toString());
        }

        public Builder(String targetURI) {
            super("request");
            setHeader("WARC-Target-URI", targetURI);
        }

        @Override
        public WarcRequest build() {
            return build(WarcRequest::new);
        }

        public Builder body(HttpRequest httpRequest) throws IOException {
            return body(MediaType.HTTP_REQUEST, httpRequest);
        }
    }

    public Optional<WarcPayload> payload() throws IOException {
        if(!contentType().base().equals(MediaType.HTTP_REQUEST)) {
            return Optional.empty();
        }
        return createPayload(http().body(), http.contentType());
    }
}
