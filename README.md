# jwarc [![](https://maven-badges.herokuapp.com/maven-central/org.netpreserve/jwarc/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.netpreserve/jwarc) [![](https://www.javadoc.io/badge/org.netpreserve/jwarc.svg)](https://www.javadoc.io/doc/org.netpreserve/jwarc)
A Java library for reading and writing WARC files. This library includes a high level API modeling
the standard record types as individual classes with typed accessors. The API is exensible and you can register
extension record types and accessors for extension header fields.

```java
try (WarcReader reader = new WarcReader(FileChannel.open(Paths.get("example.warc")))) {
    for (WarcRecord record : reader) {
        if (record instanceof WarcResponse && record.contentType().base().equals(MediaType.HTTP)) {
            WarcResponse response = (WarcResponse) record;
            System.out.println(response.http().status() + " " + response.target());
        }
    }
}
```

It uses a finite state machine parser generated from a strict [grammar](https://github.com/ato/jwarc/blob/master/src/org/netpreserve/jwarc/WarcParser.rl)
using [Ragel](http://www.colm.net/open-source/ragel/).

Gzipped records are automatically decompressed. The parser interprets ARC/1.1 record as if they are a WARC dialect and
populates the appropriate WARC headers.

All I/O is performed using NIO and an an effort is made to minimize data copies and share buffers whenever feasible.
Direct buffers and even memory-mapped files can be used, but only with uncompressed WARCS until they're supported by
Inflater (coming in JDK 11).

## Getting it

To use as a library add jwarc as a dependency from [Maven Central](https://maven-badges.herokuapp.com/maven-central/org.netpreserve/jwarc).

To use as a command-line tool install [Java 8 or later](https://adoptopenjdk.net/), download
the latest [release jar](https://github.com/iipc/jwarc/releases) and run it using:
 
    java -jar jwarc-{version}.jar

If you would prefer to build it from source install [JDK 8+](https://adoptopenjdk.net/) and 
[Maven](https://maven.apache.org/) and then run:

    mvn package

## Examples

### Saving a remote resource

```java
try (WarcWriter writer = new WarcWriter(System.out)) {
    writer.fetch(URI.create("http://example.org/"));
}
```

### Writing records

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

### Filter expressions

The [WarcFilter](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcFilter.html) class
provides a simple filter expression language for matching WARC records. For example here's a moderately complex filter
which matches all records that are not image resources or image responses:

     !((warc-type == "resource" && content-type =~ "image/.*") || 
       (warc-type == "response" && http:content-type =~ "image/.*")) 

WarcFilter implements `Predicate<WarcRecord>` and be used to conveniently with streams of records:

```java
long errorCount = warcReader.records().filter(WarcFilter.compile(":status >= 400")).count();
``` 

Their real power though is as a building block for user-supplied options. 

### Command-line tools

jwarc also includes a set of command-lines tools which serve as [examples](src/org/netpreserve/jwarc/tools/). Note that
many of the tools are lightweight demonstrations and may lack important options and features.

Capture a URL (without subresources):

    java -jar jwarc.jar fetch http://example.org/ > example.warc

Create a CDX file:

    java -jar jwarc.jar cdx example.warc > records.cdx

Run a replay proxy and web server:

    export PORT=8080
    java -jar jwarc.jar serve example.warc

Replay each page within in a WARC and use [headless Chrome](https://developers.google.com/web/updates/2017/04/headless-chrome)
to render a screenshot and save it as a resource record:

    export BROWSER=/opt/google/chrome/chrome
    java -jar jwarc.jar screenshot example.warc > screenshots.warc

Running a proxy server which records requests and responses. This will generate self-signed SSL certificates so you will
will need turn off TLS verification in the client. For Chrome/Chromium use the `--ignore-certificate-errors`
command-line option.

    export PORT=8080
    java -jar jwarc.jar recorder > example.warc

    chromium --proxy-server=http://localhost:8080 --ignore-certificate-errors

Record a command that obeys the http(s)_proxy and CURL_CA_BUNDLE environment variables:

    java -jar jwarc.jar recorder -o example.warc curl http://example.org/

Capture a page by recording headless Chrome:

    export BROWSER=/opt/google/chrome/chrome
    java -jar jwarc.jar record > example.warc

Create a new file containing only html responses with status 200:

    java -jar jwarc.jar filter ':status == 200 && http:content-type =~ "text/html(;.*)?"' example.warc > pages.warc 

## API Quick Reference

See the [javadoc](https://www.javadoc.io/doc/org.netpreserve/jwarc) for more details.

### [WarcReader](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcReader.html)

```java
              new WarcReader(stream|path|channel);                // opens a WARC file for reading
                  reader.close();                                 // closes the underlying channel
(WarcCompression) reader.compression();                           // type of compression: NONE or GZIP
       (Iterator) reader.iterator();                              // an iterator over the records
     (WarcRecord) reader.next();                                  // reads the next record
                  reader.registerType("myrecord", MyRecord::new); // registers a new record type
```

### [WarcWriter](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcWriter.html)

```java
                new WarcWriter(channel, NONE|GZIP);    // opens a WARC file for writing
                    writer.fetch(uri);                 // downloads a resource recording the request and response
             (long) writer.position();                 // byte position the next record will be written to
                    writer.write(record);              // adds a record to the WARC file
```
        
### Record types

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
            WarcRevisit     (revisit)

#### [Message](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/Message.html)

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
                 (boolean) message.headers().contains("TE", "deflate"); // tests if a value is present
        (Optional<String>) message.headers().first("Cookie");  // the first value of a header
(Map<String,List<String>>) message.headers().map();            // views the header fields as a map
        (Optional<String>) message.headers().sole("Location"); // throws if header has multiple values
         (ProtocolVersion) message.version();                  // the protocol version (e.g. HTTP/1.0 or WARC/1.1)
```

#### [WarcRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcRecord.html)

Methods available on all WARC records:

```java
  (Optional<Digest>) record.blockDigest();   // value of hash function applied to bytes of body
           (Instant) record.date();          // instant that data capture began
               (URI) record.id();            // globally unique record identifier
    (Optional<Long>) record.segmentNumber(); // position of this record in segmentated series
   (TuncationReason) record.truncated();     // reason record was truncated; or else NOT_TRUNCATED
            (String) record.type();          // "warcinfo", "request", "response" etc
```

#### [Warcinfo](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/Warcinfo.html)

```java
            (Headers) warcinfo.fields();   // parses the body as application/warc-fields
   (Optional<String>) warcinfo.filename(); // filename of the containing WARC
```

#### [WarcTargetRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcTargetRecord.html) (abstract)

Methods available on all WARC records except Warcinfo:

```java
     (Optional<String>) record.identifiedPayloadType(); // media type of payload identified by an independent check
               (String) record.target();                // captured URI as an unparsed string
                  (URI) record.targetURI();             // captured URI
(Optional<WarcPayload>) record.payload();               // payload
     (Optional<Digest>) record.payloadDigest();         // value of hash function applied to bytes of the payload
        (Optional<URI>) record.warcinfoID();            // ID of warcinfo record when stored separately
```

#### [WarcContinuation](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcContinuation.html)

```java
             (String) continuation.segmentOriginId();    // record ID of first segment
   (Optional<String>) continuation.segmentTotalLength(); // (last only) total length of all segments
```

#### [WarcConversion](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcConversion.html)

```java
      (Optional<URI>) conversion.refersTo();    // ID of record this one was converted from
```

#### [WarcCaptureRecord](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcCaptureRecord.html) (abstract)

Methods available on metadata, request, resource and response records:

```java
          (List<URI>) capture.concurrentTo();   // other record IDs from the same capture event
 (Optional<InetAddr>) capture.ipAddress();      // IP address of the server
```

#### [WarcMetadata](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcMetadata.html)

```java
            (Headers) metadata.fields();        // parses the body as application/warc-fields
```

#### [WarcRequest](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcRequest.html)

```java
        (HttpRequest) request.http();           // parses the body as a HTTP request
        (BodyChannel) request.http().body();    // HTTP request body
            (Headers) request.http().headers(); // HTTP request headers
```

#### [WarcResource](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcResource.html)

No methods are specific to resource records. See WarcRecord, WarcTargetRecord, WarcCaptureRecord above.

#### [WarcResponse](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcResponse.html)

```java
       (HttpResponse) response.http();           // parses the body as a HTTP response
        (BodyChannel) response.http().body();    // HTTP response body
            (Headers) response.http().headers(); // HTTP response headers
```

#### [WarcRevisit](https://www.javadoc.io/page/org.netpreserve/jwarc/latest/org/netpreserve/jwarc/WarcResponse.html)

```java
       (HttpResponse) revisit.http();              // parses the body as a HTTP response
            (Headers) revisit.http().headers();    // HTTP response headers (note: revisits never have a payload!)
                (URI) revisit.profile()            // revisit profile (not modified or identical payload)
                (URI) revisit.refersTo();          // id of record this is a duplicate of
                (URI) revisit.refersToTargetURI(); // targetURI of the referred to record 
            (Instant) revisit.refersToDate();      // date of the referred to record  
```

Note: revisit records never have a payload so 

## Comparison

| Criteria            | jwarc       | [JWAT]          | [webarchive-commons]  |
|---------------------|-------------|-----------------|---------------|
| License             | Apache 2    | Apache 2        | Apache 2      |
| Parser based on     | Ragel FSM   | Hand-rolled FSM | Apache HTTP   |
| Push parsing        | Low level   | ✘               | ✘             |
| Folded headers †    | ✔           | ✔               | ✔             | 
| [Encoded words] †   | ✘           | ✘ (disabled)    | ✘             |
| Validation          | The basics  | ✔               | ✘             |
| Strict parsing  ‡   | ✔           | ✘               | ✘             |
| Lenient parsing     | HTTP only   | ✔               | ✔             |
| Multi-value headers | ✔           | ✔               | ✘             |
| I/O Framework       | NIO         | IO              | IO            |
| Record type classes | ✔           | ✘               | ✘             |
| Typed accessors     | ✔           | ✔               | Some          |
| GZIP detection      | ✔           | ✔               | Filename only |
| WARC writer         | Barebones   | ✔               | ✔             |
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

See also: [Unaffiliated benchmark against other languages](https://code402.com/hello-warc-common-crawl-code-samples)

[More recent benchmarks against Java libraries](https://github.com/iipc/jwarc/pull/19)

[JWAT]: https://sbforge.org/display/JWAT/JWAT
[webarchive-commons]: https://github.com/iipc/webarchive-commons
[Encoded words]: https://www.ietf.org/rfc/rfc2047.txt

### Other WARC libraries

* [go-warc](https://github.com/wolfgangmeyers/go-warc) (Go)
* [node-warc](https://www.npmjs.com/package/node-warc) (Node.js)
* [warc](https://github.com/datatogether/warc) (Go)
* [warc](https://github.com/internetarchive/warc) (Python)
* [warc-clojure](https://github.com/shriphani/warc-clojure) (Clojure) - JWAT wrapper
* [warc-hadoop](https://github.com/ept/warc-hadoop) (Java)
* [warcat](https://github.com/chfoo/warcat) (Python)
* [warcio](https://github.com/webrecorder/warcio) (Python)
* [warctools](https://github.com/internetarchive/warctools) (Python)
* [webarchive](https://github.com/richardlehane/webarchive) (Go)
