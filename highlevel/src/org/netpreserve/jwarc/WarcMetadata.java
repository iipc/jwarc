/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

public class WarcMetadata extends WarcCaptureRecord {
    WarcMetadata(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    public static class Builder extends WarcCaptureRecord.Builder<WarcMetadata, Builder> {
        protected Builder() {
            super("metadata");
        }

        @Override
        public WarcMetadata build() {
            return build(WarcMetadata::new);
        }
    }
}
