/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The warcinfo record contains information about the web crawl that generated the records following it.
 */
public class Warcinfo extends WarcRecord {

    private Headers fields;

    Warcinfo(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * The name of the file originally containing this warcinfo record.
     */
    public Optional<String> filename() {
        return headers().sole("WARC-Filename");
    }

    /**
     * Parses the content body as application/warc-fields.
     */
    public Headers fields() throws IOException {
        if (fields == null) {
            fields = Headers.parse(body().channel());
        }
        return fields;
    }

    public static class Builder extends WarcRecord.Builder<Warcinfo,Builder> {
        public Builder() {
            super("warcinfo");
        }

        public Builder filename(String filename) {
            return setHeader("WARC-Filename", filename);
        }

        @Override
        public Warcinfo build() {
            return build(Warcinfo::new);
        }

        public Builder fields(Map<String,List<String>> map) {
            return body("application/warc-fields", Headers.format(map).getBytes(UTF_8));
        }
    }
}
