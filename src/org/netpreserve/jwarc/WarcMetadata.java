/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WarcMetadata extends WarcCaptureRecord {
    private Headers fields;

    WarcMetadata(ProtocolVersion version, Headers headers, BodyChannel body) {
        super(version, headers, body);
    }

    /**
     * Metadata records do not have a payload so this method always returns empty.
     */
    @Override
    public Optional<WarcPayload> payload() throws IOException {
        return Optional.empty();
    }

    /**
     * Parses the body as application/warc-fields.
     * <p>
     * This is a convenience method for <code>Headers.parse(metadata.body())</code>.
     */
    public Headers fields() throws IOException {
        if (fields == null) {
            fields = Headers.parse(body());
        }
        return fields;
    }

    public static class Builder extends WarcCaptureRecord.Builder<WarcMetadata, Builder> {
        protected Builder() {
            super("metadata");
        }

        @Override
        public WarcMetadata build() {
            return build(WarcMetadata::new);
        }

        public Builder fields(Map<String, List<String>> map) {
            return body("application/warc-fields", Headers.format(map).getBytes(UTF_8));
        }
    }
}
