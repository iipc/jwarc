/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */
package org.netpreserve.jwarc;

import java.io.OutputStream;
import java.net.URI;


/**
 * Options for fetching a remote resource.
 *
 * @see WarcWriter#fetch(URI, FetchOptions)
 */
public class FetchOptions {
    long maxLength = 0;
    long maxTime = 0;
    int readTimeout = 60000;
    String userAgent = "jwarc";
    OutputStream copyTo;

    /**
     * Stops the fetch after this many bytes are received (including any protocol headers). If this limit was reached
     * the header "WARC-Truncated: length" will be added to the response record.
     */
    public FetchOptions maxLength(long bytes) {
        this.maxLength = bytes;
        return this;
    }

    /**
     * Stops the fetch after this many milliseconds have elapsed. If this limit was reached the header
     * "WARC-Truncated: time" will be added to the response record.
     */
    public FetchOptions maxTime(long millis) {
        this.maxTime = millis;
        return this;
    }


    /**
     * Sets the read timeout in milliseconds on the socket. Defaults to 60000. Set to 0 for no timout.
     *
     * @see java.net.Socket#setSoTimeout(int)
     */
    public FetchOptions readTimeout(int millis) {
        this.readTimeout = millis;
        return this;
    }

    /**
     * Sets the User-Agent request header. Default: "jwarc"
     * <p>
     * If a custom HTTP request is provided this option will be ignored.
     */
    public FetchOptions userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * If specified the response will also be copied to this OutputStream as well as the WARC file.
     */
    public FetchOptions copyTo(OutputStream copyTo) {
        this.copyTo = copyTo;
        return this;
    }
}