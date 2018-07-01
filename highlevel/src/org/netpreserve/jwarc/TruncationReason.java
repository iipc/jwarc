/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

public enum TruncationReason {
    /**
     * exceeds configured max length
     */
    LENGTH,

    /**
     * exceeds configured max time
     */
    TIME,

    /**
     * network disconnect
     */
    DISCONNECT,

    /**
     * other/unknown reason
     */
    UNSPECIFIED
}
