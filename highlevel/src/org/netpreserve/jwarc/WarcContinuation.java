/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface WarcContinuation extends WarcRecord, HasPayload, HasTargetURI {
    /**
     * Identifies the starting record in a series of segmented records whose content blocks are reassembled to obtain a
     * logically complete content block.
     */
    default String getSegmentOriginId() {
        return getHeaders().get(WarcHeaders.WARC_SEGMENT_ORIGIN_ID);
    }

    /**
     * In the final record of a segmented series, the WARC-Segment-Total-Length reports the total length of all segment
     * content blocks when concatenated together.
     */
    default Long getSegmentTotalLength() {
        String value = getHeaders().get(WarcHeaders.WARC_SEGMENT_TOTAL_LENGTH);
        return value == null ? null : Long.valueOf(value);
    }

    static Builder builder() {
        return new WarcContinuationImpl.Builder();
    }

    interface Builder extends HasTargetURI.Builder<WarcContinuation, Builder> {
        default Builder setSegmentOriginId(String segmentOriginId) {
            return setHeader(WarcHeaders.WARC_SEGMENT_ORIGIN_ID, segmentOriginId);
        }

        default Builder setSegmentTotalLength(long segmentTotalLength) {
            return setHeader(WarcHeaders.WARC_SEGMENT_TOTAL_LENGTH, Long.toString(segmentTotalLength));
        }
    }
}
