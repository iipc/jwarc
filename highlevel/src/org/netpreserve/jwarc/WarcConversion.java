/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface WarcConversion extends WarcRecord, HasContentType, HasRefersTo, HasPayload, HasTargetURI {
    interface Builder extends HasContentType.Builder<WarcConversion, Builder>,
            HasRefersTo.Builder<WarcConversion, Builder>,
            HasTargetURI.Builder<WarcConversion, Builder>,
            HasPayload.Builder<WarcConversion, Builder> {

        default Builder setSegmentOriginId(String segmentOriginId) {
            return setHeader(WarcHeaders.WARC_SEGMENT_ORIGIN_ID, segmentOriginId);
        }

        default Builder setSegmentTotalLength(long segmentTotalLength) {
            return setHeader(WarcHeaders.WARC_SEGMENT_TOTAL_LENGTH, Long.toString(segmentTotalLength));
        }
    }
}
