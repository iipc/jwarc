/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasRefersTo extends WarcRecord {
    /**
     * The record id of a single record for which the present record holds additional content.
     *
     * The WARC-Refers-To field may be used to associate a 'metadata' record to another record it describes. The
     * WARC-Refers-To field may also be used to associate a record of type 'revisit' or 'conversion' with the preceding
     * record which helped determine the present record content.
     */
    default String getRefersTo() {
        return getHeaders().get(WarcHeaders.WARC_REFERS_TO);
    }

    interface Builder<R extends HasRefersTo, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        default B setRefersTo(String refersTo) {
            return setHeader(WarcHeaders.WARC_REFERS_TO, refersTo);
        }
    }
}
