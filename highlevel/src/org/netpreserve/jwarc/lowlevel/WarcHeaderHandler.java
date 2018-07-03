/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

public interface WarcHeaderHandler {
    void version(ProtocolVersion version);
    void name(String name);
    void value(String value);
}
