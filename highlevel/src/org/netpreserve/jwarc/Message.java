/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderField;
import org.netpreserve.jwarc.lowlevel.WarcHeaders;

import java.util.Map;

/**
 * A message consisting of headers and a content block. Forms the basis of protocols and formats like HTTP and WARC.
 */
public interface Message {
    Map<HeaderField, String> getHeaders();

    /**
     * The number of octets in the block or zero if no block is present.
     */
    default long getContentLength() {
        String value = getHeaders().get(WarcHeaders.CONTENT_LENGTH);
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * The MIME type (as defined in RFC2045) of the information contained in the record's block.
     */
    default String getContentType() {
        return getHeaders().get(WarcHeaders.CONTENT_TYPE);
    }

    interface Builder<R extends Message, B extends Builder<R, B>> {
        R build();

        B setHeader(HeaderField header, String value);

        default B setContentLength(long contentLength) {
            return setHeader(WarcHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
        }

        default B setContentType(String contentType) {
            return setHeader(WarcHeaders.CONTENT_TYPE, contentType);
        }
    }
}
