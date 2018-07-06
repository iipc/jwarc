/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

public class WarcResource extends WarcCaptureRecord {
    WarcResource(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    public static class Builder extends WarcCaptureRecord.Builder<WarcResource, Builder> {
        protected Builder() {
            super("resource");
        }

        @Override
        public WarcResource build() {
            return build(WarcResource::new);
        }
    }
}
