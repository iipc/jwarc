package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
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
        try (ServerSocketChannel listener = ServerSocketChannel.open()) {
            listener.bind(address);
            while (listener.isOpen()) {
                SocketChannel socket = listener.accept();
                threadPool.execute(() -> interact(socket));
            }
        }
    }

    /**
     * Handles a connection from a client.
     */
    private void interact(SocketChannel socket) {
        try {
            HttpRequest request = HttpRequest.parse(socket);
            String target = request.target();
            if (target.equals("/")) {
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

    private void replay(SocketChannel socket, String target) throws IOException {
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

    private void serve(SocketChannel socket, String resource) throws IOException {
        URLConnection conn = getClass().getResource(resource).openConnection();
        try (InputStream stream = conn.getInputStream()) {
            send(socket, new HttpResponse.Builder(200, "OK")
                    .body(MediaType.parse("application/javascript"), Channels.newChannel(stream), conn.getContentLengthLong())
                    .setHeader("Connection", "close")
                    .setHeader("Service-Worker-Allowed", "/")
                    .build());
        }
    }

    private void send(SocketChannel socket, HttpResponse response) throws IOException {
        OutputStream outputStream = socket.socket().getOutputStream();
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
