/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

public interface WarcHeaderHandler {
    void majorVersion(int major);
    void minorVersion(int minor);
    void field(HeaderName field);
    void value(String value);
}
