/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderField;

import java.util.Map;

class WarcMetadataImpl extends WarcRecordImpl implements WarcMetadata {
    WarcMetadataImpl(Map<HeaderField, String> headers) {
        super(headers);
    }
}
