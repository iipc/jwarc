/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

/**
 * Constants for the standard WARC profile URIs.
 */
public final class WarcProfiles {
    private WarcProfiles() {}

    public static String IDENTICAL_PAYLOAD_DIGEST = "http://netpreserve.org/warc/1.1/revisit/identical-payload-digest";
    public static String SERVER_NOT_MODIFIED = "http://netpreserve.org/warc/1.1/revisit/server-not-modified";
}
