/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderName;
import org.netpreserve.jwarc.lowlevel.WarcHeaders;

import java.util.HashMap;
import java.util.Map;

public abstract class WarcRecordBuilderImpl<R extends WarcRecord, B extends WarcRecord.Builder<R, B>> implements WarcRecord.Builder<R, B> {
    protected Map<HeaderName, String> headers = new HashMap<>();

    WarcRecordBuilderImpl(String warcType) {
        headers.put(WarcHeaders.WARC_TYPE, warcType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public B setHeader(HeaderName header, String value) {
        headers.put(header, value);
        return (B) this;
    }
}
