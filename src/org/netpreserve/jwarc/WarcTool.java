package org.netpreserve.jwarc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;

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
        fetch("Download a URL recording the request and response") {
            void exec(String[] args) throws IOException, URISyntaxException {
                try (WarcWriter writer = new WarcWriter(System.out)) {
                    for (String arg : args) {
                        writer.fetch(new URI(arg));
                    }
                }
            }
        };

        private final String help;

        Command(String help) {
            this.help = help;
        }

        abstract void exec(String[] args) throws Exception;
    }
}
