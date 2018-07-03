/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class WarcPayload {
    private final WarcTargetRecord record;

    WarcPayload(WarcTargetRecord record) {
        this.record = record;
    }

    /**
     * Recorded digest of the payload.
     */
    public List<Digest> digests() {
        return record.headers().all("WARC-Payload-Digest").stream().map(Digest::new).collect(toList());
    }

    /**
     * A content-type that was identified by an independent check (not just what the server said).
     */
    public Optional<String> identifiedType() {
        return record.headers().sole("WARC-Identified-Payload-Type");
    }

}
