/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class WarcConversion extends WarcTargetRecord implements HasRefersTo {
    WarcConversion(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    @Override
    public Optional<URI> refersTo() {
        return headers().sole("WARC-Refers-To").map(WarcRecord::parseURI);
    }

    public static class Builder extends WarcTargetRecord.Builder<WarcConversion, Builder> {
        public Builder() {
            super("conversion");
        }

        public Builder refersTo(UUID uuid) {
            return addHeader("WARC-Refers-To", WarcRecord.formatId(uuid));
        }

        public Builder refersTo(URI recordId) {
            return addHeader("WARC-Refers-To", WarcRecord.formatId(recordId));
        }

        @Override
        public WarcConversion build() {
            return build(WarcConversion::new);
        }
    }
}
