# jwarc [![Build Status](https://travis-ci.com/ato/jwarc.svg?branch=master)](https://travis-ci.com/ato/jwarc) [![Codecov](https://img.shields.io/codecov/c/github/ato/jwarc.svg)](https://codecov.io/gh/ato/jwarc)

(Work in progress) A Java library for reading and writing WARC files.

## Usage

Not functional yet. This is just the plan.

### Parsing a record
```java
WarcRecord record = WarcRecord.parse(channel);
```

TODO: WarcReader.

## Quick Reference

    Message
      HttpMessage
        HttpRequest
        HttpResponse
      WarcRecord
        Warcinfo            (warcinfo)
        WarcTargetRecord
          WarcContinuation  (continuation)
          WarcConversion    (conversion)
          WarcCaptureRecord
            WarcMetadata    (metadata)
            WarcRequest     (request)
            WarcResource    (resource)
            WarcResponse    (response)

### Message

The basic building block of both HTTP protocol and WARC file format is a message consisting of set of named header
fields and a body. Header field names are case-insensitvie and may have multiple values.

```java
             (BodyChannel) message.body();                     // the message body as a ReadableByteChannel
                    (long) message.body().position();          // the next byte position to read from
                     (int) message.body().read(byteBuffer);    // reads a sequence of bytes from the body
                    (long) message.body().size();              // the length in bytes of the body
             (InputStream) message.body().stream();            // views the body as an InputStream
                  (String) message.body().type();              // the MIME type of the body
                 (Headers) message.headers();                  // the header fields
            (List<String>) message.headers().all("Cookie");    // all values of a header
        (Optional<String>) message.headers().first("Cookie");  // the first value of a header
(Map<String,List<String>>) message.headers().map();            // views the header fields as a map
        (Optional<String>) message.headers().sole("Location"); // throws if header has multiple values
         (ProtocolVersion) message.version();                  // the protocol version (e.g. HTTP/1.0 or WARC/1.1)
```

### WarcRecord

Available on all WARC records.

### Warcinfo

```java
            (Headers) warcinfo.fields();   // parse the body as application/warc-fields
   (Optional<String>) warcinfo.filename(); // filename of the containing WARC
```

### WarcTargetRecord

Available on all WARC records except Warcinfo:

```java
                (URI) record.targetURI();         // captured URI
               (long) record.payload().length();  // length of payload in bytes
             (String) record.payload().type();    // content-type of payload
        (ByteChannel) record.payload().channel(); // channel for reading the payload
      (Optional<URI>) record.warcinfoID();        // ID of warcinfo record when stored separately
```

### WarcContinuation

```java
             (String) continuation.segmentOriginId();    // record ID of first segment
   (Optional<String>) continuation.segmentTotalLength(); // (last only) total length of all segments
```

### WarcConversion

```java
      (Optional<URI>) conversion.refersTo(); // ID of record this one was converted from
```

### WarcCaptureRecord

```java
          (List<URI>) capture.concurrentTo(); // other record IDs from the same capture event
 (Optional<InetAddr>) capture.ipAddress();    // IP address of the server
```

### WarcMetadata

```java
// nothing specific yet. maybe .fields() for WARC fields?
```

### WarcRequest

```java
(HttpRequest) request.http(); //
```

### HTTP headers
 
Use `http()` on a `WarcRequest` or `WarcResponse` to parse the HTTP headers.

```java
        (Optional<String>) response.http().headers().sole("Location");    // throws if multi valued
        (Optional<String>) response.http().headers().first("Set-Cookie"); // first matching header
            (List<String>) response.http().headers().all("Set-Cookie");   // all matching headers
(Map<String,List<String>>) response.http().headers().map();               // multimap of every headers

```

### WARC headers

```java
        (Optional<String>) record.headers().sole("WARC-Target-URI");
        (Optional<String>) record.headers().first("WARC-Concurrent-To");
            (List<String>) record.headers().all("WARC-Concurrent-To");
(Map<String,List<String>>) record.headers().map();
```



## Other WARC libraries

* [JWAT](https://sbforge.org/display/JWAT/JWAT)
* [webarchive-commons](https://github.com/iipc/webarchive-commons)