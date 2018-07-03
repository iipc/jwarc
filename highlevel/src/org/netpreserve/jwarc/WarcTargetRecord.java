/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;

import java.net.URI;
import java.util.Optional;

/**
 * A WARC record associated with some target URI.
 * <p>
 * This class exists solely to differentiate between the {@link Warcinfo} record type and all the other standard
 * record types.
 */
public abstract class WarcTargetRecord extends WarcRecord {
    WarcTargetRecord(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    /**
     * The URI of the original target resource this record holds information about.
     */
    public URI targetURI() {
        return headers().sole("WARC-Target-URI").map(URI::create).get();
    }

    /**
     * The payload object contained by this record or an empty payload if none is present or defined.
     * <p>
     * In a {@link WarcResource} or a {@link WarcResponse} this is the captured content identified by
     * {@link #targetURI()}. In a {@link WarcConversion} it is a converted version of the captured content. When present
     * in a {@link WarcRequest} it is the content body of the captured request (such as form data sent via HTTP POST).
     */
    public WarcPayload payload() {
        return new WarcPayload(this);
    }

    /**
     * The ID of a {@link Warcinfo} record associated with this record.
     * <p>
     * Typically this is present only when the warcinfo is not available from context such as after distributing
     * single records into separate WARC files.
     */
    public Optional<URI> warcinfoID() {
        return headers().sole("WARC-Warcinfo-ID").map(URI::create);
    }

    public static abstract class Builder<R extends WarcTargetRecord, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        public B payloadDigest(Digest payloadDigest) {
            return header("WARC-Payload-Digest", payloadDigest.toPrefixedBase32());
        }

        public B identifiedPayloadType(String identifiedPayloadType) {
            return setHeader("WARC-Identified-Payload-Type", identifiedPayloadType);
        }

        public B warcinfoId(String warcinfoId) {
            return header("WARC-Warcinfo-ID", warcinfoId);
        }

    }
}
