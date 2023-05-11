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

    FetchResult(WarcRequest request, WarcResponse response) {
        this.request = request;
        this.response = response;
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
}
