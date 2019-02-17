package org.netpreserve.jwarc;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;

public class WarcTool {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        Command cmd = Command.valueOf(args[0]);
        cmd.exec(Arrays.copyOfRange(args, 1, args.length));
    }

    private static void usage() {
        System.err.println("usage: jwarc command [args]\n\nCommands:\n");
        for (Command cmd : Command.values()) {
            System.err.format("    %-10s %s\n", cmd.name(), cmd.help);
        }
    }

    private enum Command {
        cdx("List records in CDX format") {
            void exec(String[] args) throws Exception {
                DateTimeFormatter arcDate = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);
                for (String arg : args) {
                    try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                        WarcRecord record = reader.next().orElse(null);
                        while (record != null) {
                            if ((record instanceof WarcResponse || record instanceof WarcResource) &&
                                    ((WarcCaptureRecord) record).payload().isPresent()) {
                                WarcPayload payload = ((WarcCaptureRecord) record).payload().get();
                                MediaType type;
                                try {
                                    type = payload.type().base();
                                } catch (IllegalArgumentException e) {
                                    type = MediaType.OCTET_STREAM;
                                }
                                URI uri = ((WarcCaptureRecord) record).targetURI();
                                String date = arcDate.format(record.date());
                                int status = record instanceof WarcResponse ? ((WarcResponse) record).http().status() : 200;
                                String digest = payload.digest().map(WarcDigest::toBase32).orElse("-");
                                long position = reader.position();

                                // advance to the next record so we can calculate the length
                                record = reader.next().orElse(null);
                                long length = reader.position() - position;

                                System.out.printf("%s %s %s %s %d %s - - %d %d %s%n", uri, date, uri, type, status, digest, length, position, arg);
                            } else {
                                record = reader.next().orElse(null);
                            }
                        }
                    }
                }
            }
        },
        fetch("Download a URL recording the request and response") {
            void exec(String[] args) throws IOException, URISyntaxException {
                try (WarcWriter writer = new WarcWriter(System.out)) {
                    for (String arg : args) {
                        writer.fetch(new URI(arg));
                    }
                }
            }
        },
        ls("List records in WARC file(s)") {
            void exec(String[] args) throws IOException {
                for (String arg : args) {
                    try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                        for (WarcRecord record : reader) {
                            System.out.println(record);
                        }
                    }
                }
            }
        },
        screenshot("Take a screenshot of each page in the given WARCs") {
            void exec(String[] args) throws Exception {
                ExecutorService serverThread = Executors.newSingleThreadExecutor();
                List<Path> warcs = Stream.of(args).map(Paths::get).collect(Collectors.toList());
                try (WarcWriter warcWriter = new WarcWriter(System.out);
                     ServerSocket serverSocket = new ServerSocket(0, -1, InetAddress.getLoopbackAddress())) {
                    ReplayServer replayServer = new ReplayServer(serverSocket, warcs);
                    InetSocketAddress address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
                    System.err.println("Replay proxy listening on " + address);
                    serverThread.execute(() -> {
                        try {
                            replayServer.listen();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
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

            private void screenshot(InetSocketAddress proxy, WarcCaptureRecord capture, WarcWriter warcWriter) throws IOException, InterruptedException {
                Path screenshot = Files.createTempFile("jwarc-screenshot", ".png");
                try {
                    String url = capture.targetURI().toString();
                    String[] cmd = {System.getenv().getOrDefault("BROWSER", "google-chrome"),
                            "--headless", "--disable-gpu", "--disable-breakpad",
                            "--ignore-certificate-errors",
                            "--proxy-server=" + proxy.getHostString() + ":" + proxy.getPort(),
                            "--hide-scrollbars", "--screenshot=" + screenshot,
                            url};
                    System.err.println(String.join(" ", cmd));
                    Process p = new ProcessBuilder(cmd)
                            .inheritIO()
                            .redirectOutput(new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"))
                            .start();
                    p.waitFor();
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

            private boolean isNormalPage(WarcRecord record) throws IOException {
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
        },
        serve("Serve WARC files with a basic replay server/proxy") {
            @Override
            void exec(String[] args) throws Exception {
                if (args.length == 0) {
                    System.err.println("Usage: WarcTool serve <warc-files>");
                    System.err.println("Obeys environment variable PORT.");
                    System.exit(1);
                }
                List<Path> warcs = Stream.of(args).map(Paths::get).collect(Collectors.toList());
                int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
                ReplayServer server = new ReplayServer(new ServerSocket(port), warcs);
                System.err.println("Listening on port " + port);
                server.listen();
            }
        };

        private final String help;

        Command(String help) {
            this.help = help;
        }

        abstract void exec(String[] args) throws Exception;
    }
}
