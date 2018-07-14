/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.util.Optional;

public class WarcRequest extends WarcCaptureRecord {

    private HttpRequest http;

    WarcRequest(ProtocolVersion version, Headers headers, BodyChannel body) {
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

    public static class Builder extends WarcCaptureRecord.Builder<WarcRequest, Builder> {
        protected Builder() {
            super("request");
        }

        @Override
        public WarcRequest build() {
            return build(WarcRequest::new);
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
                Optional<Digest> digest() {
                    return payloadDigest();
                }
            });
        }
        return Optional.empty();
    }
}
