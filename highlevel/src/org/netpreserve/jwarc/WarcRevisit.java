/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * A WARC record describing a subsequent visitation of content previously archived. Typically used to indicate the
 * content had not changed and therefore a duplicate copy of it was not recorded.
 */
public class WarcRevisit extends WarcCaptureRecord implements HasRefersTo {
    /**
     * WARC 1.0 revisit profile for when the payload content was the same as determined by a strong digest function.
     */
    public static final URI IDENTICAL_PAYLOAD_DIGEST_1_0 = URI.create("http://netpreserve.org/warc/1.0/revisit/identical-payload-digest");

    /**
     * WARC 1.1 revisit profile for when the payload content was the same as determined by a strong digest function.
     */
    public static final URI IDENTICAL_PAYLOAD_DIGEST_1_1 = URI.create("http://netpreserve.org/warc/1.1/revisit/identical-payload-digest");

    /**
     * WARC 1.0 revisit profile for when the server said the content had not changed.
     */
    public static final URI SERVER_NOT_MODIFIED_1_0 = URI.create("http://netpreserve.org/warc/1.0/revisit/server-not-modified");

    /**
     * WARC 1.1 revisit profile for when the server said the content had not changed.
     */
    public static final URI SERVER_NOT_MODIFIED_1_1 = URI.create("http://netpreserve.org/warc/1.1/revisit/server-not-modified");

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
     * {@link #IDENTICAL_PAYLOAD_DIGEST_1_0}, {@link #IDENTICAL_PAYLOAD_DIGEST_1_1}, {@link #SERVER_NOT_MODIFIED_1_0}
     * and {@link #SERVER_NOT_MODIFIED_1_1}.
     */
    public URI profile() {
        return headers().sole("WARC-Profile").map(URI::create).get();
    }

    @Override
    public Optional<URI> refersTo() {
        return headers().sole("WARC-Refers-To").map(WarcRecord::parseURI);
    }

    public static class Builder extends WarcCaptureRecord.Builder<WarcRevisit, Builder> {

        public Builder(URI profile) {
            super("revisit");
            setHeader("WARC-Profile", profile.toString());
        }

        @Override
        public WarcRevisit build() {
            return build(WarcRevisit::new);
        }

        public Builder refersTo(URI recordId) {
            return setHeader("WARC-Refers-To", WarcRecord.formatId(recordId));
        }

        public Builder refersTo(URI recordId, URI targetURI, Instant date) {
            setHeader("WARC-Refers-To-Target-URI", targetURI.toString());
            setHeader("WARC-Refers-To-Date", date.toString());
            return refersTo(recordId);
        }
    }
}
