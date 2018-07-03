/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.stream.Collectors;

public class WarcBody extends Body {
    WarcBody(Headers headers, ReadableByteChannel channel, ByteBuffer buffer) {
        super(headers, channel, buffer);
    }

    /**
     * Digest values that were calculated by applying hash functions to this content body.
     */
    public List<Digest> digests() {
        return headers.all("WARC-Block-Digest").stream().map(Digest::new).collect(Collectors.toList());
    }
}
