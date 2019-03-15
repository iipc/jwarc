package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WarcWriter;
import org.netpreserve.jwarc.net.Browser;
import org.netpreserve.jwarc.net.WarcRecorder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

public class RecordTool {
    public static void main(String[] args) throws Exception {
        try (ServerSocket socket = new ServerSocket(0, -1, InetAddress.getLoopbackAddress())) {
            WarcRecorder recorder = new WarcRecorder(socket, new WarcWriter(System.out));
            new Thread(recorder::listen).start();
            InetSocketAddress proxy = (InetSocketAddress) socket.getLocalSocketAddress();
            System.err.println("WarcRecorder listening on " + proxy);
            String executable = System.getenv().getOrDefault("BROWSER", "google-chrome");
            Browser browser = Browser.chrome(executable, proxy);
            for (String arg : args) {
                browser.browse(URI.create(arg));
            }
        }
        System.exit(0);
    }
}
