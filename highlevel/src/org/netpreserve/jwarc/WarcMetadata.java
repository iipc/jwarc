/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

public interface WarcMetadata extends WarcRecord, HasConcurrentTo, HasRefersTo, HasTargetURI,
        HasIPAddress {
    interface Builder extends HasRefersTo.Builder<WarcMetadata, Builder>,
            HasTargetURI.Builder<WarcMetadata, Builder>,
            HasConcurrentTo.Builder<WarcMetadata, Builder> {
    }
}
