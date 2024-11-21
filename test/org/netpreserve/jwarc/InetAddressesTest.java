package org.netpreserve.jwarc;

import org.junit.Test;

import java.net.Inet6Address;
import java.net.InetAddress;

import static org.junit.Assert.*;
import static org.netpreserve.jwarc.InetAddresses.canonicalInet6;

public class InetAddressesTest {
    @Test
    public void testCanonicalInet6() throws Exception {
        assertEquals("2001:db8::1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:db8:0:0:0:0:0:1")));
        assertEquals("::",
                canonicalInet6((Inet6Address) InetAddress.getByName("0:0:0:0:0:0:0:0")));
        assertEquals("::1",
                canonicalInet6((Inet6Address) InetAddress.getByName("0:0:0:0:0:0:0:1")));
        assertEquals("2001:db8:1:1:1:1:1:1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:db8:1:1:1:1:1:1")));
        assertEquals("2001:0:0:1::1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:0:0:1:0:0:0:1")));
        assertEquals("2001:db8:f::1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:db8:000f:0:0:0:0:1")));
        assertEquals("2001:db8::1:0:0:1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:0db8:0000:0000:0001:0000:0000:0001")));
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
                canonicalInet6((Inet6Address) InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertEquals("2001:200f::1",
                canonicalInet6((Inet6Address) InetAddress.getByName("2001:200f:0:0:0:0:0:1")));
    }

}