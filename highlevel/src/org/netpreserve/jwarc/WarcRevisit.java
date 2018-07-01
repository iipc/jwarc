/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

import java.time.Instant;

public interface WarcRevisit extends WarcRecord, HasConcurrentTo, HasRefersTo, HasPayload, HasTargetURI,
        HasIPAddress {
    /**
     * The WARC-Target-URI of a record for which the present record is considered a revisit of.
     */
    default String getRefersToTargetURI() {
        return getHeaders().get(WarcHeaders.WARC_REFERS_TO_TARGET_URI);
    }

    /**
     * The WARC-Date of a record for which the present record is considered a revisit of.
     */
    default Instant getRefersToDate() {
        String value = getHeaders().get(WarcHeaders.WARC_REFERS_TO_DATE);
        return value == null ? null : Instant.parse(value);
    }

    /**
     * A URI signifying the kind of analysis and handling applied in a ‘revisit’ record. (Like an XML namespace, the URI
     * may, but needs not, return human-readable or machine-readable documentation.) If reading software does not
     * recognize the given URI as a supported kind of handling, it shall not attempt to interpret the associated record
     * block.
     */
    default String getProfile() {
        return getHeaders().get(WarcHeaders.WARC_PROFILE);
    }
}
