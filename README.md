# jwarc [![Build Status](https://travis-ci.com/ato/jwarc.svg?branch=master)](https://travis-ci.com/ato/jwarc) [![Codecov](https://img.shields.io/codecov/c/github/ato/jwarc.svg)](https://codecov.io/gh/ato/jwarc)

(Work in progress) A Java library for reading and writing WARC files. This library includes a high level type-safe
API modeling the standard record types as individual classes with concise convenient accessors.

```java
try (WarcReader reader = new WarcReader(FileChannel.open(Paths.get("/tmp/her.warc")))) {
    for (WarcRecord record : reader) {
        
        if (record instanceof Warcinfo) {
            Warcinfo warcinfo = (Warcinfo) record;
            out.println("File format: " + warcinfo.version());
            out.println("Crawler: " + warcinfo.fields().first("software").orElse("unknown crawler"));
        }
        
        if (record instanceof WarcResponse && record.contentType().startsWith("application/http")) {
            WarcResponse response = (WarcResponse) record;
            out.println(response.http().status() + " " + response.target());
        }
        
    }
}
```

It uses a finite state machine parser generated from a strict [grammar](https://github.com/ato/jwarc/blob/master/src/org/netpreserve/jwarc/WarcParser.rl)
using [Ragel](http://www.colm.net/open-source/ragel/). You can use the parser directly in a push fashion for advanced use
cases like non-blocking I/O.

Gzipped records are automatically decompressed. The parser interprets ARC/1.1 record as if they are a WARC dialect and
populates the appropriate WARC headers.

All I/O is performed using NIO and an an effort is made to minimize data copies and share buffers whenever feasible.
Direct buffers and even memory-mapped files can be used, but only with uncompressed WARCS until they're supported by
Inflater (coming in JDK 11).

**Limitations:** This library has not been battle tested yet. The HTTP parser in particular is lacking a robust
parsing mode and is probably too strict for real world data. The API for writing of records is incomplete. The
documentation is still being written.

## Usage

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

## Comparison

| Criteria            | jwarc       | [JWAT]          | [webarchive-commons]  |
|---------------------|-------------|-----------------|---------------|
| Battle tested       | ✘           | ✔               | ✔             |
| License             | Apache 2    | Apache 2        | Apache 2      |
| Size                | 71 KB       | 143 KB          | 681 KB + deps |
| Parser based on     | Ragel FSM   | Hand-rolled FSM | Apache HTTP   |
| Push parsing        | Low level   | ✘               | ✘             |
| Folded headers †    | ✔           | ✔               | ✔             | 
| [Encoded words] †   | ✘           | ✘ (disabled)    | ✘             |
| Validation          | ✘           | ✔               | ✘             |
| Strict parsing  ‡   | ✔           | ✘               | ✘             |
| Lenient parsing     | ✘           | ✔               | ✔             |
| Multi-value headers | ✔           | ✔               | ✘             |
| I/O Framework       | NIO         | IO              | IO            |
| Record type classes | ✔           | ✘               | ✘             |
| Typed accessors     | ✔           | ✔               | Some          |
| GZIP detection      | ✔           | ✔               | Filename only |
| ARC support         | Auto        | Separate API    | Factory       |
| Speed * (.warc)     | 1x          | ~5x slower      | ~13x slower   |
| Speed * (.warc.gz)  | 1x          | ~1.4x slower    | ~2.8x slower  |

(†) WARC features copied from HTTP that have since been deprecated in HTTP. I'm not aware of any software that writes
WARCs using these features and usage of them should probably be avoided. JWAT behaves differently from jwarc and
webarchive-commons as it does not trim whitespace on folded lines.

(‡) JWAT and webarchive-commons both accept arbitrary UTF-8 characters in field names. jwarc strictly enforces the
grammar rules from the WARC specification, although it does not currently enforce the rules for the values of specific
individual fields.

(*) Relative time to scan records after JIT steady state. Only indicative. Need to redo this with a better
benchmark. JWAT was configured with a 8192 byte buffer as with default options it is 27x slower. For comparison
merely decompressing the .warc.gz file with GZIPInputStream is about 0.95x.

[JWAT]: https://sbforge.org/display/JWAT/JWAT
[webarchive-commons]: https://github.com/iipc/webarchive-commons
[Encoded words]: https://www.ietf.org/rfc/rfc2047.txt

### Other WARC libraries

* [go-warc](https://github.com/wolfgangmeyers/go-warc) (Go)
* [warc](https://github.com/datatogether/warc) (Go)
* [warc](https://github.com/internetarchive/warc) (Python)
* [warc-clojure](https://github.com/shriphani/warc-clojure) (Clojure) - JWAT wrapper
* [warc-hadoop](https://github.com/ept/warc-hadoop) (Java)
* [warcat](https://github.com/chfoo/warcat) (Python)
* [warcio](https://github.com/webrecorder/warcio) (Python)
* [warctools](https://github.com/internetarchive/warctools) (Python)
* [webarchive](https://github.com/richardlehane/webarchive) (Go)