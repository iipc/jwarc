package org.netpreserve.jwarc;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;

class CertificateAuthority {
    private final KeyPair caKeyPair;
    final KeyPair subKeyPair;
    final X509Certificate caCert;
    AtomicInteger serial = new AtomicInteger((int) (System.currentTimeMillis() >> 8));

    CertificateAuthority(X500Principal caName) throws GeneralSecurityException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        caKeyPair = keyGen.generateKeyPair();
        subKeyPair = keyGen.generateKeyPair();
        caCert = signCertificate(caName, caKeyPair.getPrivate(), caName, caKeyPair.getPublic(), serial.getAndIncrement(), true);
    }

    X509Certificate generateCertificate(X500Principal subject) throws GeneralSecurityException {
        return signCertificate(caCert.getSubjectX500Principal(), caKeyPair.getPrivate(), subject,
                subKeyPair.getPublic(), serial.getAndIncrement(), false);
    }

    private static X509Certificate signCertificate(X500Principal issuer, PrivateKey issuerKey, X500Principal subject, PublicKey subjectKey, int serial, boolean isCA) throws GeneralSecurityException {
        // handy tool for decoding this stuff: https://lapo.it/asn1js/
        byte[] algorithm = {0x30, 0xc, 0x6, 0x8, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x4, 0x3, 0x2, 0x5, 0x0};
        byte[] preamble = {(byte) 0xa0, 0x3, 0x2, 0x1, 0x2};
        byte[] serialBytes = {0x2, 0x4, (byte) (serial >> 24), (byte) (serial >> 16), (byte) (serial >> 8), (byte) serial};
        byte[] validity = {0x30, 0x1e, 0x17, 0xd, 0x31, 0x39, 0x30, 0x32, 0x31, 0x31, 0x30, 0x37, 0x31, 0x37, 0x33,
                0x30, 0x5a, 0x17, 0xd, 0x33, 0x34, 0x30, 0x32, 0x31, 0x31, 0x30, 0x37, 0x31, 0x37, 0x33, 0x30, 0x5a};
        byte[] constraints;
        if (isCA) {
            constraints = new byte[]{(byte) 0xA3, 0x16, 0x30, 0x14, 0x30, 0x12, 0x06, 0x03, 0x55, 0x1D, 0x13, 0x01, 0x01,
                    (byte) 0xFF, 0x04, 0x08, 0x30, 0x06, 0x01, 0x01, (byte) 0xFF, 0x02, 0x01, 0x0C};
        } else {
            constraints = new byte[]{};
        }
        byte[] rawCert = derSequence(preamble, serialBytes, algorithm, issuer.getEncoded(), validity,
                subject.getEncoded(), subjectKey.getEncoded(), constraints);
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(issuerKey);
        signature.update(rawCert);
        byte[] sigBytes = signature.sign();
        byte[] cert = derSequence(rawCert, algorithm, new byte[]{0x3, (byte) (sigBytes.length + 1), 0x0}, sigBytes);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
    }

    private static byte[] derSequence(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) {
            len += a.length;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x30);
            baos.write(derLength(len));
            for (byte[] a : arrays) {
                baos.write(a);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("impossible", e);
        }
    }

    private static byte[] derLength(int len) {
        if (len < 128) return new byte[]{(byte) len};
        else if (len < 256) return new byte[]{(byte) 0x81, (byte) len};
        else if (len < 65536) return new byte[]{(byte) 0x82, (byte) (len >> 8), (byte) len};
        else throw new IllegalArgumentException("too large");
    }
}
