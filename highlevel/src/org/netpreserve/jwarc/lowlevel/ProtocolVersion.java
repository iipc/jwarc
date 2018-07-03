/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

import java.util.Objects;

public final class ProtocolVersion {
    public static final ProtocolVersion HTTP_1_0 = new ProtocolVersion("HTTP", 1, 0);
    public static final ProtocolVersion HTTP_1_1 = new ProtocolVersion("HTTP", 1, 1);
    public static final ProtocolVersion WARC_1_0 = new ProtocolVersion("WARC", 1, 0);
    public static final ProtocolVersion WARC_1_1 = new ProtocolVersion("WARC", 1, 1);

    private final String protocol;
    private final int major;
    private final int minor;

    public ProtocolVersion(String protocol, int major, int minor) {
        this.protocol = protocol;
        this.major = major;
        this.minor = minor;
    }

    public String getProtocol() {
        return protocol;
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
        ProtocolVersion that = (ProtocolVersion) o;
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
