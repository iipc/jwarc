/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Assert;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class WarcDigestTest {

    @Test
    public void test() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update("hello world".getBytes());
        WarcDigest digest = new WarcDigest(md);
        assertEquals("sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.prefixedBase32());
        assertEquals("FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.base32());
        assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", digest.hex());
    }

    @Test
    public void test2()  {
        assertEquals("AELZ2347", WarcDigest.base32Encode(WarcDigest.base32Decode("aelz2347")));
    }

    @Test
    public void testEncodeHex() {
        assertEquals("000190ff", WarcDigest.hexEncode(new byte[]{0x00, 0x01, (byte) 0x90, (byte) 0xff}));
    }
}