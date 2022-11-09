/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A WARC record describing a subsequent visitation of content previously archived. Typically used to indicate the
 * content had not changed and therefore a duplicate copy of it was not recorded.
 */
public class WarcRevisit extends WarcCaptureRecord {
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

    private HttpResponse http;

    WarcRevisit(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * Parses the HTTP response captured by this record.
     * <p>
     * This is a convenience method for <code>HttpResponse.parse(revisit.body()))</code>.
     * <p>
     * Note: Revisit records do not have a payload and therefore revisit.http().body() cannot be used.
     */
    public HttpResponse http() throws IOException {
        if (http == null) {
            MessageBody body = body();
            if (body.position() != 0) throw new IllegalStateException("http() cannot be called after reading from body");
            if (body instanceof LengthedBody) {
                // if we can, save a copy of the raw header and push it back so we don't invalidate body
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                LengthedBody lengthed = (LengthedBody) body;
                http = HttpResponse.parseWithoutBody(lengthed.discardPushbackOnRead(), Channels.newChannel(baos));
                lengthed.pushback(baos.toByteArray());
            } else {
                http = HttpResponse.parseWithoutBody(body, null);
            }
        }
        return http;
    }

    /**
     * Always returns Optional.empty().
     */
    @Override
    public Optional<WarcPayload> payload() throws IOException {
        return Optional.empty();
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

    /**
     * The record id of the original response or resource that was revisited.
     */
    public Optional<URI> refersTo() {
        return headers().sole("WARC-Refers-To").map(WarcRecord::parseRecordID);
    }

    public static class Builder extends AbstractBuilder<WarcRevisit, Builder> {
        /**
         * Will be removed. Use new WarcRevisit.Builder(targetURI, profile) instead.
         */
        @Deprecated
        public Builder(URI profile) {
            super("revisit");
            setHeader("WARC-Profile", profile.toString());
        }

        public Builder(URI targetURI, URI profile) {
            super("revisit");
            setHeader("WARC-Target-URI", targetURI.toString());
            setHeader("WARC-Profile", profile.toString());
        }

        @Override
        public WarcRevisit build() {
            return build(WarcRevisit::new);
        }

        public Builder refersTo(URI recordId) {
            return setHeader("WARC-Refers-To", WarcRecord.formatId(recordId));
        }

        public Builder refersTo(UUID recordId) {
            return setHeader("WARC-Refers-To", WarcRecord.formatId(recordId));
        }

        public Builder refersTo(URI recordId, URI targetURI, Instant date) {
            setHeader("WARC-Refers-To-Target-URI", targetURI.toString());
            setHeader("WARC-Refers-To-Date", date.toString());
            return refersTo(recordId);
        }

        public Builder refersTo(UUID recordId, URI targetURI, Instant date) {
            setHeader("WARC-Refers-To-Target-URI", targetURI.toString());
            setHeader("WARC-Refers-To-Date", date.toString());
            return refersTo(recordId);
        }

        public Builder refersTo(URI recordId, String targetURI, Instant date) {
            setHeader("WARC-Refers-To-Target-URI", targetURI);
            setHeader("WARC-Refers-To-Date", date.toString());
            return refersTo(recordId);
        }
    }
}
