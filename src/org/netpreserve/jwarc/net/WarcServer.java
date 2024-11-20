package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.READ;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.netpreserve.jwarc.MediaType.HTML;

/**
 * A primitive WARC replay server.
 * <p>
 * Mainly exists to exercise the API. Can be used either as a proxy or in link-rewriting mode. Link-rewriting is
 * handled client-side by https://github.com/oduwsdl/Reconstructive
 */
public class WarcServer extends HttpServer {
    private static final DateTimeFormatter ARC_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);
    private static final DateTimeFormatter RFC_1123_UTC = RFC_1123_DATE_TIME.withZone(UTC);
    private static final MediaType LINK_FORMAT = MediaType.parse("application/link-format");
    private static final Pattern USER_AGENT_DATE_REGEX = Pattern.compile(".*\\(arcdate/([0-9]{14})\\).*");

    private final CaptureIndex index;
    private String script = "<!doctype html><script src='/__jwarc__/inject.js'></script>\n";

    public WarcServer(ServerSocket serverSocket, List<Path> warcs) throws IOException {
        this(serverSocket, new CaptureIndex(warcs));
    }

    WarcServer(ServerSocket serverSocket, CaptureIndex index) throws IOException {
        super(serverSocket);
        this.index = index;
        on("GET", "/", this::home);
        on("GET", "/__jwarc__/sw\\.js", resource("sw.js"));
        on("GET", "/__jwarc__/inject\\.js", resource("inject.js"));
        on("GET", "/replay/([0-9]{14})/(.*)", this::replay);
        on("GET", "/render/([0-9]{14})/(.*)", this::render);
        on("GET", "/timemap/(.*)", this::timemap);
        on(null, ".*", this::proxy);
    }

    private void home(HttpExchange exchange) throws IOException {
        Capture entrypoint = index.entrypoint();
        if (entrypoint == null) {
            exchange.send(404, "Empty collection");
            return;
        }
        exchange.redirect("/replay/" + ARC_DATE.format(entrypoint.date()) + "/" + entrypoint.uri());
    }

    private void proxy(HttpExchange exchange) throws IOException {
        Instant date = parseAcceptDatetime(exchange);
        if (date == null) date = parseUserAgentDate(exchange);
        if (date == null) date = Instant.EPOCH;
        replay(exchange, exchange.param(0), date, true);
    }

    private Instant parseAcceptDatetime(HttpExchange exchange) {
        Optional<String> field = exchange.request().headers().first("Accept-Datetime");
        return field.map(s -> Instant.from(RFC_1123_UTC.parse(s))).orElse(null);
    }

    private Instant parseUserAgentDate(HttpExchange exchange) {
        Optional<String> field = exchange.request().headers().first("User-Agent");
        if (!field.isPresent()) return null;
        Matcher m = USER_AGENT_DATE_REGEX.matcher(field.get());
        if (!m.matches()) return null;
        return Instant.from(ARC_DATE.parse(m.group(1)));
    }

    private void timemap(HttpExchange exchange) throws IOException {
        NavigableSet<Capture> versions = index.query(exchange.param(1));
        if (versions.isEmpty()) {
            exchange.send(404, "Not found in archive");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(versions.first().uri()).append(">;rel=\"original\"");
        for (Capture entry : versions) {
            sb.append(",\n</replay/").append(ARC_DATE.format(entry.date())).append("/").append(entry.uri())
                    .append(">;rel=\"memento\",datetime=\"").append(RFC_1123_UTC.format(entry.date()) + "\"");
        }
        sb.append("\n");
        exchange.send(200, LINK_FORMAT, sb.toString());
    }

    private void replay(HttpExchange exchange) throws IOException {
        if (!exchange.request().headers().first("x-serviceworker").isPresent()) {
            exchange.send(200, script);
            return;
        }
        Instant date = Instant.from(ARC_DATE.parse(exchange.param(1)));
        replay(exchange, exchange.param(2), date, false);
    }

    private void render(HttpExchange exchange) throws IOException {
        Instant date = Instant.from(ARC_DATE.parse(exchange.param(1)));
        String uri = exchange.param(2);
        NavigableSet<Capture> versions = index.query(uri);
        if (versions.isEmpty()) {
            exchange.send(404, "Not found in archive");
            return;
        }
        Capture capture = closest(versions, uri, date);
        if (!capture.date().equals(date)) {
            exchange.redirect("/render/" + ARC_DATE.format(capture.date()) + "/" + capture.uri());
            return;
        }
        Browser browser = new Browser("chromium-browser", (InetSocketAddress) serverSocket.getLocalSocketAddress(),
                "render (arcdate/" + ARC_DATE.format(date) + ")");
        try (FileChannel channel = browser.screenshot(uri)) {
            exchange.send(new HttpResponse.Builder(200, " ")
                    .body(MediaType.parse("image/png"), channel, channel.size()).build());
        }
    }

    private void replay(HttpExchange exchange, String target, Instant date, boolean proxy) throws IOException {
        NavigableSet<Capture> versions = index.query(target);
        if (versions.isEmpty()) {
            exchange.send(404, "Not found in archive");
            return;
        }
        Capture capture = closest(versions, target, date);
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
                body = LengthedBody.create(body, ByteBuffer.wrap(script.getBytes(US_ASCII)), script.length() + body.size());
            }
            b.body(http.contentType(), body, body.size());
            exchange.send(b.build());
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

    private Capture closest(NavigableSet<Capture> versions, String uri, Instant date) {
        Capture key = new Capture(uri, date);
        Capture a = versions.floor(key);
        Capture b = versions.higher(key);
        if (a == null) return b;
        if (b == null) return a;
        Duration da = Duration.between(a.date(), date);
        Duration db = Duration.between(b.date(), date);
        return da.compareTo(db) < 0 ? a : b;
    }

    static HttpHandler resource(String name) throws IOException {
        URL url = WarcServer.class.getResource(name);
        if (url == null) throw new NoSuchFileException(name);

        MediaType type;
        if (name.endsWith(".js")) {
            type = MediaType.parse("application/javascript");
        } else if (name.endsWith(".html")) {
            type = MediaType.HTML_UTF8;
        } else {
            throw new IllegalArgumentException("Unable to determine media type for " + name);
        }

        return exchange -> {
            URLConnection conn = url.openConnection();
            long length = conn.getContentLengthLong();

            // XXX: workaround for SubstrateVM, calculate the length by actually reading it
            if (length == -1) {
                byte[] buf = new byte[8192];
                try (InputStream stream = conn.getInputStream()) {
                    length = 0;
                    while (true) {
                        int n = stream.read(buf);
                        if (n == -1) break;
                        length += n;
                    }
                }
            }

            try (InputStream stream = conn.getInputStream()) {
                exchange.send(new HttpResponse.Builder(200, "OK")
                        .body(type, Channels.newChannel(stream), length)
                        .setHeader("Service-Worker-Allowed", "/")
                        .build());
            }
        };
    }
}
