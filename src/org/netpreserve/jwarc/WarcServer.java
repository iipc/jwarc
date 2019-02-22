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
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.netpreserve.jwarc.HttpServer.send;
import static org.netpreserve.jwarc.MediaType.HTML;

/**
 * A primitive WARC replay server.
 * <p>
 * Mainly exists to exercise the API. Can be used either as a proxy or in link-rewriting mode. Link-rewriting is
 * handled client-side by https://github.com/oduwsdl/Reconstructive
 */
class WarcServer {
    private static final DateTimeFormatter ARC_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);
    private static final DateTimeFormatter RFC_1123_UTC = RFC_1123_DATE_TIME.withZone(UTC);
    private static final MediaType LINK_FORMAT = MediaType.parse("application/link-format");
    private static final Pattern REPLAY_RE = Pattern.compile("/replay/([0-9]{14})/(.*)");

    private final HttpServer httpServer;
    private final CaptureIndex index;
    private byte[] script = "<!doctype html><script src='/__jwarc__/inject.js'></script>\n".getBytes(US_ASCII);

    WarcServer(ServerSocket serverSocket, List<Path> warcs) throws IOException {
        httpServer = new HttpServer(serverSocket, this::handle);
        index = new CaptureIndex(warcs);
    }

    /**
     * Listens and accepts new connections.
     */
    void listen() throws IOException {
        httpServer.listen();
    }

    private void handle(Socket socket, String target, HttpRequest request) throws Exception {
        if (target.equals("/")) {
            Capture entrypoint = index.entrypoint();
            if (entrypoint == null) {
                error(socket, 404, "Empty collection");
                return;
            }
            send(socket, new HttpResponse.Builder(307, "Redirect")
                    .addHeader("Content-Length", "0")
                    .addHeader("Location", "/replay/" + ARC_DATE.format(entrypoint.date()) + "/" + entrypoint.uri())
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
                return;
            }
            Matcher m = REPLAY_RE.matcher(target);
            if (!m.matches()) {
                error(socket, 404, "Malformed replay url");
                return;
            }
            Instant date = Instant.from(ARC_DATE.parse(m.group(1)));
            replay(socket, m.group(2), date, false);
        } else if (target.startsWith("/timemap/")) {
            URI uri = URI.create(target.substring("/timemap/".length()));
            NavigableSet<Capture> versions = index.query(uri);
            if (versions.isEmpty()) {
                error(socket, 404, "Not found in archive");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(versions.first().uri()).append(">;rel=\"original\"");
            for (Capture entry : versions) {
                sb.append(",\n</replay/").append(ARC_DATE.format(entry.date())).append("/").append(entry.uri())
                        .append(">;rel=\"memento\",datetime=\"").append(RFC_1123_UTC.format(entry.date()) + "\"");
            }
            sb.append("\n");
            send(socket, new HttpResponse.Builder(200, "OK")
                    .body(LINK_FORMAT, sb.toString().getBytes(UTF_8)).build());
        } else {
            Instant date = request.headers().first("Accept-Datetime")
                    .map(s -> Instant.from(RFC_1123_UTC.parse(s)))
                    .orElse(Instant.EPOCH);
            replay(socket, target, date, true);
        }
    }

    private void replay(Socket socket, String target, Instant date, boolean proxy) throws IOException {
        URI uri = URI.create(target);
        NavigableSet<Capture> versions = index.query(uri);
        if (versions.isEmpty()) {
            error(socket, 404, "Not found in archive");
            return;
        }
        Capture capture = closest(versions, uri, date);
        try (FileChannel channel = FileChannel.open(capture.file(), READ)) {
            channel.position(capture.position());
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
            b.setHeader("Memento-Datetime", RFC_1123_UTC.format(record.date()));
            if (!proxy) b.setHeader("Link", mementoLinks(versions, capture));
            if (proxy) b.setHeader("Vary", "Accept-Datetime");
            MessageBody body = http.body();
            if (!proxy && HTML.equals(http.contentType().base())) {
                body = LengthedBody.create(body, ByteBuffer.wrap(script), script.length + body.size());
            }
            b.body(http.contentType(), body, body.size());
            send(socket, b.build());
        }
    }

    private String mementoLinks(NavigableSet<Capture> versions, Capture current) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(current.uri()).append(">;rel=\"original\",");
        sb.append("</timemap/").append(current.uri()).append(">;rel=\"timemap\";type=\"").append(LINK_FORMAT).append('"');
        mementoLink(sb, "first ", current, versions.first());
        mementoLink(sb, "prev ", current, versions.lower(current));
        mementoLink(sb, "next ", current, versions.higher(current));
        mementoLink(sb, "last ", current, versions.last());
        return sb.toString();
    }

    private void mementoLink(StringBuilder sb, String rel, Capture current, Capture capture) {
        if (capture == null || capture.date().equals(current.date())) return;
        if (sb.length() != 0) sb.append(',');
        sb.append("</replay/").append(ARC_DATE.format(capture.date())).append("/").append(capture.uri()).append(">;rel=\"")
                .append(rel).append("memento\";datetime=\"").append(RFC_1123_UTC.format(capture.date())).append("\"");
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

    private Capture closest(NavigableSet<Capture> versions, URI uri, Instant date) {
        Capture key = new Capture(uri, date);
        Capture a = versions.floor(key);
        Capture b = versions.higher(key);
        if (a == null) return b;
        if (b == null) return a;
        Duration da = Duration.between(a.date(), date);
        Duration db = Duration.between(b.date(), date);
        return da.compareTo(db) < 0 ? a : b;
    }
}
