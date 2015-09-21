package jwarc;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WarcHeaderTest {

    @Test
    public void emptyHeadersShouldReturnNullForAllFields() {
        WarcHeader header = new WarcHeader();

        assertEquals(1, header.majorVersion());
        assertEquals(0, header.minorVersion());
        assertTrue(header.fields().isEmpty());

        assertNull(header.contentLength());
        assertNull(header.contentType());
        assertNull(header.blockDigest());
        assertNull(header.concurrentTo());
        assertNull(header.date());
        assertNull(header.filename());
        assertNull(header.identifiedPayloadType());
        assertNull(header.ipAddress());
        assertNull(header.payloadDigest());
        assertNull(header.profile());
        assertNull(header.recordId());
        assertNull(header.refersTo());
        assertNull(header.segmentNumber());
        assertNull(header.segmentOriginId());
        assertNull(header.segmentTotalLength());
        assertNull(header.targetUri());
        assertNull(header.truncated());
        assertNull(header.type());
        assertNull(header.warcinfoId());
    }

    @Test
    public void roundtripAllExamples() {
        for (String example : StandardExamples.all) {
            example = example.split("\r\n\r\n")[0] + "\r\n\r\n";
            ByteBuffer buffer = ByteBuffer.wrap(example.getBytes(StandardCharsets.UTF_8));
            WarcHeader header = WarcHeader.parse(buffer);
            assertEquals(example, header.toString());
        }
    }

    @Test
    public void roundtripAllExamplesViaSetters() {
        /*
         * reuse out between examples to ensure nulling fields removes them
         */
        WarcHeader out = new WarcHeader();

        for (String example : StandardExamples.all) {
            example = example.split("\r\n\r\n")[0] + "\r\n\r\n";
            ByteBuffer buffer = ByteBuffer.wrap(example.getBytes(StandardCharsets.UTF_8));
            WarcHeader in = WarcHeader.parse(buffer);

            out.majorVersion(in.majorVersion());
            out.minorVersion(in.minorVersion());
            out.contentLength(in.contentLength());
            out.contentType(in.contentType());
            out.blockDigest(in.blockDigest());
            out.concurrentTo(in.concurrentTo());
            out.date(in.date());
            out.filename(in.filename());
            out.ipAddress(in.ipAddress());
            out.identifiedPayloadType(in.identifiedPayloadType());
            out.payloadDigest(in.payloadDigest());
            out.profile(in.profile());
            out.recordId(in.recordId());
            out.refersTo(in.refersTo());
            out.segmentNumber(in.segmentNumber());
            out.segmentOriginId(in.segmentOriginId());
            out.segmentTotalLength(in.segmentTotalLength());
            out.targetUri(in.targetUri());
            out.truncated(in.truncated());
            out.type(in.type());
            out.warcinfoId(in.warcinfoId());

            assertEquals(sortFields(example), sortFields(out.toString()));
        }
    }

    String sortFields(String text) {
        String[] split = text.split("\r\n", 2);
        String[] fieldLines = split[1].split("\r\n");
        Arrays.sort(fieldLines);
        return split[0] + "\r\n" + String.join("\r\n", fieldLines) + "\r\n\r\n";
    }
}
