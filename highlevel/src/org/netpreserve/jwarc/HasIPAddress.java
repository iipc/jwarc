/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasIPAddress extends WarcRecord {
    default String getIPAddress() {
        return getHeaders().get(WarcHeaders.WARC_IP_ADDRESS);
    }
}
