package org.netpreserve.jwarc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URIsTest {
    @Test
    public void toNormalizedSurt() {
            assertEquals("org,example:8080)/foo?&&a&b&c", URIs.toNormalizedSurt("http://wWw.EXAMPLE.org:8080/FOO?c&A&&&b"));
    }
}