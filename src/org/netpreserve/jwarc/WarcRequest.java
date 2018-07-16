/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class WarcRequest extends WarcCaptureRecord {

    private HttpRequest http;

    WarcRequest(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * Parses the content body of this record as HTTP request.
     * <p/>
     * This is a convenience method for <code>HttpRequest.parse(request.body())</code>.
     */
    public HttpRequest http() throws IOException {
        if (http == null) {
            http = HttpRequest.parse(body());
        }
        return http;
    }

    public static class Builder extends AbstractBuilder<WarcRequest, Builder> {
        public Builder(URI targetURI) {
            super("request");
            setHeader("WARC-Target-URI", targetURI.toString());
        }

        @Override
        public WarcRequest build() {
            return build(WarcRequest::new);
        }

        public Builder body(HttpRequest httpRequest) {
            return body(MediaType.HTTP_REQUEST, httpRequest);
        }
    }

    public Optional<WarcPayload> payload() throws IOException {
        if (contentType().base().equals(MediaType.HTTP)) {
            return Optional.of(new WarcPayload(http().body()) {

                @Override
                MediaType type() {
                    return http.contentType();
                }

                @Override
                Optional<MediaType> identifiedType() {
                    return identifiedPayloadType();
                }

                @Override
                Optional<WarcDigest> digest() {
                    return payloadDigest();
                }
            });
        }
        return Optional.empty();
    }
}
