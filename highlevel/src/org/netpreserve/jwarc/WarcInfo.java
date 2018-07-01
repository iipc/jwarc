/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface WarcInfo extends WarcRecord, HasContentType {
    /**
     * The filename containing this 'warcinfo' record.
     */
    default String getFilename() {
        return getHeaders().get(WarcHeaders.WARC_FILENAME);
    }

    interface Builder extends HasContentType.Builder<WarcConversion, Builder> {
        default Builder getFilename(String filename) {
            return setHeader(WarcHeaders.WARC_FILENAME, filename);
        }
    }
}
