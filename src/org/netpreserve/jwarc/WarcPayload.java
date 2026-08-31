/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.Optional;

public abstract class WarcPayload {
    private final MessageBody body;

    WarcPayload(MessageBody body) {
        this.body = body;
    }

    public MessageBody body() {
        return body;
    }

    public abstract MediaType type();

    /**
     * The media type specified for this payload, or empty if no content type was specified.
     */
    public abstract Optional<MediaType> typeHeader();

    abstract Optional<MediaType> identifiedType();

    public abstract Optional<WarcDigest> digest();
}
