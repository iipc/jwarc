package org.netpreserve.jwarc;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;

/**
 * A primitive WARC replay server.
 * <p>
 * Mainly exists to exercise the API. Can be used either as a proxy or in link-rewriting mode. Link-rewriting is
 * handled client-side by https://github.com/oduwsdl/Reconstructive
 */
class ReplayServer {
    private static final MediaType HTML = MediaType.parse("text/html");
    private String firstUrl = null;
    private final Map<String, IndexEntry> index = new HashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final InetSocketAddress address;
    private byte[] script = "<!doctype html><script src='/__jwarc__/inject.js'></script>".getBytes(US_ASCII);

    ReplayServer(InetSocketAddress address) {
        this.address = address;
    }

    void index(Path warcFile) throws IOException {
        try (WarcReader reader = new WarcReader(warcFile)) {
            for (WarcRecord record : reader) {
                if ((record instanceof WarcResponse || record instanceof WarcResource)) {
                    WarcCaptureRecord capture = (WarcCaptureRecord) record;
                    String scheme = capture.targetURI().getScheme();
                    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                        String url = capture.targetURI().toString();
                        index.put(url, new IndexEntry(warcFile, reader.position()));
                        if (firstUrl == null && HTML.equals(capture.payloadType().base())) {
                            firstUrl = url;
                        }
                    }
                }
            }
        }
    }

    /**
     * Listens and accepts new connections.
     */
    void serve() throws IOException {
        try (ServerSocket listener = new ServerSocket()) {
            listener.bind(address);
            while (!listener.isClosed()) {
                Socket socket = listener.accept();
                threadPool.execute(() -> interact(socket));
            }
        }
    }

    /**
     * Handles a connection from a client.
     * @param socket
     */
    private void interact(Socket socket) {
        try {
            HttpRequest request = HttpRequest.parse(Channels.newChannel(socket.getInputStream()));
            String target = request.target();
            if (request.method().equals("CONNECT")) {
                send(socket, new HttpResponse.Builder(200, "OK").build());
                upgradeToTls(socket, target.replaceFirst(":[0-9]+$", ""));
            } else if (target.equals("/")) {
                send(socket, new HttpResponse.Builder(307, "Redirect")
                        .addHeader("Connection", "close")
                        .addHeader("Location", "/replay/12345678901234/" + firstUrl)
                        .build());
            } else if (target.equals("/__jwarc__/sw.js")) {
                serve(socket, "sw.js");
            } else if (target.equals("/__jwarc__/inject.js")) {
                serve(socket, "inject.js");
            } else if (target.startsWith("/replay/")) {
                if (!request.headers().first("x-serviceworker").isPresent()) {
                    send(socket, new HttpResponse.Builder(200, "OK")
                            .body(HTML, script)
                            .setHeader("Connection", "close")
                            .build());
                }
                int i = target.indexOf('/', "/replay/".length());
                if (i != -1) {
                    target = target.substring(i + 1);
                }
                replay(socket, target);
            } else {
                replay(socket, target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void upgradeToTls(Socket socket, String host) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        generateCertificate(host, keyStore);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, null);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, null, true);
        sslSocket.setUseClientMode(false);
        sslSocket.startHandshake();
        interact(sslSocket);
    }

    /**
     * Generate a self-signed certificate.
     *
     * We're naughtily using sun.security so that we don't have to add a dependency on Bouncy Castle.
     * Therefore load the classes using reflection so that only TLS mode breaks if run on a JVM without it.
     */
    private void generateCertificate(String host, KeyStore keyStore) throws Exception {
        Class<?> certGenClass = Class.forName("sun.security.tools.keytool.CertAndKeyGen");
        Class<?> x500NameClass = Class.forName("sun.security.x509.X500Name");
        Object x500Name = x500NameClass.getConstructor(String.class).newInstance("cn=" + host);
        Object certGen = certGenClass.getConstructor(String.class, String.class).newInstance("EC", "SHA256withECDSA");
        certGenClass.getMethod("generate", int.class).invoke(certGen, 256);
        X509Certificate cert = (X509Certificate)certGenClass.getMethod("getSelfCertificate", x500NameClass, long.class)
                .invoke(certGen, x500Name, TimeUnit.DAYS.toSeconds(365));
        PrivateKey key = (PrivateKey) certGenClass.getMethod("getPrivateKey").invoke(certGen);
        keyStore.setKeyEntry(host, key, null, new Certificate[]{cert});
    }

    private void replay(Socket socket, String target) throws IOException {
        IndexEntry entry = index.get(target);
        if (entry != null) {
            try (FileChannel channel = FileChannel.open(entry.file, READ)) {
                channel.position(entry.position);
                WarcReader reader = new WarcReader(channel);
                HttpResponse http = ((WarcResponse) reader.next().get()).http();
                HttpResponse.Builder b = new HttpResponse.Builder(http.status(), http.reason());
                for (Map.Entry<String, List<String>> e : http.headers().map().entrySet()) {
                    for (String value : e.getValue()) {
                        b.addHeader(e.getKey(), value);
                    }
                }
                b.setHeader("Connection", "close");
                MessageBody body = http.body();
                if (HTML.equals(http.contentType().base())) {
                    body = new MessageBody(body, ByteBuffer.wrap(script), script.length + body.size());
                }
                b.body(http.contentType(), body, body.size());
                send(socket, b.build());
            }
        } else {
            send(socket, new HttpResponse.Builder(404, "Not found")
                    .body(HTML, "Not found".getBytes(UTF_8))
                    .setHeader("Connection", "close")
                    .build());
        }
    }

    private void serve(Socket socket, String resource) throws IOException {
        URLConnection conn = getClass().getResource(resource).openConnection();
        try (InputStream stream = conn.getInputStream()) {
            send(socket, new HttpResponse.Builder(200, "OK")
                    .body(MediaType.parse("application/javascript"), Channels.newChannel(stream), conn.getContentLengthLong())
                    .setHeader("Connection", "close")
                    .setHeader("Service-Worker-Allowed", "/")
                    .build());
        }
    }

    private void send(Socket socket, HttpResponse response) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(response.serializeHeader());
        IOUtils.copy(response.body().stream(), outputStream);
    }

    private static class IndexEntry {
        private final Path file;
        private final long position;

        IndexEntry(Path file, long position) {
            this.file = file;
            this.position = position;
        }
    }
}
