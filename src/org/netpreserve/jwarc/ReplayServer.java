package org.netpreserve.jwarc;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ServerSocket serverSocket;
    private String firstUrl = null;
    private final Map<String, IndexEntry> index = new HashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private byte[] script = "<!doctype html><script src='/__jwarc__/inject.js'></script>".getBytes(US_ASCII);
    private CertificateAuthority ca;

    ReplayServer(ServerSocket serverSocket, List<Path> warcs) throws IOException {
        this.serverSocket = serverSocket;
        for (Path warc : warcs) {
            index(warc);
        }
    }

    private void index(Path warcFile) throws IOException {
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
    void listen() throws IOException {
        while (!serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            threadPool.execute(() -> interact(socket, ""));
        }
    }

    /**
     * Handles a connection from a client.
     *
     * @param socket
     */
    private void interact(Socket socket, String prefix) {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        try {
            while (!socket.isInputShutdown()) {
                HttpRequest request;
                try {
                    request = HttpRequest.parse(Channels.newChannel(socket.getInputStream()), buffer);
                } catch (SocketException | EOFException e) {
                    return; // client probably closed
                } catch (SSLProtocolException e) {
                    if (e.getCause() instanceof SocketException) {
                        return; // client probably closed
                    }
                    throw e;
                }
                handle(socket, prefix, request);
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

    private void handle(Socket socket, String prefix, HttpRequest request) throws Exception {
        String target = prefix + request.target();
        if (request.method().equals("CONNECT")) {
            send(socket, new HttpResponse.Builder(200, "OK").build());
            upgradeToTls(socket, target);
        } else if (target.equals("/")) {
            send(socket, new HttpResponse.Builder(307, "Redirect")
                    .addHeader("Connection", "close")
                    .addHeader("Location", "/replay/12345678901234/" + firstUrl)
                    .build());
        } else if (target.equals("/__jwarc__/sw.js")) {
            listen(socket, "sw.js");
        } else if (target.equals("/__jwarc__/inject.js")) {
            listen(socket, "inject.js");
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
    }

    private void upgradeToTls(Socket socket, String target) throws Exception {
        synchronized (this) {
            if (ca == null) {
                ca = new CertificateAuthority(new X500Principal("cn=Dummy CA"));
            }
        }
        String host = target.replaceFirst(":[0-9]+$", "");
        X509Certificate cert = ca.generateCertificate(new X500Principal("cn=" + host));
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{new X509KeyManager() {
            public X509Certificate[] getCertificateChain(String alias) {
                return new X509Certificate[] {
                        cert, ca.caCert
                };
            }
            public PrivateKey getPrivateKey(String s) {
                return ca.subKeyPair.getPrivate();
            }
            public String[] getClientAliases(String s, Principal[] principals) {
                throw new IllegalStateException();
            }
            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                throw new IllegalStateException();
            }
            public String[] getServerAliases(String s, Principal[] principals) {
                return new String[]{host};
            }
            public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                return host;
            }
        }}, null, null);
        SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, null, true);
        sslSocket.setUseClientMode(false);
        sslSocket.startHandshake();
        interact(sslSocket, "https://" + target.replaceFirst(":443$", ""));
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
                b.setHeader("Connection", "keep-alive");
                MessageBody body = http.body();
                if (HTML.equals(http.contentType().base())) {
                    body = new LengthedBody(body, ByteBuffer.wrap(script), script.length + body.size());
                }
                b.body(http.contentType(), body, body.size());
                send(socket, b.build());
            }
        } else {
            send(socket, new HttpResponse.Builder(404, "Not found")
                    .body(HTML, "Not found".getBytes(UTF_8))
                    .setHeader("Connection", "keep-alive")
                    .build());
        }
    }

    private void listen(Socket socket, String resource) throws IOException {
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
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.serializeHeader());
            IOUtils.copy(response.body().stream(), outputStream);
        } catch (SSLProtocolException | SocketException e) {
            socket.close(); // client probably closed
        }
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
