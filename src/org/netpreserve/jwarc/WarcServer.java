package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.netpreserve.jwarc.HttpServer.send;

/**
 * A primitive WARC replay server.
 * <p>
 * Mainly exists to exercise the API. Can be used either as a proxy or in link-rewriting mode. Link-rewriting is
 * handled client-side by https://github.com/oduwsdl/Reconstructive
 */
class WarcServer {
    private static final MediaType HTML = MediaType.parse("text/html");
    private final HttpServer httpServer;
    private String firstUrl = null;
    private final Map<String, IndexEntry> index = new HashMap<>();
    private byte[] script = "<!doctype html><script src='/__jwarc__/inject.js'></script>".getBytes(US_ASCII);

    WarcServer(ServerSocket serverSocket, List<Path> warcs) throws IOException {
        httpServer = new HttpServer(serverSocket, this::handle);
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
        httpServer.listen();
    }

    private void handle(Socket socket, String target, HttpRequest request) throws Exception {
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
            replay(socket, target, true);
        } else {
            replay(socket, target, false);
        }
    }

    private void replay(Socket socket, String target, boolean inject) throws IOException {
        IndexEntry entry = index.get(target);
        if (entry != null) {
            try (FileChannel channel = FileChannel.open(entry.file, READ)) {
                channel.position(entry.position);
                WarcReader reader = new WarcReader(channel);
                WarcResponse record = (WarcResponse) reader.next().get();
                HttpResponse http = record.http();
                HttpResponse.Builder b = new HttpResponse.Builder(http.status(), http.reason());
                for (Map.Entry<String, List<String>> e : http.headers().map().entrySet()) {
                    if (e.getKey().equalsIgnoreCase("Strict-Transport-Security")) continue;
                    if (e.getKey().equalsIgnoreCase("Transfer-Encoding")) continue;
                    if (e.getKey().equalsIgnoreCase("Public-Key-Pins")) continue;
                    for (String value : e.getValue()) {
                        b.addHeader(e.getKey(), value);
                    }
                }
                b.setHeader("Connection", "keep-alive");
                b.setHeader("Memento-Datetime", RFC_1123_DATE_TIME.format(record.date().atOffset(UTC)));
                MessageBody body = http.body();
                if (inject && HTML.equals(http.contentType().base())) {
                    body = LengthedBody.create(body, ByteBuffer.wrap(script), script.length + body.size());
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

    private static class IndexEntry {
        private final Path file;
        private final long position;

        IndexEntry(Path file, long position) {
            this.file = file;
            this.position = position;
        }
    }
}
