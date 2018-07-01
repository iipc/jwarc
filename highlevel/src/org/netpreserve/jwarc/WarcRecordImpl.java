/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderField;

import java.util.Map;

class WarcRecordImpl implements WarcRecord {
    private final Map<HeaderField,String> headers;

    WarcRecordImpl(Map<HeaderField, String> headers) {
        this.headers = headers;
    }

    @Override
    public Map<HeaderField, String> getHeaders() {
        return headers;
    }
}
