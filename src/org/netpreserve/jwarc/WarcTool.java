package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

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
        serve("Serve WARC files with a basic replay server/proxy") {
            @Override
            void exec(String[] args) throws Exception {
                if (args.length == 0) {
                    System.err.println("Usage: WarcTool serve <warc-files>");
                    System.err.println("Obeys environment variable PORT.");
                    System.exit(1);
                }
                int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
                ReplayServer server = new ReplayServer(new InetSocketAddress(port));
                for (String arg : args) {
                    System.err.println("Indexing " + arg);
                    server.index(Paths.get(arg));
                }
                System.err.println("Listening on port " + port);
                server.serve();
            }
        };

        private final String help;

        Command(String help) {
            this.help = help;
        }

        abstract void exec(String[] args) throws Exception;
    }
}
