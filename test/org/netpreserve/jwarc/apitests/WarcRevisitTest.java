/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRevisit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WarcRevisitTest {
    final static String warc = "WARC/1.1\r\n" +
            "WARC-Type: revisit\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2017-06-23T12:43:35Z\r\n" +
            "WARC-Profile: http://netpreserve.org/warc/1.1/revisit/server-not-modified\r\n" +
            "WARC-Record-ID: <urn:uuid:16da6da0-bcdc-49c3-927e-57494593bbbb>\r\n" +
            "WARC-Refers-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "WARC-Refers-To-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Refers-To-Date: 2016-09-19T17:20:24Z\r\n" +
            "Content-Type: message/http\r\n" +
            "Content-Length: 202\r\n" +
            "\r\n" +
            "HTTP/1.0 304 Not Modified\r\n" +
            "Date: Tue, 06 Mar 2017 00:43:35 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu) PHP/5.0.5-2ubuntu1.4 Connection: Keep-Alive\r\n" +
            "Keep-Alive: timeout=15, max=100\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "\r\n" +
            "this line should not be read";

    @Test
    public void test() throws IOException {
        WarcRevisit revisit = (WarcRevisit) new WarcReader(new ByteArrayInputStream(warc.getBytes(UTF_8))).next().get();
        assertEquals(WarcRevisit.SERVER_NOT_MODIFIED_1_1, revisit.profile());
        assertEquals(Instant.parse("2016-09-19T17:20:24Z"), revisit.refersToDate().get());
        assertEquals(URI.create("http://www.archive.org/images/logoc.jpg"), revisit.refersToTargetURI().get());
        assertEquals(URI.create("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0"), revisit.refersTo().get());
        assertEquals(304, revisit.http().status());
        assertEquals(Optional.of("timeout=15, max=100"), revisit.http().headers().sole("Keep-Alive"));
        assertFalse(revisit.payload().isPresent());
    }

    @Test
    public void buildingWithoutRefersToRecordId() {
        WarcRevisit revisit = new WarcRevisit.Builder(URI.create("http://example.org/"),
                WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_1)
                .refersTo((URI)null, URI.create("http://example.org/other"), Instant.parse("2016-09-19T17:20:24Z"))
                .build();
        assertEquals(Optional.empty(), revisit.refersTo());
        assertEquals(Optional.of(URI.create("http://example.org/other")), revisit.refersToTargetURI());
        assertEquals(Optional.of(Instant.parse("2016-09-19T17:20:24Z")), revisit.refersToDate());
    }

    @Test
    public void builder() throws IOException {
        URI target = URI.create("http://example.org/");
        Instant date = Instant.now();
        URI reference = URI.create("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0");
        WarcRevisit revisit = new WarcRevisit.Builder(target, WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_1)
                .refersTo(reference, target, date)
                .build();
        assertEquals(WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_1, revisit.profile());
        assertEquals(target, revisit.targetURI());
        assertEquals(date, revisit.refersToDate().get());
        assertEquals(target, revisit.refersToTargetURI().get());
        assertEquals(reference, revisit.refersTo().get());
    }

}