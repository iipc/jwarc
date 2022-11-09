package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WarcWriter;
import org.netpreserve.jwarc.net.WarcRecorder;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RecorderTool {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path caCertificateSaveFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p":
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--save-ca-certificate":
                    caCertificateSaveFile = Paths.get(args[++i]);
                    break;
                case "-h":
                case "--help":
                    System.err.println("Usage: jwarc recorder [options]");
                    System.err.println("  -p, --port <port>             Port to listen on (default: 8080)");
                    System.err.println("  --save-ca-certificate <file>  Saves the CA certificate as a PEM file");
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Try 'jwarc recorder --help' for more information.");
                    System.exit(1);
            }
        }

        WarcRecorder warcRecorder = new WarcRecorder(new ServerSocket(port), new WarcWriter(System.out));

        if (caCertificateSaveFile != null) {
            X509Certificate certificate = warcRecorder.certificateAuthority().certificate();
            Files.write(caCertificateSaveFile, pemEncode(certificate).getBytes(UTF_8));
        }

        warcRecorder.listen();
    }

    private static String pemEncode(X509Certificate certificate) throws CertificateEncodingException {
        String base64 = Base64.getEncoder().encodeToString(certificate.getEncoded());
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            builder.append(base64, i, Math.min(i + 64, base64.length()));
            builder.append('\n');
        }
        builder.append("-----END CERTIFICATE-----\n");
        return builder.toString();
    }
}
