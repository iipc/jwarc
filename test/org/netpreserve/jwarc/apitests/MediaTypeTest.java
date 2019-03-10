/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class MediaTypeTest {

    @Test
    public void test() {
        MediaType type = MediaType.parse("text/html;  charset=\"foo\\\" bar\";foo=bar  ;b=c");
        assertEquals("text/html;b=c;charset=\"foo\\\" bar\";foo=bar", type.toString());
        assertEquals("foo\" bar", type.parameters().get("charset"));
        assertEquals("text", type.type());
        assertEquals("html", type.subtype());
        assertEquals("text/html", type.base().toString());
        assertEquals(MediaType.parse("text/html"), MediaType.parse("teXT/htML"));
        assertEquals(MediaType.parse("text/html;charset=utf-8"), MediaType.parse("teXT/htML  ;\tCHARsET=utf-8"));
        assertEquals(MediaType.parse("text/html;charset=utf-8").hashCode(), MediaType.parse("teXT/htML  ;\tCHARsET=utf-8").hashCode());
        assertNotEquals(MediaType.parse("text/html;chartset=utf-8"), MediaType.parse("text/html;chartset=UTF-8"));
        assertEquals(MediaType.parse("text/html"), MediaType.parse("teXT/htML  ;\tCHARsET=utf-8").base());
        assertTrue(type.base().parameters().isEmpty());
    }

}