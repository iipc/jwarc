/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

public interface WarcResource extends WarcRecord, HasContentType, HasConcurrentTo, HasPayload, HasTargetURI,
        HasIPAddress {
    interface Builder extends HasContentType.Builder<WarcResource, Builder>,
            HasConcurrentTo.Builder<WarcResource, Builder>,
            HasTargetURI.Builder<WarcResource, Builder>,
            HasPayload.Builder<WarcResource, Builder> {
    }
}
