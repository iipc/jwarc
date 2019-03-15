package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.MessageVersion;
import org.netpreserve.jwarc.WarcWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * HTTP proxy which records requests and responses as WARC records.
 */
public class WarcRecorder {
    private final HttpServer httpServer;
    private final WarcWriter warcWriter;

    public WarcRecorder(ServerSocket serverSocket, WarcWriter warcWriter) {
        this.httpServer = new HttpServer(serverSocket, this::handle);
        this.warcWriter = warcWriter;
    }

    private void handle(Socket socket, String target, HttpRequest httpRequest) throws IOException, URISyntaxException {
        URI uri = new URI(target);
        String path = uri.getPath();
        if (uri.getQuery() != null) {
            path += "?" + uri.getQuery();
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
        warcWriter.fetch(uri, rb.build(), socket.getOutputStream());
        socket.close();
    }

    public void listen() throws IOException {
        httpServer.listen();
    }
}
