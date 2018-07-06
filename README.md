# jwarc [![Build Status](https://travis-ci.com/ato/jwarc.svg?branch=master)](https://travis-ci.com/ato/jwarc) [![Codecov](https://img.shields.io/codecov/c/github/ato/jwarc.svg)](https://codecov.io/gh/ato/jwarc)

(Work in progress) A Java library for reading and writing WARC files.

## Usage

Not functional yet. This is just the plan.

### Parsing a record
```java
WarcRecord record = WarcRecord.parse(channel);
```

TODO: WarcReader.

## Record types

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