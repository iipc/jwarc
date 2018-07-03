/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * A WARC record describing a subsequent visitation of content previously archived. Typically used to indicate the
 * content had not changed and therefore a duplicate copy of it was not recorded.
 */
public abstract class WarcRevisit extends WarcCaptureRecord implements HasRefersTo {
    /**
     * Revisit profile for when the payload content was the same as determined by a strong digest function.
     */
    URI IDENTICAL_PAYLOAD_DIGEST = URI.create("http://netpreserve.org/warc/1.1/revisit/identical-payload-digest");

    /**
     * Revisit profile for when the server said the content had not changed.
     */
    URI SERVER_NOT_MODIFIED = URI.create("http://netpreserve.org/warc/1.1/revisit/server-not-modified");

    WarcRevisit(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * The target URI of the record this record is a revisit of.
     */
    public Optional<URI> refersToTargetURI() {
        return headers().sole("WARC-Refers-To-Target-URI").map(URI::create);
    }

    /**
     * The date of the record this record is a revisit of.
     */
    public Optional<Instant> refersToDate() {
        return headers().sole("WARC-Refers-To-Date").map(Instant::parse);
    }

    /**
     * The revisit profile explaining why this capture was written as a revisit record. The standard profiles are
     * {@link #IDENTICAL_PAYLOAD_DIGEST} and {@link #SERVER_NOT_MODIFIED}.
     */
    public URI profile() {
        return headers().sole("WARC-Profile").map(URI::create).get();
    }
}
