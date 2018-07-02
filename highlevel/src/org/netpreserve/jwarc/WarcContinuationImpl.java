/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderName;
import org.netpreserve.jwarc.lowlevel.WarcTypes;

import java.util.Map;

class WarcContinuationImpl extends WarcRecordImpl implements WarcContinuation {
    WarcContinuationImpl(Map<HeaderName, String> headers) {
        super(headers);
    }

    static class Builder extends WarcRecordBuilderImpl<WarcContinuation, WarcContinuation.Builder>
            implements WarcContinuation.Builder {
        Builder() {
            super(WarcTypes.CONTINUATION);
        }

        @Override
        public WarcContinuation build() {
            return new WarcContinuationImpl(headers);
        }
    }
}
