/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ArcTest {

    @Test
    public void test() throws IOException {
        String raw = "filedesc://example.arc 0.0.0.0 20050614070144 text/plain 1338\n" +
                "1 1 InternetArchive\n" +
                "URL IP-address Archive-date Content-type Archive-length\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<arcmetadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:arc=\"http://archive.org/arc/1.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://archive.org/arc/1.0/\" xsi:schemaLocation=\"http://archive.org/arc/1.0/ http://www.archive.org/arc/1.0/arc.xsd\">\n" +
                "<arc:software>Heritrix 1.5.0-200506132127 http://crawler.archive.org</arc:software>\n" +
                "<arc:hostname>example.org</arc:hostname>\n" +
                "<arc:ip>127.0.0.1</arc:ip>\n" +
                "<dcterms:isPartOf>CRAWL</dcterms:isPartOf>\n" +
                "<dc:description>Example crawl</dc:description>\n" +
                "<arc:operator>Example</arc:operator>\n" +
                "<dc:publisher>Example</dc:publisher>\n" +
                "<dcterms:audience>Example</dcterms:audience>\n" +
                "<ns0:date xmlns:ns0=\"http://purl.org/dc/elements/1.1/\" xsi:type=\"dcterms:W3CDTF\">2005-06-14T06:37:49+00:00</ns0:date>\n" +
                "<arc:http-header-user-agent>Mozilla/5.0 (compatible; heritrix/1.5.0-200506132127 +http://example.org/)</arc:http-header-user-agent>\n" +
                "<arc:http-header-from>example@example.org</arc:http-header-from>\n" +
                "<arc:robots>classic</arc:robots>\n" +
                "<dc:format>ARC file version 1.1</dc:format>\n" +
                "<dcterms:conformsTo xsi:type=\"dcterms:URI\">http://www.archive.org/web/researcher/ArcFileFormat.php</dcterms:conformsTo>\n" +
                "</arcmetadata>\n" +
                "\n" +
                "dns:www.law.gov.au 207.241.224.11 20050614070144 text/dns 55\n" +
                "20050614070144\n" +
                "www.law.gov.au.\t\t6858\tIN\tA\t152.91.15.12\n" +
                "\n" +
                "http://www.uq.edu.au/robots.txt 130.102.5.51 20050614070151 text/html 524\n" +
                "HTTP/1.1 302 Found\r\n" +
                "Date: Tue, 14 Jun 2005 07:01:49 GMT\r\n" +
                "Server: Apache/1.3.28 (Unix) DAV/1.0.3 PHP/4.2.2 mod_perl/1.24_01 mod_ssl/2.8.15 OpenSSL/0.9.7c\r\n" +
                "Location: http://www.uq.edu.au/\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/html; charset=iso-8859-1\r\n" +
                "\r\n" +
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" +
                "<HTML><HEAD>\n" +
                "<TITLE>302 Found</TITLE>\n" +
                "</HEAD><BODY>\n" +
                "<H1>Found</H1>\n" +
                "The document has moved <A HREF=\"http://www.uq.edu.au/\">here</A>.<P>\n" +
                "<HR>\n" +
                "<ADDRESS>Apache/1.3.28 Server at www.uq.edu.au Port 80</ADDRESS>\n" +
                "</BODY></HTML>\n" +
                "\n";

        WarcReader reader = new WarcReader(new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));

        Warcinfo filedesc = (Warcinfo) reader.next().get();
        assertEquals(Optional.of("example.arc"), filedesc.filename());
        assertEquals(Instant.parse("2005-06-14T07:01:44Z"), filedesc.date());
        assertEquals(1338, filedesc.body().size());
        assertEquals(MediaType.parse("text/plain"), filedesc.contentType());
        assertEquals(MessageVersion.ARC_1_1, filedesc.version());

        WarcResponse dns = (WarcResponse) reader.next().get();
        assertEquals("dns:www.law.gov.au", dns.target());
        assertEquals(Optional.of(InetAddresses.forString("207.241.224.11")), dns.ipAddress());
        assertEquals(MediaType.parse("text/dns"), dns.contentType());
        assertEquals(Instant.parse("2005-06-14T07:01:44Z"), dns.date());

        WarcResponse response = (WarcResponse) reader.next().get();
        assertEquals("http://www.uq.edu.au/robots.txt", response.target());
        assertEquals(Optional.of(InetAddresses.forString("130.102.5.51")), response.ipAddress());
        assertEquals(MediaType.HTTP_RESPONSE, response.contentType());
        assertEquals(Instant.parse("2005-06-14T07:01:51Z"), response.date());
        assertEquals(302, response.http().status());
        assertEquals("Found", response.http().reason());
        assertEquals(MediaType.parse("text/html;charset=iso-8859-1"), response.http().contentType());

        assertFalse(reader.next().isPresent());
    }

}