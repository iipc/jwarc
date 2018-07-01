/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasPayload extends HasTargetURI {
    /**
     * An optional parameter indicating the algorithm name and calculated value of a digest applied to the payload
     * referred to or contained by the record, which is not necessarily equivalent to the record block.
     *
     * The payload of an application/http block is its 'entity-body' (specified in RFC2616).
     */
    default Digest getPayloadDigest() {
        String value = getHeaders().get(WarcHeaders.WARC_PAYLOAD_DIGEST);
        return value == null ? null : new Digest(value);
    }

    /**
     * The content-type of the record’s payload as determined by an independent check. This string shall not be arrived
     * at by blindly promoting an HTTP Content-Type value up from a record block into the WARC header without direct
     * analysis of the payload, as such values may often be unreliable.
     */
    default String getIdentifiedPayloadType() {
        return getHeaders().get(WarcHeaders.WARC_IDENTIFIED_PAYLOAD_TYPE);
    }

    interface Builder<R extends HasPayload, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        default B setPayloadDigest(String algorithm, String digest) {
            return setPayloadDigest(new Digest(algorithm, digest));
        }

        default B setPayloadDigest(Digest payloadDigest) {
            return setHeader(WarcHeaders.WARC_PAYLOAD_DIGEST, payloadDigest.toPrefixedBase32());
        }

        default B setIdentifiedPayloadType(String identifiedPayloadType) {
            return setHeader(WarcHeaders.WARC_IDENTIFIED_PAYLOAD_TYPE, identifiedPayloadType);
        }
    }
}
