package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WarcWriter;
import org.netpreserve.jwarc.net.WarcRecorder;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RecorderTool {
    public static void main(String[] args) throws Exception {
        int port = -1;
        Path caCertificateSaveFile = null;
        Path outputFile = null;
        List<String> commandToRun = new ArrayList<>();

        outer: for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                case "--output-file":
                    outputFile = Paths.get(args[++i]);
                    break;
                case "-p":
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--save-ca-certificate":
                    caCertificateSaveFile = Paths.get(args[++i]);
                    break;
                case "-h":
                case "--help":
                    System.out.println("Usage: jwarc recorder [options] [command [args...]]");
                    System.out.println("  -o, --output-file <file>      Write WARC to <file> instead of STDOUT");
                    System.out.println("  -p, --port <port>             Port to listen on (default: 8080)");
                    System.out.println("  --save-ca-certificate <file>  Saves the CA certificate as a PEM file");
                    System.out.println();
                    System.out.println("If a command is specified, it will be run and passed the following environment variables:");
                    System.out.println("   https_proxy, https_proxy");
                    System.out.println("                   The proxy URL to use for HTTP and HTTPS requests");
                    System.out.println("   CURL_CA_BUNDLE, SSL_CERT_PATH");
                    System.out.println("                   Path to the generated CA certificate file");
                    System.exit(0);
                    break;
                default:
                    if (!args[i].startsWith("-")) {
                        commandToRun = Arrays.asList(args).subList(i, args.length);
                        break outer;
                    }
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Try 'jwarc recorder --help' for more information.");
                    System.exit(1);
            }
        }

        if (port == -1) {
            if (commandToRun.isEmpty()) {
                port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            } else {
                port = 0; // use an automatic port when running a command
            }
        }

        if (caCertificateSaveFile == null && !commandToRun.isEmpty()) {
            caCertificateSaveFile = Files.createTempFile("jwarc-ca-certificate", ".pem");
        }

        ServerSocket serverSocket = new ServerSocket(port);
        WarcRecorder warcRecorder = new WarcRecorder(serverSocket, new WarcWriter(outputFile == null ? System.out : Files.newOutputStream(outputFile)));

        if (caCertificateSaveFile != null) {
            X509Certificate certificate = warcRecorder.certificateAuthority().certificate();
            Files.write(caCertificateSaveFile, pemEncode(certificate).getBytes(UTF_8));
        }

        if (commandToRun.isEmpty()) {
            warcRecorder.listen();
            return;
        }

        Thread listenerThread = new Thread(warcRecorder::listen);
        listenerThread.start();

        ProcessBuilder processBuilder = new ProcessBuilder(commandToRun);
        processBuilder.environment().put("http_proxy", "http://localhost:" + serverSocket.getLocalPort());
        processBuilder.environment().put("https_proxy", "http://localhost:" + serverSocket.getLocalPort());
        if (caCertificateSaveFile != null) {
            processBuilder.environment().put("CURL_CA_BUNDLE", caCertificateSaveFile.toAbsolutePath().toString());
            processBuilder.environment().put("SSL_CERT_FILE", caCertificateSaveFile.toAbsolutePath().toString());
        }
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        process.waitFor();

        if (caCertificateSaveFile != null) {
            Files.delete(caCertificateSaveFile);
        }

        serverSocket.close();
        listenerThread.join();
        System.exit(process.exitValue());
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
