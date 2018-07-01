/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

import java.util.Map;
import java.util.TreeMap;

public abstract class WarcRecordBuilderImpl<R extends WarcRecord, B extends WarcRecord.Builder<R, B>> implements WarcRecord.Builder<R, B> {
    protected Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    WarcRecordBuilderImpl(String warcType) {
        headers.put(WarcHeaders.WARC_TYPE, warcType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public B setHeader(String header, String value) {
        headers.put(header, value);
        return (B) this;
    }
}
