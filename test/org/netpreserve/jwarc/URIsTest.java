package org.netpreserve.jwarc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URIsTest {
    @Test
    public void toNormalizedSurt() {
        assertEquals("", URIs.toNormalizedSurt(""));
        assertEquals("warcinfo:test.warc.gz", URIs.toNormalizedSurt("warcinfo:test.warc.gz"));
        assertEquals("org,example:8080)/foo?&&a&b&c", URIs.toNormalizedSurt("http://wWw.EXAMPLE.org:8080/FOO?c&A&&&b"));
        assertEquals("filedesc:test.arc.gz", URIs.toNormalizedSurt("filedesc:test.arc.gz"));
        assertEquals("filedesc:/test.arc.gz", URIs.toNormalizedSurt("filedesc:/test.arc.gz"));
        assertEquals("filedesc://test.arc.gz", URIs.toNormalizedSurt("filedesc://test.arc.gz"));
        assertEquals("dns:example.com", URIs.toNormalizedSurt("dns:example.com"));
        assertEquals("org,example)/", URIs.toNormalizedSurt("http://www.example.org/"));
        assertEquals("org,example:1)/", URIs.toNormalizedSurt("http://www.example.org:1/"));
        assertEquals("org,example)/", URIs.toNormalizedSurt("http://www123.example.org/"));
        assertEquals("org,example)/", URIs.toNormalizedSurt("http://example.org/"));
        assertEquals("org,example)/", URIs.toNormalizedSurt("  http://example.org/   "));
        assertEquals("org,example)/", URIs.toNormalizedSurt("http://example.org:/"));
        assertEquals("org,example)/dir", URIs.toNormalizedSurt("http://example.org/dir/"));
        assertEquals("org,example)/dir", URIs.toNormalizedSurt("http://example.org/dir/?"));
        assertEquals("org,example)/dir?a&b", URIs.toNormalizedSurt("http://example.org/dir/?b&a"));
        assertEquals("org,example)/dir?a=1&a=2&b", URIs.toNormalizedSurt("http://example.org/dir/?a=2&b&a=1"));
        assertEquals("org,example)/a/c", URIs.toNormalizedSurt("http://example.org/a/././b/../c"));
        assertEquals("org,example)/", URIs.toNormalizedSurt("http://example.org/a/././b/../c/../../../../.."));
        assertEquals("2001:db8:0::1)/", URIs.toNormalizedSurt("http://[2001:db8:0::1]:80/"));
        assertEquals("2001:db8:0::1)/", URIs.toNormalizedSurt("http://[2001:db8:0::1]:/"));
        assertEquals("2001:db8:0::1)/", URIs.toNormalizedSurt("http://[2001:db8:0::1]/"));
        assertEquals("4,3,2,1)/", URIs.toNormalizedSurt("http://1.2.3.4:80/"));
        assertEquals("4,3,2,1)/", URIs.toNormalizedSurt("http://1.2.3.4:/"));
        assertEquals("4,3,2,1)/", URIs.toNormalizedSurt("http://1.2.3.4/"));
        assertEquals("ht%20tp)/ww%20w.exampl%20e.org%20", URIs.toNormalizedSurt("  ht tp://ww w.exampl e.org /  "));
        assertEquals("org,example)/", URIs.toNormalizedSurt("\tht\ttp://ww\tw.exampl\te.org\t/\t"));
        assertEquals("org,example)/index.php?page=2", URIs.toNormalizedSurt("http://example.org/index.php?PHPSESSID=0123456789abcdefghijklemopqrstuv&page=2"));
        assertEquals("com,host%23)/~a!b@c%23d$e%25f^00&11*22(33)44_55+", URIs.toNormalizedSurt("http://host%23.com/%257Ea%2521b%2540c%2523d%2524e%25f%255E00%252611%252A22%252833%252944_55%252B"));
        assertEquals("au,gov,wa,intersector)/current_issue?jsessionid=92303280691120833351543", URIs.toNormalizedSurt("http://intersector.wa.gov.au/current_issue?CFID=2051199&CFTOKEN=697395b12ed216e1-F6DFAF77-D433-FA57-5582BC6000844470&jsessionid=92303280691120833351543"));
        assertEquals("org,example)/mileg.aspx", URIs.toNormalizedSurt("http://example.org/(S(4hqa0555fwsecu455xqckv45))/mileg.aspx"));
        assertEquals("org,example)/mileg.aspx", URIs.toNormalizedSurt("http://example.org/(4hqa0555fwsecu455xqckv45)/mileg.aspx"));

        // python surt quirks
        assertEquals("org,example)/?foobar", URIs.toNormalizedSurt("http://example.org?foosid=3E544261B39C3B399E1C6BB38D6888E6&bar"));
        assertEquals("filedesc.example.com", URIs.toNormalizedSurt("filedesc.example.com"));
        assertEquals("com,example,filedesc)/", URIs.toNormalizedSurt("Filedesc.example.com"));
        assertEquals("urn:pageinfo:https://www.example.org", URIs.toNormalizedSurt("urn:pageinfo:https://www.example.org/"));
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