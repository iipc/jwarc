/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.MessageVersion;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MessageVersionTest {
    @Test
    public void test() {
        Map<MessageVersion, Integer> map = new HashMap<>();
        map.put(MessageVersion.HTTP_1_0, 10);
        map.put(MessageVersion.HTTP_1_1, 11);
        assertEquals(10, (int) map.get(MessageVersion.HTTP_1_0));
        assertEquals("HTTP", MessageVersion.HTTP_1_0.getProtocol());
        assertEquals(1, MessageVersion.HTTP_1_0.getMajor());
        assertEquals(0, MessageVersion.HTTP_1_0.getMinor());
        assertEquals("HTTP/1.0", MessageVersion.HTTP_1_0.toString());
    }

}