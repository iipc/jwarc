/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class GeminiRequest {
    private final String url;

    public GeminiRequest(String url) {
        this.url = url;
    }

    public static GeminiRequest parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        GeminiParser parser = new GeminiParser();
        parser.strictRequest();
        parser.parse(channel, buffer, null);
        return new GeminiRequest(parser.url());
    }
}
