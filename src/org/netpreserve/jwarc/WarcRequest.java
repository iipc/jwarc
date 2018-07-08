/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;

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
}
