package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.WarcRequest;
import org.netpreserve.jwarc.WarcResponse;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;

import static org.junit.Assert.*;
import static org.netpreserve.jwarc.WarcFilter.compile;

public class WarcFilterTest {

    @Test
    public void test() throws ParseException, IOException {
        WarcResponse response = new WarcResponse.Builder(URI.create("http://example.org/"))
                .setHeader("five", "5")
                .body(new HttpResponse.Builder(200, "OK")
                        .setHeader("Transfer-Encoding", "chunked")
                        .build())
                .build();
        assertTrue(compile("WARC-Type == \"response\"").test(response));
        assertTrue(compile("warc-typE==  \t \"response\"").test(response));
        assertFalse(compile("WARC-Type != \"response\"").test(response));
        assertTrue(compile("WARC-Target-URI =~ \"http:.*\"").test(response));
        assertFalse(compile("WARC-Target-URI =~ \"org\"").test(response));
        assertFalse(compile("WARC-Target-URI !~ \"http:.*\"").test(response));
        assertTrue(compile("content-length < 500").test(response));
        assertTrue(compile("warc-type <= 500").test(response));
        assertTrue(compile("five >= 5").test(response));
        assertTrue(compile("five == 5").test(response));
        assertTrue(compile(":status == 200").test(response));
        assertTrue(compile("http:transfer-encoding == \"chunked\"").test(response));
        assertTrue(compile("(((five >= 5)))").test(response));
        assertFalse(compile("!(five >= 5)").test(response));
        assertFalse(compile("five > 5").test(response));
        assertTrue(compile("five > 10 || five > 11 || five <= 5").test(response));
        assertFalse(compile("five < 10 && five > 10").test(response));
        assertTrue(compile("(five < 10 || five > 10) && five == \"5\"").test(response));
        assertFalse(compile("(five > 100) && five < 10").test(response));
        assertFalse(compile("(five < 10) && five > 100").test(response));
        assertFalse(compile("(five < 10 || five > 10) && five > 100").test(response));
    }

}