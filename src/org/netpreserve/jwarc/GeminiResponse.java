/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class GeminiResponse extends Message {
    private final int status;
    private final String meta;

    public GeminiResponse(int status, String meta, MessageBody body) {
        super(MessageVersion.GEMINI, new MessageHeaders(Collections.emptyMap()), body);
        this.status = status;
        this.meta = meta;
    }

    public static GeminiResponse parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        GeminiParser parser = new GeminiParser();
        parser.strictResponse();
        parser.parse(channel, buffer, null);
        return new GeminiResponse(parser.status(), parser.meta(), LengthedBody.createFromContentLength(channel, buffer, null));
    }

    public int status() {
        return status;
    }

    /**
     * Returns the HTTP equivalent of the status code. (e.g. 20 -&gt; 200, 51 -&gt; 404)
     */
    public int statusHttpEquivalent() {
        switch (status) {
            case 20:
                return 200;
            case 31: // redirect - temporary
                return 307;
            case 32: // redirect - permanent
                return 308;
            case 40: // temporary failure
                return 503;
            case 41: // server unavailable
                return 503;
            case 42: // CGI error
                return 500;
            case 43: // proxy error
                return 502;
            case 44: // slow down
                return 429;
            case 50: // permanent failure
                return 500;
            case 51: // not found
                return 404;
            case 52: // gone
                return 410;
            case 53: // proxy request refused
                return 502;
            case 59: // bad request
                return 400;
            case 60: // client certificate required
                return 401;
            case 61: // certificate not authorized
                return 403;
            case 62: // certificate not valid
                return 403;
            default:
                if (status > 10 && status < 20) { // input
                    return 100;
                } else if (status >= 20 && status < 30) { // success
                    return 200;
                } else if (status >= 30 && status < 40) { // redirect
                    return 307;
                } else if (status >= 60 && status < 70) { // client cert required
                    return 401;
                } else {
                    return 500;
                }
        }
    }

    public String meta() {
        return meta;
    }

    @Override
    public byte[] serializeHeader() {
        return (String.format("%02d", status) + " " + meta + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public MediaType contentType() {
        if (status >= 20 && status < 30) {
            if (meta.isEmpty()) {
                return MediaType.parse("text/gemini; charset=utf-8");
            }
            return MediaType.parse(meta);
        } else {
            return MediaType.OCTET_STREAM;
        }
    }
}
