/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

/**
 * Constants for standard WARC headers.
 */
public final class WarcHeaders {
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String WARC_BLOCK_DIGEST = "WARC-Block-Digest";
    public static final String WARC_CONCURRENT_TO = "WARC-Concurrent-To";
    public static final String WARC_DATE = "WARC-Date";
    public static final String WARC_FILENAME = "WARC-Filename";
    public static final String WARC_IDENTIFIED_PAYLOAD_TYPE = "WARC-Identified-Payload-Type";
    public static final String WARC_PAYLOAD_DIGEST = "WARC-Payload-Digest";
    public static final String WARC_PROFILE = "WARC-Profile";
    public static final String WARC_RECORD_ID = "WARC-Record-ID";
    public static final String WARC_REFERS_TO_DATE = "WARC-Refers-To-Date";
    public static final String WARC_REFERS_TO_TARGET_URI = "WARC-Refers-To-Target-URI";
    public static final String WARC_REFERS_TO = "WARC-Refers-To";
    public static final String WARC_SEGMENT_NUMBER = "WARC-Segment-Number";
    public static final String WARC_SEGMENT_ORIGIN_ID = "WARC-Segment-Origin-ID";
    public static final String WARC_SEGMENT_TOTAL_LENGTH = "WARC-Segment-Total-Length";
    public static final String WARC_TARGET_URI = "WARC-Target-URI";
    public static final String WARC_TRUNCATED = "WARC-Truncated";
    public static final String WARC_TYPE = "WARC-Type";
    public static final String WARC_WARCINFO_ID = "WARC-Warcinfo-ID";
    public static final String WARC_IP_ADDRESS = "WARC-IP-Address";

    private WarcHeaders() {}
}
