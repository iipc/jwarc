/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

/**
 * The result of a fetch operation. This contains the request and response as WARC records (without payloads) so that
 * the request and response headers can be inspected.
 */
public class FetchResult {
    private final WarcRequest request;
    private final WarcResponse response;
    private final Throwable exception;

    FetchResult(WarcRequest request, WarcResponse response, Throwable exception) {
        this.request = request;
        this.response = response;
        this.exception = exception;
    }

    /**
     * The WARC record containing the request that was sent. The request body will not be readable.
     */
    public WarcRequest request() {
        return request;
    }

    /**
     * The WARC record containing the request that was sent. The response body will not be readable.
     */
    public WarcResponse response() {
        return response;
    }

    /**
     * If the fetch was interrupted by an exception but truncated records were still written this will return the caught
     * exception. This can occur if the WarcWriter was closed during the fetch.
     */
    public Throwable exception() {
        return exception;
    }
}
