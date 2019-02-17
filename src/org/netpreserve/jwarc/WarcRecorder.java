package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * (Work in progress) Recording HTTP proxy.
 *
 * Status: some requests work, lots fail with parse errors.
 *
 * TODO: HTTP/1.1, request payloads, keep-alive
 */
class WarcRecorder {
    private final HttpServer httpServer;
    private final WarcWriter warcWriter;

    WarcRecorder(ServerSocket serverSocket, WarcWriter warcWriter) {
        this.httpServer = new HttpServer(serverSocket, this::handle);
        this.warcWriter = warcWriter;
    }

    private void handle(Socket clientSocket, String target, HttpRequest httpRequest) throws IOException, URISyntaxException {
        URI uri = new URI(target);
        String path = uri.getPath();
        if (uri.getQuery() != null) {
            path += "?" + uri.getQuery();
        }
        HttpRequest.Builder b = new HttpRequest.Builder(httpRequest.method(), path).version(MessageVersion.HTTP_1_0);
        for (Map.Entry<String, List<String>> e : httpRequest.headers().map().entrySet()) {
            if (e.getKey().equalsIgnoreCase("TE")) continue;
            if (e.getKey().equalsIgnoreCase("Accept-Encoding")) continue;
            if (e.getKey().equalsIgnoreCase("Connection")) continue;
            for (String v : e.getValue()) {
                b.addHeader(e.getKey(), v);
            }
        }
        HttpRequest backRequest = b.build();

        // FIXME: duplicates WarcWriter.fetch
        Path tempPath = Files.createTempFile("jwarc", ".tmp");
        try (FileChannel tempFile = FileChannel.open(tempPath, READ, WRITE, DELETE_ON_CLOSE, TRUNCATE_EXISTING)) {
            Instant date = Instant.now();
            InetAddress ip;
            try (Socket socket = IOUtils.connect(uri.getScheme(), uri.getHost(), uri.getPort())) {
                ip = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
                socket.getOutputStream().write(backRequest.serializeHeader());
                IOUtils.copy(socket.getInputStream(), Channels.newOutputStream(tempFile));
            }
            tempFile.position(0);
            WarcRequest request = new WarcRequest.Builder(uri)
                    .date(date)
                    .body(backRequest)
                    .ipAddress(ip)
                    .build();
            WarcResponse response = new WarcResponse.Builder(uri)
                    .date(date)
                    .body(MediaType.HTTP_RESPONSE, tempFile, tempFile.size())
                    .concurrentTo(request.id())
                    .ipAddress(ip)
                    .build();
            warcWriter.write(request);
            warcWriter.write(response);
            tempFile.position(0);
            IOUtils.copy(Channels.newInputStream(tempFile), clientSocket.getOutputStream());
            clientSocket.close();
        }
    }

    void listen() throws IOException {
        httpServer.listen();
    }
}
