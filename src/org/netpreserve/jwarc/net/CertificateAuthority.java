package org.netpreserve.jwarc.net;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class CertificateAuthority {
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

    X509Certificate issue(X500Principal subject) throws GeneralSecurityException {
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
        byte[] extensions;
        if (isCA) {
            extensions = new byte[]{(byte) 0xA3, 0x16, 0x30, 0x14, 0x30, 0x12, 0x06, 0x03, 0x55, 0x1D, 0x13, 0x01, 0x01,
                    (byte) 0xFF, 0x04, 0x08, 0x30, 0x06, 0x01, 0x01, (byte) 0xFF, 0x02, 0x01, 0x0C};
        } else {
            byte[] subjectAltNameExtension = derSequence(new byte[]{0x06, 0x03, 0x55, 0x1D, 0x11},
                    tag(0x04, derSequence(tag(0x82, subject.getName().split("=")[1].getBytes(US_ASCII)))));
            extensions = tag(0xA3, derSequence(subjectAltNameExtension));
        }
        byte[] rawCert = derSequence(preamble, serialBytes, algorithm, issuer.getEncoded(), validity,
                subject.getEncoded(), subjectKey.getEncoded(), extensions);
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(issuerKey);
        signature.update(rawCert);
        byte[] sigBytes = signature.sign();
        byte[] cert = derSequence(rawCert, algorithm, new byte[]{0x3, (byte) (sigBytes.length + 1), 0x0}, sigBytes);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
    }

    private static byte[] tag(int tag, byte[] value) {
        byte[] length = derLength(value.length);
        return concat(new byte[]{(byte) tag}, length, value);
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] out = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, out, offset, array.length);
            offset += array.length;
        }
        return out;
    }

    private static byte[] derSequence(byte[]... arrays) {
        int length = 0;
        for (byte[] a : arrays) {
            length += a.length;
        }
        byte[] encodedLength = derLength(length);
        byte[] out = new byte[length + encodedLength.length + 1];
        out[0] = 0x30;
        System.arraycopy(encodedLength, 0, out, 1, encodedLength.length);
        int pos = encodedLength.length + 1;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, pos, a.length);
            pos += a.length;
        }
        return out;
    }

    private static byte[] derLength(int len) {
        if (len < 128) return new byte[]{(byte) len};
        else if (len < 256) return new byte[]{(byte) 0x81, (byte) len};
        else if (len < 65536) return new byte[]{(byte) 0x82, (byte) (len >> 8), (byte) len};
        else throw new IllegalArgumentException("too large");
    }

    public X509Certificate certificate() {
        return caCert;
    }

    public static void main(String[] args) throws GeneralSecurityException {
        X509Certificate cert = new CertificateAuthority(new X500Principal("CN=ca"))
                .issue(new X500Principal("CN=www.example.org"));
        System.out.println(Base64.getEncoder().encodeToString(cert.getEncoded()));
    }
}
