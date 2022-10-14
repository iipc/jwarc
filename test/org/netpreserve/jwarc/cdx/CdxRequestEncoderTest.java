package org.netpreserve.jwarc.cdx;

import org.junit.Test;
import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.MediaType;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class CdxRequestEncoderTest {
    @Test
    public void test() throws IOException {
        assertEquals("__wb_method=POST&__wb_post_data=Zm9vPWJhciZkaXI9JTJGYmF6", CdxRequestEncoder.encode(new HttpRequest.Builder("POST", "/foo")
                .body(MediaType.OCTET_STREAM, "foo=bar&dir=%2Fbaz".getBytes(UTF_8)).build()));
        assertEquals("__wb_method=PUT&foo=bar&dir=/baz", CdxRequestEncoder.encode(new HttpRequest.Builder("PUT", "/foo")
                .body(MediaType.WWW_FORM_URLENCODED, "foo=bar&dir=%2Fbaz".getBytes(UTF_8)).build()));
        assertEquals("__wb_method=PUT&__wb_post_data=/w==", CdxRequestEncoder.encode(new HttpRequest.Builder("PUT", "/foo")
                .body(MediaType.WWW_FORM_URLENCODED, new byte[]{-1}).build()));
        assertEquals("__wb_method=POST&a=b&a.2_=2&d=e", CdxRequestEncoder.encode(new HttpRequest.Builder("POST", "/")
                .body(MediaType.JSON, ("{\"a\": \"b\", \"c\": {\"a\": 2}, \"d\": \"e\"}").getBytes(UTF_8)).build()));
        assertEquals("__wb_method=POST&type=event&id=44.0&values=True&values.2_=False&values.3_=None" +
                "&type.2_=component&id.2_=a%2Bb%26c%3D+d&values.4_=3&values.5_=4",
                CdxRequestEncoder.encode(new HttpRequest.Builder("POST", "/events")
                .body(MediaType.JSON, ("{\n" +
                        "   \"type\": \"event\",\n" +
                        "   \"id\": 44.0,\n" +
                        "   \"values\": [true, false, null],\n" +
                        "   \"source\": {\n" +
                        "      \"type\": \"component\",\n" +
                        "      \"id\": \"a+b&c= d\",\n" +
                        "      \"values\": [3, 4]\n" +
                        "   }\n" +
                        "}\n").getBytes(UTF_8)).build()));
    }
}