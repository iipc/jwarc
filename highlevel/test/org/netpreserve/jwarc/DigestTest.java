/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class DigestTest {

    @Test
    public void test() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update("hello world".getBytes());
        assertEquals("sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", new Digest(md).toPrefixedBase32());
    }

    @Test
    public void test2()  {
        assertEquals("AELZ2347", Digest.base32Encode(Digest.base32Decode("aelz2347")));
    }
}