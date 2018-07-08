/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class WarcConversionTest {

    final static String warc = "WARC/1.0\r\n" +
            "WARC-Type: conversion\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2016-09-19T19:00:40Z\r\n" +
            "WARC-Record-ID: <urn:uuid:16da6da0-bcdc-49c3-927e-57494593dddd>\r\n" +
            "WARC-Refers-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "WARC-Block-Digest: sha1:XQMRY75YY42ZWC6JAT6KNXKD37F7MOEK\r\n" +
            "Content-Type: image/neoimg\r\n" +
            "Content-Length: 934\r\n" +
            "\r\n" +
            "[image/neoimg binary data here]";

    @Test
    public void test() throws IOException {
        WarcConversion conversion = (WarcConversion) new WarcReader(new ByteArrayInputStream(warc.getBytes(UTF_8))).next().get();
        assertEquals(URI.create("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0"), conversion.refersTo().get());
        assertEquals(934, conversion.body().size());
        assertEquals("image/neoimg", conversion.body().type());
        assertEquals(URI.create("http://www.archive.org/images/logoc.jpg"), conversion.targetURI());
    }

    @Test
    public void builder() throws IOException {
        URI reference = URI.create("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0");
        WarcConversion conversion = new WarcConversion.Builder()
                .refersTo(reference)
                .build();
        assertEquals("conversion", conversion.type());
        assertEquals(reference, conversion.refersTo().get());
    }
}