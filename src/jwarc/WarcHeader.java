package jwarc;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class WarcHeader {

    public static final String WARC_TYPE = "WARC-Type";
    public static final String WARC_RECORD_ID = "WARC-Record-ID";
    public static final String WARC_DATE = "WARC-Date";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String WARC_CONCURRENT_TO = "WARC-Concurrent-To";
    public static final String WARC_BLOCK_DIGEST = "WARC-Block-Digest";
    public static final String WARC_PAYLOAD_DIGEST = "WARC-Payload-Digest";
    public static final String WARC_IP_ADDRESS = "WARC-IP-Address";
    public static final String WARC_REFERS_TO = "WARC-Refers-To";
    public static final String WARC_TARGET_URI = "WARC-Target-URI";
    public static final String WARC_TRUNCATED = "WARC-Truncated";
    public static final String WARC_WARCINFO_ID = "WARC-Warcinfo-ID";
    public static final String WARC_FILENAME = "WARC-Filename";
    public static final String WARC_PROFILE = "WARC-Profile";
    public static final String WARC_IDENTIFIED_PAYLOAD_TYPE = "WARC-Identified-Payload-Type";
    public static final String WARC_SEGMENT_ORIGIN_ID = "WARC-Segment-Origin-ID";
    public static final String WARC_SEGMENT_NUMBER = "WARC-Segment-Number";
    public static final String WARC_SEGMENT_TOTAL_LENGTH = "WARC-Segment-Total-Length";

    private int majorVersion;
    private int minorVersion;
    private Map<String,String> fields;

    public WarcHeader() {
        majorVersion = 1;
        minorVersion = 0;
        fields = new LinkedTreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    WarcHeader(int majorVersion, int minorVersion, Map<String, String> fields) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.fields = fields;
    }

    public Map<String,String> fields() {
        return fields;
    }

    URI parseUri(String uri) {
        /*
         * The BNF grammar for WARC-Target-URI and WARC-Profile in the WARC 1.0 specification is inconsistent with its
         * own examples.  The grammar specifies that URIs must be wrapped in "<" and ">", which examples follow except
         * for those two fields.  Most implementations follow the examples.  Let's accept either form when parsing.
         */
        if (uri == null) {
            return null;
        } else if (uri.startsWith("<") && uri.endsWith(">")) {
            return URI.create(uri.substring(1, uri.length() - 1));
        } else {
            return URI.create(uri);
        }
    }


    public static WarcHeader parse(ByteBuffer buffer) {
        WarcParser parser = new WarcParser();
        parser.feed(buffer);
        if (parser.isError()) {
            throw new IllegalArgumentException("parse error");
        } else if (!parser.isFinished()) {
            throw new IllegalArgumentException("premature end of input");
        } else {
            return new WarcHeader(parser.versionMajor, parser.versionMinor, parser.fields);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("WARC/" + majorVersion + "." + minorVersion + "\r\n");
        for (Map.Entry<String, String> field : fields().entrySet()) {
            sb.append(field.getKey()).append(": ").append(field.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public WarcHeader set(String field, String value) {
        if (value != null) {
            fields.put(field, value);
        } else {
            fields.remove(field);
        }
        return this;
    }

    /*
     * Getters for standard fields
     */

    public int majorVersion() {
        return majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public String type() {
        return fields.get(WARC_TYPE);
    }

    public WarcRecordId recordId() {
        return WarcRecordId.parse(fields.get(WARC_RECORD_ID));
    }

    public WarcRecordId refersTo() {
        return WarcRecordId.parse(fields.get(WARC_REFERS_TO));
    }

    public Long contentLength() {
        String value = fields.get(CONTENT_LENGTH);
        return value == null ? null : Long.valueOf(value);
    }

    public Date date() {
        String value = fields.get(WARC_DATE);
        return value == null ? null : Date.from(Instant.parse(value));
    }

    public String contentType() {
        return fields.get(CONTENT_TYPE);
    }

    public WarcRecordId concurrentTo() {
        return WarcRecordId.parse(fields.get(WARC_CONCURRENT_TO));
    }

    public WarcDigest blockDigest() {
        String value = fields.get(WARC_BLOCK_DIGEST);
        return value == null ? null : WarcDigest.parse(value);
    }

    public WarcDigest payloadDigest() {
        String value = fields.get(WARC_PAYLOAD_DIGEST);
        return value == null ? null : WarcDigest.parse(value);
    }

    public String ipAddress() {
        return fields.get(WARC_IP_ADDRESS);
    }

    public URI targetUri() {
        return parseUri(fields.get(WARC_TARGET_URI));
    }

    public String truncated() {
        return fields.get(WARC_TRUNCATED);
    }

    public WarcRecordId warcinfoId() {
        return WarcRecordId.parse(fields.get(WARC_WARCINFO_ID));
    }

    public String filename() {
        // TODO: support quoted-string
        return fields.get(WARC_FILENAME);
    }

    public URI profile() {
        return parseUri(fields.get(WARC_PROFILE));
    }

    public String identifiedPayloadType() {
        return fields.get(WARC_IDENTIFIED_PAYLOAD_TYPE);
    }

    public Long segmentNumber() {
        String value = fields.get(WARC_SEGMENT_NUMBER);
        return value == null ? null : Long.valueOf(value);
    }

    public WarcRecordId segmentOriginId() {
        return WarcRecordId.parse(fields.get(WARC_SEGMENT_ORIGIN_ID));
    }

    public Long segmentTotalLength() {
        String value = fields.get(WARC_SEGMENT_TOTAL_LENGTH);
        return value == null ? null : Long.valueOf(value);
    }

    /*
     * Setters for standard fields
     */

    public WarcHeader majorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
        return this;
    }

    public WarcHeader minorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
        return this;
    }

    public WarcHeader blockDigest(WarcDigest blockDigest) {
        return set(WARC_BLOCK_DIGEST, blockDigest == null ? null : blockDigest.toString());
    }

    public WarcHeader concurrentTo(WarcRecordId concurrentTo) {
        return set(WARC_CONCURRENT_TO, concurrentTo == null ? null : concurrentTo.toString());
    }

    public WarcHeader contentLength(Long contentLength) {
        return set(CONTENT_LENGTH, contentLength == null ? null : contentLength.toString());
    }

    public WarcHeader contentType(String contentType) {
        return set(CONTENT_TYPE, contentType);
    }

    public WarcHeader date(Date date) {
        return set(WARC_DATE, date == null ? null :date.toInstant().toString());
    }

    public WarcHeader filename(String filename) {
        return set(WARC_FILENAME, filename);
    }

    public WarcHeader identifiedPayloadType(String identifiedPayloadType) {
        return set(WARC_IDENTIFIED_PAYLOAD_TYPE, identifiedPayloadType);
    }

    public WarcHeader ipAddress(String ipAddress) {
        return set(WARC_IP_ADDRESS, ipAddress);
    }

    public WarcHeader payloadDigest(WarcDigest payloadDigest) {
        return set(WARC_PAYLOAD_DIGEST, payloadDigest == null ? null : payloadDigest.toString());
    }

    public WarcHeader profile(URI profile) {
        return set(WARC_PROFILE, profile == null ? null : profile.toString());
    }

    public WarcHeader recordId(WarcRecordId recordId) {
        return set(WARC_RECORD_ID, recordId == null ? null : recordId.toString());
    }

    public WarcHeader refersTo(WarcRecordId refersTo) {
        return set(WARC_REFERS_TO, refersTo == null ? null : refersTo.toString());
    }

    public WarcHeader segmentNumber(Long segmentNumber) {
        return set(WARC_SEGMENT_NUMBER, segmentNumber == null ? null : segmentNumber.toString());
    }

    public WarcHeader segmentOriginId(WarcRecordId segmentOriginId) {
        return set(WARC_SEGMENT_ORIGIN_ID, segmentOriginId == null ? null : segmentOriginId.toString());
    }

    public WarcHeader segmentTotalLength(Long segmentTotalLength) {
        return set(WARC_SEGMENT_TOTAL_LENGTH, segmentTotalLength == null ? null : segmentTotalLength.toString());
    }

    public WarcHeader targetUri(URI targetUri) {
        return set(WARC_TARGET_URI, targetUri == null ? null : targetUri.toString());
    }

    public WarcHeader truncated(String truncated) {
        return set(WARC_TRUNCATED, truncated);
    }

    public WarcHeader type(String type) {
        return set(WARC_TYPE, type);
    }

    public WarcHeader warcinfoId(WarcRecordId warcinfoId) {
        return set(WARC_WARCINFO_ID, warcinfoId == null ? null : warcinfoId.toString());
    }

}
