/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

public class WarcRequest extends WarcCaptureRecord {
    WarcRequest(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * Parses the content body of this record as HTTP request.
     * <p/>
     * This is a convenience method for <code>HttpRequest.parse(request.body().channel())</code>.
     */
    public HttpRequest http() {
        return HttpRequest.parse(body().channel());
    }
}
