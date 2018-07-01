/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

public interface WarcResponse extends WarcRecord, HasConcurrentTo, HasPayload, HasTargetURI,
        HasIPAddress {
    interface Builder extends HasConcurrentTo.Builder<WarcResponse, Builder>,
            HasTargetURI.Builder<WarcResponse, Builder>,
            HasPayload.Builder<WarcResponse, Builder> {
    }
}
