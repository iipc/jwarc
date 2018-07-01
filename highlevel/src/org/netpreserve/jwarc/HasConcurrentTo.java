/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasConcurrentTo extends WarcRecord {
    default String getConcurrentTo() {
        return getHeaders().get(WarcHeaders.WARC_CONCURRENT_TO);
    }

    interface Builder<R extends HasConcurrentTo, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        default B setConcurrentTo(String recordId) {
            return setHeader(WarcHeaders.WARC_CONCURRENT_TO, recordId);
        }
    }
}
