/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasTargetURI extends WarcRecord {
    /**
     * The WARC-Target-URI is the original URI whose capture gave rise to the information content in this record.
     *
     * In the context of web harvesting, this is the URI that was the target of a crawler’s retrieval request. For a
     * 'revisit' record, it is the URI that was the target of a retrieval request. Indirectly, such as for a 'metadata',
     * or 'conversion' record, it is a copy of the WARC-Target-URI appearing in the original record to which the newer
     * record pertains. The URI in this value shall be written as specified in RFC3986.
     */
    default String getTargetURI() {
        return getHeaders().get(WarcHeaders.WARC_TARGET_URI);
    }

    interface Builder<R extends HasTargetURI, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        default B setTargetURI(String targetURI) {
            return setHeader(WarcHeaders.WARC_TARGET_URI, targetURI);
        }
    }
}
