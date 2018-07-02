/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderName;

import java.util.Map;

class WarcRecordImpl implements WarcRecord {
    private final Map<HeaderName,String> headers;

    WarcRecordImpl(Map<HeaderName, String> headers) {
        this.headers = headers;
    }

    @Override
    public Map<HeaderName, String> getHeaders() {
        return headers;
    }
}
