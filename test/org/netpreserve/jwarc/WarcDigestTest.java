/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Assert;
import org.junit.Test;
import org.netpreserve.jwarc.WarcDigest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class WarcDigestTest {

    @Test
    public void test() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update("hello world".getBytes());
        Assert.assertEquals("sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", new WarcDigest(md).toPrefixedBase32());
    }

    @Test
    public void test2()  {
        assertEquals("AELZ2347", WarcDigest.base32Encode(WarcDigest.base32Decode("aelz2347")));
    }
}