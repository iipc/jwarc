/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.Optional;

public abstract class WarcPayload {
    private final BodyChannel body;

    WarcPayload(BodyChannel body) {
        this.body = body;
    }

    public BodyChannel body() {
        return body;
    }

    abstract MediaType type();

    abstract Optional<MediaType> identifiedType();

    abstract Optional<Digest> digest();
}
