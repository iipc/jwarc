# jwarc [![](https://travis-ci.com/iipc/jwarc.svg?branch=master)](https://travis-ci.com/iipc/jwarc) [![](https://maven-badges.herokuapp.com/maven-central/org.netpreserve/jwarc/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.netpreserve/jwarc) [![](https://www.javadoc.io/badge/org.netpreserve/jwarc.svg)](https://www.javadoc.io/doc/org.netpreserve/jwarc)
(Work in progress) A Java library for reading and writing WARC files. This library includes a high level API modeling
the standard record types as individual classes with typed accessors. The API is exensible and you can register
extension record types and accessors for extension header fields.

```java
try (WarcReader reader = new WarcReader(FileChannel.open(Paths.get("/tmp/her.warc")))) {
    for (WarcRecord record : reader) {
        
        if (record instanceof Warcinfo) {
            Warcinfo warcinfo = (Warcinfo) record;
            out.println("File format: " + warcinfo.version());
            out.println("Crawler: " + warcinfo.fields().first("software").orElse("unknown crawler"));
        }
        
        if (record instanceof WarcResponse && record.contentType().base().equals(MediaType.HTTP)) {
            WarcResponse response = (WarcResponse) record;
            out.println(response.http().status() + " " + response.target());
        }
        
    }
}
```

It uses a finite state machine parser generated from a strict [grammar](https://github.com/ato/jwarc/blob/master/src/org/netpreserve/jwarc/WarcParser.rl)
using [Ragel](http://www.colm.net/open-source/ragel/). You can use the parser directly in a push fashion for advanced
use cases like non-blocking I/O.

Gzipped records are automatically decompressed. The parser interprets ARC/1.1 record as if they are a WARC dialect and
populates the appropriate WARC headers.

All I/O is performed using NIO and an an effort is made to minimize data copies and share buffers whenever feasible.
Direct buffers and even memory-mapped files can be used, but only with uncompressed WARCS until they're supported by
Inflater (coming in JDK 11).

**Limitations:** This library has not been battle tested yet. The HTTP parser in lacking a robust parsing mode and is 
probably too strict for real world data. The API for writing of records is incomplete.

Once implemented the API for writing records will probably look something like this:

```java
// write a warcinfo record
// date and record id will be populated automatically if unset
writer.write(new Warcinfo.Builder()
    .fields("software", "my-cool-crawler/1.0",
            "robots", "obey")
    .build());

// we can also supply a specific date
Instant captureDate = Instant.now();

// write a request but keep a copy of it to reference later
WarcRequest request = new WarcRequest.Builder()
    .date(captureDate)
    .target(uri)
    .contentType("application/http")
    .body(bodyStream, bodyLength)
    .build();
writer.write(request);

// write a response referencing the request
WarcResponse response = new WarcResponse.Builder()
    .date(captureDate)
    .target(uri)
    .contentType("application/http")
    .body("HTTP/1.0 200 OK\r\n...".getBytes())
    .concurrentTo(request.id())
    .build();
writer.write(response);
```

## API Overview

See the [javadoc](https://www.javadoc.io/doc/org.netpreserve/jwarc) for more details.

Record type hierarchy:

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

### [Message](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/Message.html)

The basic building block of both HTTP protocol and WARC file format is a message consisting of set of named header
fields and a body. Header field names are case-insensitvie and may have multiple values.

```java
             (BodyChannel) message.body();                     // the message body as a ReadableByteChannel
                    (long) message.body().position();          // the next byte position to read from
                     (int) message.body().read(byteBuffer);    // reads a sequence of bytes from the body
                    (long) message.body().size();              // the length in bytes of the body
             (InputStream) message.body().stream();            // views the body as an InputStream
                  (String) message.contentType();              // the media type of the body
                 (Headers) message.headers();                  // the header fields
            (List<String>) message.headers().all("Cookie");    // all values of a header
        (Optional<String>) message.headers().first("Cookie");  // the first value of a header
(Map<String,List<String>>) message.headers().map();            // views the header fields as a map
        (Optional<String>) message.headers().sole("Location"); // throws if header has multiple values
         (ProtocolVersion) message.version();                  // the protocol version (e.g. HTTP/1.0 or WARC/1.1)
```

### [WarcRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcRecord.html)

Methods available on all WARC records:

```java
  (Optional<Digest>) record.blockDigest();   // value of hash function applied to bytes of body
           (Instant) record.date();          // instant that data capture began
               (URI) record.id();            // globally unique record identifier
    (Optional<Long>) record.segmentNumber(); // position of this record in segmentated series
   (TuncationReason) record.truncated();     // reason record was truncated; or else NOT_TRUNCATED
            (String) record.type();          // "warcinfo", "request", "response" etc
```

### [Warcinfo](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/Warcinfo.html)

```java
            (Headers) warcinfo.fields();   // parses the body as application/warc-fields
   (Optional<String>) warcinfo.filename(); // filename of the containing WARC
```

### [WarcTargetRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcTargetRecord.html)

Methods available on all WARC records except Warcinfo:

```java
   (Optional<String>) record.identifiedPayloadType(); // media type of payload identified by an independent check
             (String) record.target();                // captured URI as an unparsed string
                (URI) record.targetURI();             // captured URI
   (Optional<Digest>) record.payloadDigest();         // value of hash function applied to bytes of the payload
      (Optional<URI>) record.warcinfoID();            // ID of warcinfo record when stored separately
```

### [WarcContinuation](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcContinuation.html)

```java
             (String) continuation.segmentOriginId();    // record ID of first segment
   (Optional<String>) continuation.segmentTotalLength(); // (last only) total length of all segments
```

### [WarcConversion](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcConversion.html)

```java
      (Optional<URI>) conversion.refersTo();    // ID of record this one was converted from
```

### [WarcCaptureRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcCaptureRecord.html)

Methods available on metadata, request, resource and response records:

```java
          (List<URI>) capture.concurrentTo();   // other record IDs from the same capture event
 (Optional<InetAddr>) capture.ipAddress();      // IP address of the server
```

### [WarcMetadata](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcMetadata.html)

```java
            (Headers) metadata.fields();        // parses the body as application/warc-fields
```

### [WarcRequest](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcRequest.html)

```java
        (HttpRequest) request.http();           // parses the body as a HTTP request
        (BodyChannel) request.http().body();    // HTTP request body
            (Headers) request.http().headers(); // HTTP request headers
```

### [WarcResource](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcResource.html)

No methods are specific to resource records. See WarcRecord, WarcTargetRecord, WarcCaptureRecord above.

### [WarcResponse](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcResponse.html)

```java
       (HttpResponse) response.http();           // parses the body as a HTTP response
        (BodyChannel) response.http().body();    // HTTP response body
            (Headers) response.http().headers(); // HTTP response headers
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
| WARC writer         | TODO        | ✔               | ✔             |
| ARC reader          | Auto        | Separate API    | Factory       |
| ARC writer          | ✘           | ✔               | ✔             |
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
