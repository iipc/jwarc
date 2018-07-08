/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProtocolVersionTest {
    @Test
    public void test() {
        Map<ProtocolVersion, Integer> map = new HashMap<>();
        map.put(ProtocolVersion.HTTP_1_0, 10);
        map.put(ProtocolVersion.HTTP_1_1, 11);
        assertEquals(10, (int) map.get(ProtocolVersion.HTTP_1_0));
        assertEquals("HTTP", ProtocolVersion.HTTP_1_0.getProtocol());
        assertEquals(1, ProtocolVersion.HTTP_1_0.getMajor());
        assertEquals(0, ProtocolVersion.HTTP_1_0.getMinor());
        assertEquals("HTTP/1.0", ProtocolVersion.HTTP_1_0.toString());
    }

}