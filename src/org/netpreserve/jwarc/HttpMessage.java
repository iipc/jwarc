/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.IOException;

public abstract class HttpMessage extends Message {
    HttpMessage(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    @Override
    Charset headerCharset() {
        return ISO_8859_1;
    }

    /**
     * The HTTP payload with Content-Encoding decoded.
     *
     * @return a message body with content decoded following the HTTP
     *         Content-Encoding header.
     * @throws IOException
     */
    public MessageBody bodyDecoded() throws IOException {
        MessageBody payload = body();
        List<String> contentEncodings = headers().all("Content-Encoding");
        if (contentEncodings.isEmpty()) {
            return payload;
        } else if (contentEncodings.size() > 1) {
            throw new IOException("Multiple Content-Encodings not supported: " + contentEncodings);
        } else if (contentEncodings.get(0).equalsIgnoreCase("identity")
                || contentEncodings.get(0).equalsIgnoreCase("none")) {
            return payload;
        } else if (contentEncodings.get(0).equalsIgnoreCase("gzip")
                || contentEncodings.get(0).equalsIgnoreCase("x-gzip")) {
            return DecodedBody.create(payload, DecodedBody.Encoding.GZIP);
        } else if (contentEncodings.get(0).equalsIgnoreCase("br")) {
            return DecodedBody.create(payload, DecodedBody.Encoding.BROTLI);
        } else if (contentEncodings.get(0).equalsIgnoreCase("deflate")) {
            return DecodedBody.create(payload, DecodedBody.Encoding.DEFLATE);
        } else if (contentEncodings.get(0).equalsIgnoreCase("zstd")) {
            return DecodedBody.create(payload, DecodedBody.Encoding.ZSTD);
        } else {
            throw new IOException("Content-Encoding not supported: " + contentEncodings.get(0));
        }

    }

    public abstract static class AbstractBuilder<R extends HttpMessage, B extends AbstractBuilder<R, B>> extends Message.AbstractBuilder<R, B> {
        public AbstractBuilder() {
            super(MessageVersion.HTTP_1_1);
        }

        @Override
        public B version(MessageVersion version) {
            version.requireProtocol("HTTP");
            return super.version(version);
        }
    }
}
