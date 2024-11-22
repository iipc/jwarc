package org.netpreserve.jwarc;

import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;
import static org.netpreserve.jwarc.InetAddresses.toAddrString;

public class InetAddressesTest {
    @Test
    public void testCanonicalInet6() throws Exception {
        assertEquals("2001:db8::1",
                toAddrString(InetAddress.getByName("2001:db8:0:0:0:0:0:1")));
        assertEquals("::",
                toAddrString(InetAddress.getByName("0:0:0:0:0:0:0:0")));
        assertEquals("::1",
                toAddrString(InetAddress.getByName("0:0:0:0:0:0:0:1")));
        assertEquals("2001:db8:1:1:1:1:1:1",
                toAddrString(InetAddress.getByName("2001:db8:1:1:1:1:1:1")));
        assertEquals("2001:0:0:1::1",
                toAddrString(InetAddress.getByName("2001:0:0:1:0:0:0:1")));
        assertEquals("2001:db8:f::1",
                toAddrString(InetAddress.getByName("2001:db8:000f:0:0:0:0:1")));
        assertEquals("2001:db8::1:0:0:1",
                toAddrString(InetAddress.getByName("2001:0db8:0000:0000:0001:0000:0000:0001")));
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
                toAddrString(InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertEquals("2001:200f::1",
                toAddrString(InetAddress.getByName("2001:200f:0:0:0:0:0:1")));
        // https://datatracker.ietf.org/doc/html/rfc5952#section-4.2.2
        // "The symbol "::" MUST NOT be used to shorten just one 16-bit 0 field."
        assertEquals("2001:0:3:4:5:6:7:8",
                toAddrString(InetAddress.getByName("2001:0:3:4:5:6:7:8")));
        // shorten first of same-length consecutive 0 fields, also in initial position
        assertEquals("::4:0:0:0:ffff",
                toAddrString(InetAddress.getByName("0:0:0:4:0:0:0:ffff")));
    }

}