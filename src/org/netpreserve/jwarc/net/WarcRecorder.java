package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.*;

/**
 * HTTP proxy which records requests and responses as WARC records.
 */
public class WarcRecorder extends HttpServer {
    private final WarcWriter warcWriter;

    public WarcRecorder(ServerSocket serverSocket, WarcWriter warcWriter) {
        super(serverSocket);
        this.warcWriter = warcWriter;
        try {
            on("GET", "/", WarcServer.resource("recorder.html"));
            on("GET", "/__jwarc__/recorder-sw.js", WarcServer.resource("recorder-sw.js"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    void handle(Socket socket, String target, HttpRequest httpRequest) throws Exception {
        boolean rewriteHeaders = false;
        if (target.startsWith("/__jwarc__/record/")) {
            target = target.substring("/__jwarc__/record/".length());
            rewriteHeaders = true;
        } else if (target.startsWith("/")) {
            super.handle(socket, target, httpRequest);
            return;
        }
        URI uri = new URI(target);
        if (uri.getPath().isEmpty()) {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/", uri.getQuery(), uri.getFragment());
        }
        String path = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        HttpRequest.Builder rb = new HttpRequest.Builder(httpRequest.method(), path).version(MessageVersion.HTTP_1_0);
        for (Map.Entry<String, List<String>> e : httpRequest.headers().map().entrySet()) {
            if (e.getKey().equalsIgnoreCase("TE")) continue;
            if (e.getKey().equalsIgnoreCase("Accept-Encoding")) continue;
            if (e.getKey().equalsIgnoreCase("Connection")) continue;
            for (String v : e.getValue()) {
                rb.addHeader(e.getKey(), v);
            }
        }
        rb.setHeader("Host", uri.getPort() != -1 ? uri.getHost() + ":" + uri.getPort() : uri.getHost());
        OutputStream outputStream = socket.getOutputStream();
        if (rewriteHeaders) outputStream = new HeaderRewriter(outputStream);
        warcWriter.fetch(uri, rb.build(), outputStream);
        socket.close();
    }

    private static class HeaderRewriter extends FilterOutputStream {
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private static final byte[] SENTINEL = "\r\n\r\n".getBytes(US_ASCII);
        private int state = 0;

        public HeaderRewriter(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (state == SENTINEL.length) {
                out.write(b, off, len);
                return;
            }
            for (int i = off; i < off + len; i++) {
                if (b[i] == SENTINEL[state]) {
                    state++;
                    if (state == SENTINEL.length) {
                        buffer.write(b, off, i - off + 1);
                        HttpResponse response = HttpResponse.parseWithoutBody(Channels.newChannel(new ByteArrayInputStream(buffer.toByteArray())), null);
                        HttpResponse.Builder builder = new HttpResponse.Builder(response.status(), response.reason())
                                .version(response.version())
                                .addHeaders(response.headers().map())
                                .setHeader("Content-Length", null)
                                .setHeader("Connection", "close")
                                .setHeader("X-Frame-Options", null)
                                .setHeader("Content-Security-Policy-Report-Only", null);
                        response.headers().first("Location").ifPresent(location ->
                                builder.setHeader("Location", "/__jwarc__/record/" + location));
                        out.write(builder.build().serializeHeader());
                        out.write(b, i + 1, len - (i - off + 1));
                        buffer = null;
                        return;
                    }
                } else {
                    state = 0;
                }
            }
            buffer.write(b, off, len);
        }
    }
}
