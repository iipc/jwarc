/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

public interface HasContentType extends HasHeaders {
    /**
     * The MIME type (as defined in RFC2045) of the information contained in the record’s block. For example, in HTTP
     * request and response records, this would be 'application/http' as specified in 19.1 of RFC2616
     * (or ‘application/http; msgtype=request’ and ‘application/http; msgtype=response’ respectively). In particular,
     * the content-type is not the value of the HTTP Content-Type header in a HTTP response but a MIME type to
     * describe the full archived HTTP message (hence ‘application/http’ if the block contains request or response
     * headers).
     */
    default String getContentType() {
        return getHeaders().get(WarcHeaders.CONTENT_TYPE);
    }

    interface Builder<R extends HasContentType, B extends Builder<R, B>> extends HasHeaders.Builder<R, B> {
        default B setContentType(String contentType) {
            return setHeader(WarcHeaders.CONTENT_TYPE, contentType);
        }
    }
}
