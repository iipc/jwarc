/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.Map;

public interface HasHeaders {
    Map<String,String> getHeaders();

    interface Builder<R extends HasHeaders, B extends HasHeaders.Builder<R,B>> {
        R build();

        B setHeader(String header, String value);
    }
}
