package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Comparator.comparing;
import static org.netpreserve.jwarc.HttpServer.send;

/**
 * A primitive WARC replay server.
 * <p>
 * Mainly exists to exercise the API. Can be used either as a proxy or in link-rewriting mode. Link-rewriting is
 * handled client-side by https://github.com/oduwsdl/Reconstructive
 */
class WarcServer {
    private static final DateTimeFormatter ARC_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);
    private static final MediaType HTML = MediaType.parse("text/html");
    private static final Pattern REPLAY_RE = Pattern.compile("/replay/([0-9]{14})/(.*)");

    private final HttpServer httpServer;
    private final Index index = new Index();
    private byte[] script = "<!doctype html><script src='/__jwarc__/inject.js'></script>".getBytes(US_ASCII);
    private Entry entrypoint;

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
                        Entry entry = new Entry(capture.targetURI(), capture.date(), warcFile, reader.position());
                        index.add(entry);
                        if (entrypoint == null && HTML.equals(capture.payloadType().base())) {
                            entrypoint = entry;
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
            if (entrypoint == null) {
                error(socket, 404, "Empty collection");
                return;
            }
            send(socket, new HttpResponse.Builder(307, "Redirect")
                    .addHeader("Connection", "close")
                    .addHeader("Location", "/replay/" + ARC_DATE.format(entrypoint.date) + "/" + entrypoint.uri)
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
            Matcher m = REPLAY_RE.matcher(target);
            if (!m.matches()) {
                error(socket, 404, "Malformed replay url");
            }
            Instant date = Instant.from(ARC_DATE.parse(m.group(1)));
            replay(socket, m.group(2), date, true);
        } else {
            Instant date = request.headers().first("Accept-Datetime")
                    .map(s -> Instant.from(RFC_1123_DATE_TIME.parse(s)))
                    .orElse(Instant.EPOCH);
            replay(socket, target, date, false);
        }
    }

    private void replay(Socket socket, String target, Instant date, boolean inject) throws IOException {
        Entry entry = index.closest(URI.create(target), date);
        if (entry == null) {
            error(socket, 404, "Not found in archive");
            return;
        }
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
    }

    private void error(Socket socket, int status, String reason) throws IOException {
        send(socket, new HttpResponse.Builder(status, reason)
                .body(HTML, reason.getBytes(UTF_8))
                .setHeader("Connection", "keep-alive")
                .build());
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

    private static class Index {
        NavigableSet<Entry> entries = new TreeSet<>(comparing((Entry e) -> e.urikey).thenComparing(e -> e.date));

        void add(Entry entry) {
            entries.add(entry);
        }

        Entry closest(URI uri, Instant date) {
            NavigableSet<Entry> versions = versions(uri);
            Entry key = new Entry(uri, date);
            Entry a = versions.floor(key);
            Entry b = versions.higher(key);
            if (a == null) return b;
            if (b == null) return a;
            Duration da = Duration.between(a.date, date);
            Duration db = Duration.between(b.date, date);
            return da.compareTo(db) < 0 ? a : b;
        }

        NavigableSet<Entry> versions(URI uri) {
            return entries.subSet(new Entry(uri, Instant.MIN), true, new Entry(uri, Instant.MAX), true);
        }
    }

    private static class Entry {
        private final String urikey;
        private final URI uri;
        private final Instant date;
        private final Path file;
        private final long position;

        Entry(URI uri, Instant date) {
            this(uri, date, null, -1);
        }

        Entry(URI uri, Instant date, Path file, long position) {
            urikey = uri.toString();
            this.uri = uri;
            this.date = date;
            this.file = file;
            this.position = position;
        }
    }
}
