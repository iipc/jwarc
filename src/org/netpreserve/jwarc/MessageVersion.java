/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.Objects;

public final class MessageVersion {
    public static final MessageVersion HTTP_1_0 = new MessageVersion("HTTP", 1, 0);
    public static final MessageVersion HTTP_1_1 = new MessageVersion("HTTP", 1, 1);
    public static final MessageVersion WARC_1_0 = new MessageVersion("WARC", 1, 0);
    public static final MessageVersion WARC_1_1 = new MessageVersion("WARC", 1, 1);
    public static final MessageVersion ARC_1_1 = new MessageVersion("ARC", 1, 1);

    private final String protocol;
    private final int major;
    private final int minor;

    public MessageVersion(String protocol, int major, int minor) {
        this.protocol = protocol;
        this.major = major;
        this.minor = minor;
    }

    public String getProtocol() {
        return protocol;
    }

    void requireProtocol(String expectedProtocol) {
        if (!protocol.equals(expectedProtocol)) {
            throw new IllegalArgumentException("Expected a version of " + expectedProtocol + " but got " + this);
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageVersion that = (MessageVersion) o;
        return major == that.major &&
                minor == that.minor &&
                Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, major, minor);
    }

    @Override
    public String toString() {
        return protocol + "/" + major + "." + minor;
    }
}
