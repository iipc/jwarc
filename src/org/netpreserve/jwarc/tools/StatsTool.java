package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;
import org.netpreserve.jwarc.cdx.CdxReader;
import org.netpreserve.jwarc.cdx.CdxRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.util.Comparator.comparing;

public class StatsTool {
    private static class Row {
        final String key;
        long count;
        long totalSize;

        Row(String key) {
            this.key = key;
        }

        public void add(long size) {
            count++;
            totalSize += size;
        }
    }

    private static class Table {
        final String name;
        final Function<WarcRecord, String> warcKeyFunction;
        final Function<CdxRecord, String> cdxKeyFunction;
        final Map<String, Row> rows = new HashMap<>();

        private Table(String name, Function<WarcRecord, String> warcKeyFunction, Function<CdxRecord, String> cdxKeyFunction) {
            this.name = name;
            this.warcKeyFunction = warcKeyFunction;
            this.cdxKeyFunction = cdxKeyFunction;
        }

        public void add(WarcRecord record, long size) {
            String key = warcKeyFunction.apply(record);
            if (key == null) return;
            rows.computeIfAbsent(key, Row::new).add(size);
        }

        public void add(CdxRecord record) {
            String key = cdxKeyFunction.apply(record);
            if (key == null) return;
            rows.computeIfAbsent(key, Row::new).add(record.size());
        }

        public void print(Function<Long,String> sizeFormatter) {
            if (rows.isEmpty()) return;
            int maxKeyLength = Math.max(name.length(), rows.keySet().stream().mapToInt(String::length).max().orElse(10));
            System.out.printf("%-" + maxKeyLength + "s %10s %10s %10s%n", name, "COUNT", "TOTSIZE", "AVGSIZE");
            rows.values().stream().sorted(comparing(e -> -e.count)).forEachOrdered(row ->
                    System.out.printf("%-" + maxKeyLength + "s %10d %10s %10s%n",
                            row.key, row.count, sizeFormatter.apply(row.totalSize), sizeFormatter.apply(row.totalSize / row.count)));
            System.out.println();
        }
    }

    List<Table> tables = Arrays.asList(
            new Table("RECORD", WarcRecord::type, r -> "cdx"),
            new Table("MIME", record -> {
                if (record instanceof WarcResponse || record instanceof WarcResource) {
                    try {
                        return ((WarcCaptureRecord)record).payload()
                                .map(payload -> payload.type().base().toString()).orElse(null);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                return null;
            }, record -> record.contentType().toString()),
            new Table("HOST", record -> {
                if (record instanceof WarcResponse || record instanceof WarcResource) {
                    return ((WarcCaptureRecord) record).targetURI().getHost();
                } else {
                    return  null;
                }
            }, record -> record.targetURI().getHost())
    );

    public static void main(String[] args) throws IOException {
        StatsTool statsTool = new StatsTool();
        Function<Long,String> sizeFormatter = String::valueOf;
        List<Path> files = new ArrayList<>();

        for (String arg: args) {
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "-h":
                    case "--human-readable":
                        sizeFormatter = StatsTool::humanByteSize;
                        break;
                    case "--help":
                        System.out.println("Usage: jwarc stats [warc-or-cdx-files...]");
                        System.out.println();
                        System.out.println("Options:");
                        System.out.println("  -h, --human-readable  Print sizes in powers of 1024 (e.g. 13.1 MB)");
                        return;
                    default:
                        System.err.println("Unrecognized option: " + arg);
                        System.err.println("Try `jwarc stats --help` for usage information");
                        System.exit(1);
                        break;
                }
            } else {
                files.add(Paths.get(arg));
            }
        }

        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".cdx")) {
                statsTool.loadCdxFile(file);
            } else {
                statsTool.loadWarcFile(file);
            }
        }
        statsTool.print(sizeFormatter);
    }

    private void print(Function<Long,String> sizeFormatter) {
        tables.forEach(table -> table.print(sizeFormatter));
    }

    private void loadCdxFile(Path path) throws IOException {
        try (CdxReader reader = new CdxReader(Files.newInputStream(path))) {
            for (CdxRecord record: reader) {
                for (Table table : tables) {
                    table.add(record);
                }
            }
        }
    }

    private void loadWarcFile(Path path) throws IOException {
        try (WarcReader reader = new WarcReader(path)) {
            WarcRecord next = reader.next().orElse(null);
            while (next != null) {
                long position = reader.position();
                WarcRecord record = next;
                if (record instanceof WarcCaptureRecord) {
                    // ensure http headers are parsed before moving to the next record
                    ((WarcCaptureRecord) record).payload();
                }
                next = reader.next().orElse(null);
                long length = reader.position() - position;
                for (Table table : tables) {
                    table.add(record, length);
                }
            }
        }
    }

    private static String humanByteSize(long n) {
        if (n < 0) return "-" + humanByteSize(-n);
        int i = n == 0 ? 0 : (63 - Long.numberOfLeadingZeros(n)) / 10;
        return String.format("%.1f %sB", (double)n / (1L << i * 10), " KMGTPE".charAt(i));
    }
}
