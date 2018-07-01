/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

/**
 * Constants for standard WARC headers.
 */
public final class WarcHeaders {
    public static final HeaderField CONTENT_LENGTH = new HeaderField("Content-Length");
    public static final HeaderField CONTENT_TYPE = new HeaderField("Content-Type");
    public static final HeaderField WARC_BLOCK_DIGEST = new HeaderField("WARC-Block-Digest");
    public static final HeaderField WARC_CONCURRENT_TO = new HeaderField("WARC-Concurrent-To");
    public static final HeaderField WARC_DATE = new HeaderField("WARC-Date");
    public static final HeaderField WARC_FILENAME = new HeaderField("WARC-Filename");
    public static final HeaderField WARC_IDENTIFIED_PAYLOAD_TYPE = new HeaderField("WARC-Identified-Payload-Type");
    public static final HeaderField WARC_IP_ADDRESS = new HeaderField("WARC-IP-Address");
    public static final HeaderField WARC_PAYLOAD_DIGEST = new HeaderField("WARC-Payload-Digest");
    public static final HeaderField WARC_PROFILE = new HeaderField("WARC-Profile");
    public static final HeaderField WARC_RECORD_ID = new HeaderField("WARC-Record-ID");
    public static final HeaderField WARC_REFERS_TO_DATE = new HeaderField("WARC-Refers-To-Date");
    public static final HeaderField WARC_REFERS_TO_TARGET_URI = new HeaderField("WARC-Refers-To-Target-URI");
    public static final HeaderField WARC_REFERS_TO = new HeaderField("WARC-Refers-To");
    public static final HeaderField WARC_SEGMENT_NUMBER = new HeaderField("WARC-Segment-Number");
    public static final HeaderField WARC_SEGMENT_ORIGIN_ID = new HeaderField("WARC-Segment-Origin-ID");
    public static final HeaderField WARC_SEGMENT_TOTAL_LENGTH = new HeaderField("WARC-Segment-Total-Length");
    public static final HeaderField WARC_TARGET_URI = new HeaderField("WARC-Target-URI");
    public static final HeaderField WARC_TRUNCATED = new HeaderField("WARC-Truncated");
    public static final HeaderField WARC_TYPE = new HeaderField("WARC-Type");
    public static final HeaderField WARC_WARCINFO_ID = new HeaderField("WARC-Warcinfo-ID");

    private WarcHeaders() {}
}
