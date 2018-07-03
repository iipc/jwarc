/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

public class WarcRecords {
//    private static Map<String, WarcRecordConstructor> constructors = initConstructors();
//
//    private static Map<String, WarcRecordConstructor> initConstructors() {
//        Map<String, WarcRecordConstructor> map = new ConcurrentHashMap<>();
//        map.put("continuation", WarcContinuationImpl::new);
//        map.put("conversion", WarcConversionImpl::new);
//        map.put("metadata", WarcMetadataImpl::new);
//        map.put("request", WarcRequestImpl::new);
//        map.put("resource", WarcResourceImpl::new);
//        map.put("response", WarcResponseImpl::new);
//        map.put("revisit", WarcRevisitImpl::new);
//        map.put("warcinfo", WarcInfoImpl::new);
//        return map;
//    }
//
//    /**
//     * Register a constructor that produces a WarcRecord from a map of headers for use by {@link #fromHeaders(Map)}.
//     */
//    public static void registerConstructor(String warcType, WarcRecordConstructor constructor) {
//        constructors.put(warcType, constructor);
//    }
//
//    /**
//     * Construct a WarcRecord from a map of headers.
//     * <p>
//     * Constructors for extension WARC-Type values may be registered using the
//     * {@link #registerConstructor(String, WarcRecordConstructor)} method.
//     *
//     * @throws IllegalArgumentException if the WARC-Type header is unknown or missing
//     */
//    public static WarcRecord fromHeaders(Map<String, List<String>> headers) {
//        /*
//        Map<HeaderName, String> copy = new HashMap<>();
//        copy.putAll(headers);
//        copy = Collections.unmodifiableMap(copy);
//
//        String type = copy.get(WarcHeaders.WARC_TYPE);
//        if (type == null) {
//            throw new IllegalArgumentException(WarcHeaders.WARC_TYPE + " header is mandatory");
//        }
//
//        WarcRecordConstructor constructor = constructors.get(type);
//        if (constructor == null) {
//            throw new IllegalArgumentException("Unknown " + WarcHeaders.WARC_TYPE + ": " + type);
//        }
//
//        return constructor.construct(copy);
//        */
//        return null;
//    }
}
