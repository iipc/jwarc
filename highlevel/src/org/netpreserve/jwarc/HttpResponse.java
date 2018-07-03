/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;

import java.nio.channels.ReadableByteChannel;

public abstract class HttpResponse extends HttpMessage {
    HttpResponse(ProtocolVersion version, Headers headers, Body body) {
        super(version, headers, body);
    }

    public static HttpResponse parse(ReadableByteChannel channel) {
        return null;
    }
}
