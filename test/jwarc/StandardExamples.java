package jwarc;

public class StandardExamples {

    /*
     * Examples from the WARC standard used to check our compliance.
     */

    final static String warcinfo = "WARC/1.0\r\n" +
            "WARC-Type: warcinfo\r\n" +
            "WARC-Date: 2006-09-19T17:20:14Z\r\n" +
            "WARC-Record-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "Content-Type: application/warc-fields\r\n" +
            "Content-Length: 381\r\n" +
            "\r\n" +
            "software: Heritrix 1.12.0 http://crawler.archive.org\r\n" +
            "hostname: crawling017.archive.org\r\n" +
            "ip: 207.241.227.234\r\n" +
            "isPartOf: testcrawl-20050708\r\n" +
            "description: testcrawl with WARC output\r\n" +
            "operator: IA\\_Admin\r\n" +
            "http-header-user-agent:\r\n" +
            " Mozilla/5.0 (compatible; heritrix/1.4.0 +http://crawler.archive.org)\r\n" +
            "format: WARC file version 1.0\r\n" +
            "conformsTo:\r\n" +
            " http://www.archive.org/documents/WarcFileFormat-1.0.html\r\n\r\n";

    final static String request = "WARC/1.0\r\n" +
            "WARC-Type: request\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Warcinfo-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "Content-Length: 236\r\n" +
            "WARC-Record-ID: <urn:uuid:4885803b-eebd-4b27-a090-144450c11594>\r\n" +
            "Content-Type: application/http;msgtype=request\r\n" +
            "WARC-Concurrent-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "\r\n" +
            "GET /images/logoc.jpg HTTP/1.0\r\n" +
            "User-Agent: Mozilla/5.0 (compatible; heritrix/1.10.0)\r\n" +
            "From: stack@example.org\r\n" +
            "Connection: close\r\n" +
            "Referer: http://www.archive.org/\r\n" +
            "Host: www.archive.org\r\n" +
            "Cookie: PHPSESSID=009d7bb11022f80605aa87e18224d824\r\n\r\n";

    final static String response = "WARC/1.0\r\n" +
            "WARC-Type: response\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Warcinfo-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2\r\n" +
            "WARC-Payload-Digest: sha1:CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2\r\n" +
            "WARC-IP-Address: 207.241.233.58\r\n" +
            "WARC-Record-ID: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "Content-Type: application/http;msgtype=response\r\n" +
            "WARC-Identified-Payload-Type: image/jpeg\r\n" +
            "Content-Length: 1902\r\n" +
            "\r\n" +
            "HTTP/1.1 200 OK\r\n" +
            "Date: Tue, 19 Sep 2006 17:18:40 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu)\r\n" +
            "Last-Modified: Mon, 16 Jun 2003 22:28:51 GMT\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Content-Length: 1662\r\n" +
            "Connection: close\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n" +
            "[image/jpeg binary data here]";

    final static String resource = "WARC/1.0\r\n"+
            "WARC-Type: resource\r\n"+
            "WARC-Target-URI: file://var/www/htdoc/images/logoc.jpg\r\n"+
            "WARC-Date: 2006-09-30T16:40:32Z\r\n"+
            "WARC-Record-ID: <urn:uuid:23200706-de3e-3c61-a131-g65d7fd80cc1>\r\n"+
            "Content-Type: image/jpeg\r\n"+
            "WARC-Payload-Digest: sha1:DBXHDRBXLF4OMUZ5DN4JJ2KFUAOB6VK8\r\n"+
            "WARC-Block-Digest: sha1:DBXHDRBXLF4OMUZ5DN4JJ2KFUAOB6VK8\r\n"+
            "Content-Length: 1662\r\n"+
            "\r\n"+
            "[image/jpeg binary data here]";

    final static String metadata = "WARC/1.0\r\n" +
            "WARC-Type: metadata\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Record-ID: <urn:uuid:16da6da0-bcdc-49c3-927e-57494593b943>\r\n" +
            "WARC-Concurrent-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "Content-Type: application/warc-fields\r\n" +
            "WARC-Block-Digest: sha1:VXT4AF5BBZVHDYKNC2CSM8TEAWDB6CH8\r\n" +
            "\r\n" +
            "Content-Length: 59\r\n" +
            "via: http://www.archive.org/\r\n" +
            "hopsFromSeed: E\r\n" +
            "fetchTimeMs: 565\r\n\r\n";

    final static String revisit = "WARC/1.0\r\n" +
            "WARC-Type: revisit\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2007-03-06T00:43:35Z\r\n" +
            "WARC-Profile: http://netpreserve.org/warc/1.0/server-not-modified\r\n" +
            "WARC-Record-ID: <urn:uuid:16da6da0-bcdc-49c3-927e-57494593bbbb>\r\n" +
            "WARC-Refers-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "Content-Type: message/http\r\n" +
            "Content-Length: 226\r\n" +
            "\r\n" +
            "HTTP/1.x 304 Not Modified\r\n" +
            "Date: Tue, 06 Mar 2007 00:43:35 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu) PHP/5.0.5-2ubuntu1.4\r\n" +
            "Connection: Keep-Alive\r\n" +
            "Keep-Alive: timeout=15, max=100\r\n" +
            "Etag: \"3e45-67e-2ed02ec0\"\r\n\r\n";

    final static String conversion = "WARC/1.0\r\n" +
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

    final static String continuation1 = "WARC/1.0\r\n" +
            "WARC-Type: response\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:2ASS7ZUZY6ND6CCHXETFVJDENAWF7KQ2\r\n" +
            "WARC-Payload-Digest: sha1:CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2\r\n" +
            "WARC-IP-Address: 207.241.233.58\r\n" +
            "WARC-Record-ID: <urn:uuid:39509228-ae2f-11b2-763a-aa4c6ec90bb0>\r\n" +
            "WARC-Segment-Number: 1\r\n" +
            "Content-Type: application/http;msgtype=response\r\n" +
            "Content-Length: 1600\r\n" +
            "\r\n" +
            "HTTP/1.1 200 OK\r\n" +
            "Date: Tue, 19 Sep 2006 17:18:40 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu)\r\n" +
            "Last-Modified: Mon, 16 Jun 2003 22:28:51 GMT\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Content-Length: 1662\r\n" +
            "Connection: close\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n" +
            "[first 1360 bytes of image/jpeg binary data here]";

    final static String continuation2 = "WARC/1.0\r\n" +
            "WARC-Type: continuation\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:T7HXETFVA92MSS7ZENMFZY6ND6WF7KB7\r\n" +
            "WARC-Record-ID: <urn:uuid:70653950-a77f-b212-e434-7a7c6ec909ef>\r\n" +
            "WARC-Segment-Origin-ID: <urn:uuid:39509228-ae2f-11b2-763a-aa4c6ec90bb0>\r\n" +
            "WARC-Segment-Number: 2\r\n" +
            "WARC-Segment-Total-Length: 1902\r\n" +
            "WARC-Identified-Payload-Type: image/jpeg\r\n" +
            "Content-Length: 302\r\n" +
            "\r\n" +
            "[last 302 bytes of image/jpeg binary data here]";


    /*
     * our own examples
     */

    final static String wacky = "WARC/3.141959\r\n\r\n";

    final static String[] all = new String[]{warcinfo, request, response, resource, metadata, revisit, conversion, continuation1, continuation2, wacky};

}
