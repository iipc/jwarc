/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.MediaType;

import static org.junit.Assert.*;

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
        assertEquals("one", MediaType.parse("text/html;CHARSET=one;charset=two;charset=three").parameters().get("charset"));
    }

    @Test
    public void testParseLeniently() {
        {
            MediaType mediaType = MediaType.parseLeniently("text/html;ISO-8859-1;a\0=2;ok=ok");
            assertFalse(mediaType.isValid());
            assertEquals("text/html;ok=ok", mediaType.toString());
            assertEquals(1, mediaType.parameters().size());
            assertEquals("ok", mediaType.parameters().get("ok"));
            mediaType.raw().equals("text/html;ISO-8859-1;a\0=2;ok=ok");
        }
        assertEquals("bog\0us", MediaType.parseLeniently("bog\0us").toString());
        assertEquals("\0/\0", MediaType.parseLeniently("\0/\0").toString());
        assertEquals("", MediaType.parseLeniently("").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void strictParsingShouldThrow() {
        MediaType.parse("text/html;ISO-8859-1;a\0=2;ok=ok");
    }

}