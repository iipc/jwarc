/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.netpreserve.jwarc.tools.DigestEncodingUtils.*;

public class WarcDigestTest {

    private byte[] contentBytes = "hello world".getBytes();

    @Test
    public void testParsing() {
        WarcDigest digest = new WarcDigest("Sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN");
        assertEquals("Sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.raw());
        assertEquals("sha1", digest.algorithm());
        assertEquals("sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.prefixedBase32());
    }

    @Test
    public void testSha1() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(contentBytes);
        WarcDigest digest = new WarcDigest(md);
        assertEquals("sha1:FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.prefixedBase32());
        assertEquals("FKXGYNOJJ7H3IFO35FPUBC445EPOQRXN", digest.base32());
        assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", digest.hex());
        assertEquals("Kq5sNclPz7QV2+lfQIuc6R7oRu0=", digest.base64());
    }

    @Test
    public void testBase32()  {
        assertEquals("AELZ2347", base32Encode(base32Decode("aelz2347")));
        // test with and without padding (although obligatory following RFC 4648)
        assertEquals("AELZ2347AE======", base32Encode(base32Decode("aelz2347ae======")));
        assertEquals("AELZ2347AE======", base32Encode(base32Decode("aelz2347ae")));
    }

    @Test
    public void testBase64()  {
        assertEquals("ARedb58B", base64Encode(base64Decode("ARedb58B")));
        // test with and without padding (although obligatory following RFC 4648)
        assertEquals("ARedb58BAQ==", base64Encode(base64Decode("ARedb58BAQ==")));
        assertEquals("ARedb58BAQ==", base64Encode(base64Decode("ARedb58BAQ")));
    }

    @Test
    public void testEncodeHex() {
        assertEquals("000190ff", hexEncode(new byte[]{0x00, 0x01, (byte) 0x90, (byte) 0xff}));
        assertEquals("000190ff", hexEncode(hexDecode("000190ff")));
    }

    @Test
    public void testSha224() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-224");
        md.update(contentBytes);
        WarcDigest digest = new WarcDigest(md);
        assertEquals("sha224:F4CUO76CJO2PV36YMULRK3NP33HMIW4K2PHSKIVFMNMCW===", digest.prefixedBase32());
        assertEquals("F4CUO76CJO2PV36YMULRK3NP33HMIW4K2PHSKIVFMNMCW===", digest.base32());
        assertEquals("2f05477fc24bb4faefd86517156dafdecec45b8ad3cf2522a563582b", digest.hex());
        assertEquals("LwVHf8JLtPrv2GUXFW2v3s7EW4rTzyUipWNYKw==", digest.base64());
    }

    @Test
    public void testSha256() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(contentBytes);
        WarcDigest digest = new WarcDigest(md);
        assertEquals("sha256:XFGSPOMTJU7ARJJOKLL5U7NL7LCIJ37DPJJYB3UQRD32ZYXPZXUQ====", digest.prefixedBase32());
        assertEquals("XFGSPOMTJU7ARJJOKLL5U7NL7LCIJ37DPJJYB3UQRD32ZYXPZXUQ====", digest.base32());
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", digest.hex());
        assertEquals("uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=", digest.base64());
    }

    @Test
    public void testSha384() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        md.update(contentBytes);
        WarcDigest digest = new WarcDigest(md);
        assertEquals("sha384:7W6Y45NGP4U7OANE4BADQXROEOMGGA7KCARZEENPSB74XOBVPCZ6IF6LOHHGI3X5BAM53DAIRXQ32===",
                digest.prefixedBase32());
        assertEquals("7W6Y45NGP4U7OANE4BADQXROEOMGGA7KCARZEENPSB74XOBVPCZ6IF6LOHHGI3X5BAM53DAIRXQ32===",
                digest.base32());
        assertEquals(
                "fdbd8e75a67f29f701a4e040385e2e23986303ea10239211af907fcbb83578b3e417cb71ce646efd0819dd8c088de1bd",
                digest.hex());
        assertEquals("/b2OdaZ/KfcBpOBAOF4uI5hjA+oQI5IRr5B/y7g1eLPkF8txzmRu/QgZ3YwIjeG9", digest.base64());
    }

    /**
     * Test round-trip encoding for various message digests, cf.
     * <code>java.security.Security.getAlgorithms("MessageDigest")</code>
     */
    @Test
    public void testMessageDigests() throws NoSuchAlgorithmException {
        String[] algorithms = { "sha", "sha1", "sha224", "sha256", "sha384", "sha512", "md5" };
        for (String algorithm : algorithms) {
            MessageDigest md = DigestUtils.getDigester(algorithm);
            md.update(contentBytes);
            byte[] digest = md.digest(); // note: digest() resets the digester
            md.update(contentBytes);
            WarcDigest wd = new WarcDigest(md);
            assertTrue("Digest bytes not equal for " + algorithm, Arrays.equals(wd.bytes(), digest));
        }
    }

    @Test
    public void testBaseEncodingAutoDetection() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(contentBytes);
        byte[] digest = md.digest();
        WarcDigest wd = new WarcDigest("sha256:XFGSPOMTJU7ARJJOKLL5U7NL7LCIJ37DPJJYB3UQRD32ZYXPZXUQ====");
        assertTrue("Bytes not equal for Base32 encoded SHA-256 digest", Arrays.equals(wd.bytes(), digest));
        wd = new WarcDigest("sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
        assertTrue("Bytes not equal for Base16 encoded SHA-256 digest", Arrays.equals(wd.bytes(), digest));
        wd = new WarcDigest("sha256:uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=");
        assertTrue("Bytes not equal for Base64 encoded SHA-256 digest", Arrays.equals(wd.bytes(), digest));
    }
}