/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;

public class WarcResponse extends WarcCaptureRecord {
    WarcResponse(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * Parses the HTTP response captured by this record.
     * <p/>
     * This is a convenience method for <code>HttpResponse.parse(response.body().channel())</code>.
     */
    public HttpResponse http() {
        return HttpResponse.parse(body().channel());
    }

    public static abstract class Builder extends WarcCaptureRecord.Builder<WarcResponse, Builder> {
    }
}
