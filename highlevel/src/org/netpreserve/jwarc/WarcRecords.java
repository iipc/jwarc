/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.HeaderName;
import org.netpreserve.jwarc.lowlevel.WarcHeaders;
import org.netpreserve.jwarc.lowlevel.WarcTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WarcRecords {
    private static Map<String, WarcRecordConstructor> constructors = initConstructors();

    private static Map<String, WarcRecordConstructor> initConstructors() {
        Map<String, WarcRecordConstructor> map = new ConcurrentHashMap<>();
        map.put(WarcTypes.CONTINUATION, WarcContinuationImpl::new);
        map.put(WarcTypes.CONVERSION, WarcConversionImpl::new);
        map.put(WarcTypes.METADATA, WarcMetadataImpl::new);
        map.put(WarcTypes.REQUEST, WarcRequestImpl::new);
        map.put(WarcTypes.RESOURCE, WarcResourceImpl::new);
        map.put(WarcTypes.RESPONSE, WarcResponseImpl::new);
        map.put(WarcTypes.REVISIT, WarcRevisitImpl::new);
        map.put(WarcTypes.WARCINFO, WarcInfoImpl::new);
        return map;
    }

    /**
     * Register a constructor that produces a WarcRecord from a map of headers for use by {@link #fromHeaders(Map)}.
     */
    public static void registerConstructor(String warcType, WarcRecordConstructor constructor) {
        constructors.put(warcType, constructor);
    }

    /**
     * Construct a WarcRecord from a map of headers.
     * <p>
     * Constructors for extension WARC-Type values may be registered using the
     * {@link #registerConstructor(String, WarcRecordConstructor)} method.
     *
     * @throws IllegalArgumentException if the WARC-Type header is unknown or missing
     */
    public static WarcRecord fromHeaders(Map<HeaderName, String> headers) {
        Map<HeaderName, String> copy = new HashMap<>();
        copy.putAll(headers);
        copy = Collections.unmodifiableMap(copy);

        String type = copy.get(WarcHeaders.WARC_TYPE);
        if (type == null) {
            throw new IllegalArgumentException(WarcHeaders.WARC_TYPE + " header is mandatory");
        }

        WarcRecordConstructor constructor = constructors.get(type);
        if (constructor == null) {
            throw new IllegalArgumentException("Unknown " + WarcHeaders.WARC_TYPE + ": " + type);
        }

        return constructor.construct(copy);
    }
}
