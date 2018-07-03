/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.*;

import static java.util.Collections.emptyList;

public class Headers {
    private Map<String,List<String>> map;

    Headers(Map<String, List<String>> map) {
        map.replaceAll((name, values) -> Collections.unmodifiableList(values));
        this.map = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the value of a single-valued header field. Throws an exception if there are more than one.
     */
    public Optional<String> sole(String name) {
        List<String> values = all(name);
        if (values.size() > 1) {
            throw new IllegalArgumentException("record has " + values.size() + " " + name + " headers");
        }
        return values.stream().findFirst();
    }

    /**
     * Returns the first value of a header field.
     */
    public Optional<String> first(String name) {
        return all(name).stream().findFirst();
    }

    /**
     * Returns all the values of a header field.
     */
    public List<String> all(String name) {
        return map.getOrDefault(name, emptyList());
    }

    /**
     * Returns a map of header fields to their values.
     */
    public Map<String,List<String>> map() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    private static final boolean[] ILLEGAL = initIllegalLookup();
    private static boolean[] initIllegalLookup() {
        boolean[] illegal = new boolean[256];
        String separators = "()<>@,;:\\\"/[]?={} \t";
        for (int i = 0; i < separators.length(); i++) {
            illegal[separators.charAt(i)] = true;
        }
        for (int i = 0; i < 32; i++) { // control characters
            illegal[i] = true;
        }
        return illegal;
    }
}
