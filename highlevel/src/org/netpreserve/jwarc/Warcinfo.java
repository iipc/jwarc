/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;

import java.util.Optional;

/**
 * The warcinfo record contains information about the web crawl that generated the records following it.
 */
public class Warcinfo extends WarcRecord {
    Warcinfo(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * The name of the file originally containing this warcinfo record.
     */
    public Optional<String> filename() {
        return headers().sole("WARC-Filename");
    }

    public static abstract class Builder extends WarcRecord.Builder<WarcConversion, Builder> {
        public Builder filename(String filename) {
            return header("WARC-Filename", filename);
        }
    }
}
