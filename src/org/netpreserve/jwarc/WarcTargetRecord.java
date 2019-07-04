/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * A WARC record associated with some target URI.
 * <p>
 * This class exists solely to differentiate between the {@link Warcinfo} record type and all the other standard
 * record types.
 */
public abstract class WarcTargetRecord extends WarcRecord {
    WarcTargetRecord(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * The URI of the original target resource this record holds information about as an unparsed string.
     */
    public String target() {
        return headers().sole("WARC-Target-URI").get();
    }

    /**
     * The URI of the original target resource this record holds information about.
     */
    public URI targetURI() {
        return headers().sole("WARC-Target-URI").map(URIs::parseLeniently).get();
    }

    /**
     * Digest values that were calculated by applying hash functions to payload.
     */
    public Optional<WarcDigest> payloadDigest() {
        return headers().sole("WARC-Payload-Digest").map(WarcDigest::new);
    }

    /**
     * A content-type that was identified by an independent check (not just what the server said).
     */
    public Optional<MediaType> identifiedPayloadType() {
        return headers().sole("WARC-Identified-Payload-Type").map(MediaType::parse);
    }

    /**
     * Returns the payload of this record if one is present.
     * <p>
     * This method returns an empty optional when the payload is undefined for this record type or if this library does
     * not know how to parse the body in order to extract the payload. If the payload is well defined but
     * happens to be zero bytes in length this method still returns a WarcPayload object.
     */
    public Optional<WarcPayload> payload() throws IOException {
        return Optional.of(new WarcPayload(body()) {
            @Override
            public MediaType type() {
                return contentType();
            }

            @Override
            Optional<MediaType> identifiedType() {
                return Optional.empty();
            }

            @Override
            public Optional<WarcDigest> digest() {
                Optional<WarcDigest> payloadDigest = payloadDigest();
                return payloadDigest.isPresent() ? payloadDigest : blockDigest();
            }
        });
    }

    /**
     * The ID of a {@link Warcinfo} record associated with this record.
     */
    public Optional<URI> warcinfoID() {
        return headers().sole("WARC-Warcinfo-ID").map(WarcRecord::parseRecordID);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + date() + " " + target() + ">";
    }

    public static abstract class Builder<R extends WarcTargetRecord, B extends Builder<R, B>> extends AbstractBuilder<R, B> {
        public Builder(String type) {
            super(type);
        }

        public B payloadDigest(WarcDigest payloadDigest) {
            return addHeader("WARC-Payload-Digest", payloadDigest.prefixedBase32());
        }

        public B identifiedPayloadType(String identifiedPayloadType) {
            return setHeader("WARC-Identified-Payload-Type", identifiedPayloadType);
        }

        public B warcinfoId(URI recordId) {
            return addHeader("WARC-Warcinfo-ID", WarcRecord.formatId(recordId));
        }

        public B payloadDigest(String algorithm, String value) {
            return payloadDigest(new WarcDigest(algorithm, value));
        }
    }
}
