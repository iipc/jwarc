/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

/**
 * Constants for standard WARC headers.
 */
public final class WarcHeaders {
    public static final HeaderName CONTENT_LENGTH = new HeaderName("Content-Length");
    public static final HeaderName CONTENT_TYPE = new HeaderName("Content-Type");
    public static final HeaderName WARC_BLOCK_DIGEST = new HeaderName("WARC-Block-Digest");
    public static final HeaderName WARC_CONCURRENT_TO = new HeaderName("WARC-Concurrent-To");
    public static final HeaderName WARC_DATE = new HeaderName("WARC-Date");
    public static final HeaderName WARC_FILENAME = new HeaderName("WARC-Filename");
    public static final HeaderName WARC_IDENTIFIED_PAYLOAD_TYPE = new HeaderName("WARC-Identified-Payload-Type");
    public static final HeaderName WARC_IP_ADDRESS = new HeaderName("WARC-IP-Address");
    public static final HeaderName WARC_PAYLOAD_DIGEST = new HeaderName("WARC-Payload-Digest");
    public static final HeaderName WARC_PROFILE = new HeaderName("WARC-Profile");
    public static final HeaderName WARC_RECORD_ID = new HeaderName("WARC-Record-ID");
    public static final HeaderName WARC_REFERS_TO_DATE = new HeaderName("WARC-Refers-To-Date");
    public static final HeaderName WARC_REFERS_TO_TARGET_URI = new HeaderName("WARC-Refers-To-Target-URI");
    public static final HeaderName WARC_REFERS_TO = new HeaderName("WARC-Refers-To");
    public static final HeaderName WARC_SEGMENT_NUMBER = new HeaderName("WARC-Segment-Number");
    public static final HeaderName WARC_SEGMENT_ORIGIN_ID = new HeaderName("WARC-Segment-Origin-ID");
    public static final HeaderName WARC_SEGMENT_TOTAL_LENGTH = new HeaderName("WARC-Segment-Total-Length");
    public static final HeaderName WARC_TARGET_URI = new HeaderName("WARC-Target-URI");
    public static final HeaderName WARC_TRUNCATED = new HeaderName("WARC-Truncated");
    public static final HeaderName WARC_TYPE = new HeaderName("WARC-Type");
    public static final HeaderName WARC_WARCINFO_ID = new HeaderName("WARC-Warcinfo-ID");

    private WarcHeaders() {}
}
