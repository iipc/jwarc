/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WarcMetadata extends WarcCaptureRecord {
    private MessageHeaders fields;

    WarcMetadata(MessageVersion version, MessageHeaders headers, MessageBody body) {
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
    public MessageHeaders fields() throws IOException {
        if (fields == null) {
            fields = MessageHeaders.parse(body());
        }
        return fields;
    }

    public static class Builder extends AbstractBuilder<WarcMetadata, Builder> {
        public Builder() {
            super("metadata");
        }

        @Override
        public WarcMetadata build() {
            return build(WarcMetadata::new);
        }

        public Builder fields(Map<String, List<String>> map) {
            return body(MediaType.WARC_FIELDS, MessageHeaders.format(map).getBytes(UTF_8));
        }

        public Builder targetURI(String uri) {
            addHeader("WARC-Target-URI", uri);
            return this;
        }

        public Builder targetURI(URI uri) {
            return targetURI(uri.toString());
        }
    }
}
