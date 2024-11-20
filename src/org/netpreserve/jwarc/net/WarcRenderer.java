package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcResource;
import org.netpreserve.jwarc.WarcWriter;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public class WarcRenderer implements Closeable  {
    private static final DateTimeFormatter ARC_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);

    private final ServerSocket proxySocket;
    private final WarcServer server;
    private final String browserExecutable;

    public WarcRenderer(CaptureIndex index) throws IOException {
        this(index, System.getenv().getOrDefault("BROWSER", "google-chrome"));
    }

    public WarcRenderer(CaptureIndex index, String browserExecutable) throws IOException {
        this.proxySocket = new ServerSocket(0, -1, InetAddress.getLoopbackAddress());
        this.server = new WarcServer(proxySocket, index);
        this.browserExecutable = browserExecutable;
        new Thread(server::listen).start();
    }

    public void screenshot(URI uri, Instant date, WarcWriter warcWriter) throws IOException {
        screenshot(uri.toString(), date, warcWriter);
    }

    public void screenshot(String url, Instant date, WarcWriter warcWriter) throws IOException {
        Path screenshot = Files.createTempFile("jwarc-screenshot", ".png");
        try {
            Browser browser = new Browser(browserExecutable, (InetSocketAddress) proxySocket.getLocalSocketAddress(),
                    "WarcRenderer (arctime/" + ARC_TIME.format(date) + ")");
            browser.screenshot(url, screenshot);
            try (FileChannel channel = FileChannel.open(screenshot)) {
                long size = channel.size();
                if (size == 0) return;
                warcWriter.write(new WarcResource.Builder(URI.create("screenshot:" + url))
                        .date(date)
                        .body(MediaType.parse("image/png"), channel, size)
                        .build());
            }
        } finally {
            Files.deleteIfExists(screenshot);
        }
    }

    @Override
    public void close() throws IOException {
        proxySocket.close();
    }
}
