package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.net.WarcRecorder;
import org.netpreserve.jwarc.WarcWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class RecordTool {
    public static void main(String[] args) throws Exception {
        try (ServerSocket socket = new ServerSocket(0, -1, InetAddress.getLoopbackAddress())) {
            WarcRecorder recorder = new WarcRecorder(socket, new WarcWriter(System.out));
            new Thread(() -> {
                try {
                    recorder.listen();
                } catch (IOException e) {
                    // probably shutting down
                }
            }).start();
            InetSocketAddress address = (InetSocketAddress) socket.getLocalSocketAddress();
            System.err.println("WarcRecorder listening on " + address);
            for (String arg : args) {
                Browser.run(address, arg);
            }
        }
        System.exit(0);
    }
}
