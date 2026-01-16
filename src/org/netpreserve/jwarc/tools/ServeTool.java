package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.net.WarcServer;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServeTool {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || Utils.hasHelpFlag(args)) {
            System.err.println("Usage: WarcTool serve <warc-files>");
            System.err.println("Obeys environment variable PORT.");
            System.exit(1);
        }
        List<Path> warcs = Stream.of(args).map(Paths::get).collect(Collectors.toList());
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        WarcServer server = new WarcServer(new ServerSocket(port), warcs);
        System.err.println("Listening on port " + port);
        server.listen();
    }
}
