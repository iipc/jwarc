package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScreenshotTool {
    public static void main(String[] args) throws Exception {
        List<Path> warcs = Stream.of(args).map(Paths::get).collect(Collectors.toList());
        try (WarcWriter warcWriter = new WarcWriter(System.out);
             ServerSocket serverSocket = new ServerSocket(0, -1, InetAddress.getLoopbackAddress())) {
            WarcServer warcServer = new WarcServer(serverSocket, warcs);
            InetSocketAddress address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
            System.err.println("Replay proxy listening on " + address);
            new Thread(() -> {
                try {
                    warcServer.listen();
                } catch (IOException e) {
                    // just shutdown
                }
            }).start();
            for (String arg : args) {
                try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                    for (WarcRecord record : reader) {
                        if (!isNormalPage(record)) continue;
                        WarcCaptureRecord capture = (WarcCaptureRecord) record;
                        screenshot(address, capture, warcWriter);
                    }
                }
            }
        }
    }

    private static void screenshot(InetSocketAddress proxy, WarcCaptureRecord capture, WarcWriter warcWriter) throws IOException, InterruptedException {
        Path screenshot = Files.createTempFile("jwarc-screenshot", ".png");
        try {
            String url = capture.targetURI().toString();
            Browser.run(proxy, "--screenshot=" + screenshot, url);
            try (FileChannel channel = FileChannel.open(screenshot)) {
                long size = channel.size();
                if (size == 0) return;
                warcWriter.write(new WarcResource.Builder(URI.create("screenshot:" + url))
                        .date(capture.date())
                        .body(MediaType.parse("image/png"), channel, size)
                        .build());
            }
        } finally {
            Files.deleteIfExists(screenshot);
        }
    }

    private static boolean isNormalPage(WarcRecord record) throws IOException {
        if (!(record instanceof WarcResponse) && !(record instanceof WarcResource)) {
            return false;
        }
        WarcCaptureRecord capture = (WarcCaptureRecord) record;
        String scheme = capture.targetURI().getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }
        try {
            if (!(capture.payload().isPresent() && capture.payload().get().type().base().equals(MediaType.HTML))) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return !(capture instanceof WarcResponse) || ((WarcResponse) capture).http().status() == 200;
    }
}
