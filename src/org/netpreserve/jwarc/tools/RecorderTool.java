package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.net.WarcRecorder;
import org.netpreserve.jwarc.WarcWriter;

import java.net.ServerSocket;

public class RecorderTool {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        new WarcRecorder(new ServerSocket(port), new WarcWriter(System.out)).listen();
    }
}
