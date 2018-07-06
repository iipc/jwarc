/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.WarcParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class MapBuildingHandler implements WarcParser.Handler {
    Map<String,List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private String name;
    ProtocolVersion version;

    @Override
    public void version(int major, int minor) {
        version = new ProtocolVersion("WARC", major, minor);
    }

    @Override
    public void name(String name) {
        this.name = name;
    }

    @Override
    public void value(String value) {
        headerMap.computeIfAbsent(name, name -> new ArrayList<>()).add(value);
    }
}
