/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

/**
 * Constants for standard WARC-Type header values.
 */
public final class WarcTypes {
    public static final String CONTINUATION = "continuation";
    public static final String CONVERSION = "conversion";
    public static final String METADATA = "metadata";
    public static final String REQUEST = "request";
    public static final String RESOURCE = "resource";
    public static final String RESPONSE = "response";
    public static final String REVISIT = "revisit";
    public static final String WARCINFO = "warcinfo";

    private WarcTypes() {}
}
