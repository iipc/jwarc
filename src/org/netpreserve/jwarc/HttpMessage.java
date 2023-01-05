/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public abstract class HttpMessage extends Message {
    HttpMessage(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    @Override
    Charset headerCharset() {
        return ISO_8859_1;
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
