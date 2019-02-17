package org.netpreserve.jwarc;

import java.net.ServerSocket;
import java.net.Socket;

class WarcRecorder {
    private final HttpServer httpServer;
    private final WarcWriter warcWriter;

    WarcRecorder(ServerSocket serverSocket, WarcWriter warcWriter) {
        this.httpServer = new HttpServer(serverSocket, this::handle);
        this.warcWriter = warcWriter;
    }

    private void handle(Socket socket, String target, HttpRequest request) {
        // TODO
    }
}
