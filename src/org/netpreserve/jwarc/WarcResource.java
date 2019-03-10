/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;

public class WarcResource extends WarcCaptureRecord {
    WarcResource(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    public static class Builder extends AbstractBuilder<WarcResource, Builder> {
        public Builder(URI targetURI) {
            super("resource");
            setHeader("WARC-Target-URI", targetURI.toString());
        }

        public Builder() {
            super("resource");
        }

        @Override
        public WarcResource build() {
            return build(WarcResource::new);
        }
    }
}
