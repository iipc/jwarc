package org.netpreserve.jwarc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URIsTest {
    @Test
    public void toNormalizedSurt() {
            assertEquals("org,example:8080)/foo?&&a&b&c", URIs.toNormalizedSurt("http://wWw.EXAMPLE.org:8080/FOO?c&A&&&b"));
    }

    @Test
    public void testParseLeniently() {
        roundtripParseLeniently("");
        roundtripParseLeniently("https://www.example.com#anchor");
        roundtripParseLeniently("https://example.com?a=b&cd[]=4");
        roundtripParseLeniently("/path/to/resource");
        roundtripParseLeniently("http://[2001:db8::1]/resource");
        roundtripParseLeniently("https://example.com/path%20with%20spaces");
        roundtripParseLeniently("https://example.com#fragment%20with%20spaces");
        roundtripParseLeniently("https://example.com?query%20with%20spaces");
        roundtripParseLeniently("https://example.com/路径");
        roundtripParseLeniently("https://example.com?query=测试");
        roundtripParseLeniently("https://////example.com?query=测试");
        roundtripParseLeniently("https://www.prijmeni.cz/Kr%C3%A1kora");
        roundtripParseLeniently("https://dx.doi.org/10.1038%2F35008096");

        assertEquals("https://example.com/path%20with%20spaces", URIs.parseLeniently("https://example.com/path with spaces").toString());
        assertEquals("https://example.com?query%20with%20spaces", URIs.parseLeniently("https://example.com?query with spaces").toString());
        assertEquals("https://example.com#fragment%20with%20spaces", URIs.parseLeniently("https://example.com#fragment with spaces").toString());
        assertEquals("https://example.com/a%20b%25", URIs.parseLeniently("https://example.com/a b%25").toString());
        assertEquals("https://example.com/a%20b路径", URIs.parseLeniently("https://example.com/a b路径").toString());
        assertEquals("https://example.com?a%20b%25", URIs.parseLeniently("https://example.com?a b%25").toString());
        assertEquals("https://example.com?a%20b路径", URIs.parseLeniently("https://example.com?a b路径").toString());
        assertEquals("https://example.com#a%20b%25", URIs.parseLeniently("https://example.com#a b%25").toString());
        assertEquals("https://example.com/a%20b%25路径%5b?a%20b%25路径[?#a%20b%25路径[?", URIs.parseLeniently("https://example.com/a b%25路径[?a b%25路径[?#a b%25路径[?").toString());
        assertEquals("https://example.com/a%20b?c%20d#e%20f", URIs.parseLeniently("https://example.com/a b?c d#e f").toString());
    }

    private void roundtripParseLeniently(String s) {
        assertEquals(s, URIs.parseLeniently(s).toString());
    }
}