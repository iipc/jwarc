/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.util.Optional;

public class WarcResponse extends WarcCaptureRecord {

    private HttpResponse http;

    WarcResponse(ProtocolVersion version, Headers headers, BodyChannel body) {
        super(version, headers, body);
    }

    /**
     * Parses the HTTP response captured by this record.
     * <p/>
     * This is a convenience method for <code>HttpResponse.parse(response.body().channel())</code>.
     */
    public HttpResponse http() throws IOException {
        if (http == null) {
            http = HttpResponse.parse(body());
        }
        return http;
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

    public static class Builder extends WarcCaptureRecord.Builder<WarcResponse, Builder> {
        protected Builder() {
            super("response");
        }

        @Override
        public WarcResponse build() {
            return build(WarcResponse::new);
        }
    }
}
