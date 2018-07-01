/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

public interface HeaderHandler {
    default void protocolVersion(String protocol, int major, int minor) {}

    default void header(HeaderField header, String value) {}

    default void error(String message) {
        throw new IllegalArgumentException("parse error: " + message);
    }
}
