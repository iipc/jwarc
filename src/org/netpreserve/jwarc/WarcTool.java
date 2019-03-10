package org.netpreserve.jwarc;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        filter("Copy records that match a given filter expression") {
            void exec(String[] args) throws Exception {
                try {
                    String[] files;
                    if (args.length == 0) {
                        System.err.println("Usage: jwarc filter <expression> [warc-file]...");
                        System.err.println("  e.g. jwarc filter 'warc-type == \"response\" && http:content-type =~ \"image/.*\" && :status == 200' example.warc");
                        System.exit(1);
                        return;
                    } else if (args.length > 1) {
                        files = Arrays.copyOfRange(args, 1, args.length);
                    } else {
                        if (System.console() != null) {
                            System.err.println("Warning: No input files specified, reading from STDIN");
                        }
                        files = new String[]{"-"};
                    }
                    WarcFilter filter = WarcFilter.compile(args[0]);
                    try (WarcWriter writer = new WarcWriter(System.out)) {
                        for (String file : files) {
                            try (WarcReader reader = file.equals("-") ? new WarcReader(System.in) : new WarcReader(Paths.get(file))) {
                                filterRecords(filter, writer, reader);
                            }
                        }
                    }
                } catch (WarcFilterException e) {
                    System.err.println(e.prettyPrint());
                    System.exit(2);
                }
            }

            private void filterRecords(WarcFilter filter, WarcWriter writer, WarcReader reader) throws IOException {
                for (WarcRecord record : reader) {
                    if (filter.test(record)) {
                        writer.write(record);
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
        record("Fetch a page and subresources using headless Chrome") {
            void exec(String[] args) throws Exception {
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
                        runBrowser(address, arg);
                    }
                }
                System.exit(0);
            }
        },
        recorder("Run a recording proxy") {
            @Override
            void exec(String[] args) throws Exception {
                int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
                new WarcRecorder(new ServerSocket(port), new WarcWriter(System.out)).listen();
            }
        },
        screenshot("Take a screenshot of each page in the given WARCs") {
            void exec(String[] args) throws Exception {
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

            private void screenshot(InetSocketAddress proxy, WarcCaptureRecord capture, WarcWriter warcWriter) throws IOException, InterruptedException {
                Path screenshot = Files.createTempFile("jwarc-screenshot", ".png");
                try {
                    String url = capture.targetURI().toString();
                    runBrowser(proxy, "--screenshot=" + screenshot, url);
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
                WarcServer server = new WarcServer(new ServerSocket(port), warcs);
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

    private static void runBrowser(InetSocketAddress proxy, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(System.getenv().getOrDefault("BROWSER", "google-chrome"),
                "--headless", "--disable-gpu", "--disable-breakpad",
                "--ignore-certificate-errors",
                "--proxy-server=" + proxy.getHostString() + ":" + proxy.getPort(),
                "--hide-scrollbars"));
        cmd.addAll(Arrays.asList(args));
        System.err.println(String.join(" ", cmd));
        Process p = new ProcessBuilder(cmd)
                .inheritIO()
                .redirectOutput(new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"))
                .start();
        p.waitFor();
    }
}
