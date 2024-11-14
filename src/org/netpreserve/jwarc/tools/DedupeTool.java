package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;
import org.netpreserve.jwarc.cdx.CdxReader;
import org.netpreserve.jwarc.cdx.CdxRecord;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class DedupeTool {
    private long minimumSize = 256;
    private String cdxServer;
    private boolean verbose;

    public void deduplicateWarcFile(Path infile, Path outfile) throws IOException {
        try (FileChannel input = FileChannel.open(infile);
             WarcReader reader = new WarcReader(input);
             FileChannel output = FileChannel.open(outfile, WRITE, CREATE, TRUNCATE_EXISTING)) {

            // We create the WarcWriter on demand so that if no records are deduplicated we don't write an empty
            // gzip member at the end of the file.
            WarcWriter writer = null;

            WarcRecord record = reader.next().orElse(null);
            while (record != null) {
                long position = reader.position();
                WarcRevisit revisit = deduplicate(record);

                record = reader.next().orElse(null);
                long length = reader.position() - position;

                if (revisit == null) {
                    if (verbose) System.out.println("Copying " + position + ":" + length);
                    transferExactly(input, position, length, output);
                } else {
                    if (verbose) System.out.println("Writing revisit for " + position + ":" + length);
                    if (writer == null) writer = new WarcWriter(output, reader.compression());
                    writer.write(revisit);
                }
            }
        }
    }

    private static void transferExactly(FileChannel input, long position, long length, FileChannel output) throws IOException {
        long transferred = 0;
        while (transferred < length) {
            long n = input.transferTo(position + transferred, length - transferred, output);
            if (n <= 0) {
                throw new IOException("FileChannel.transferTo returned " + n);
            }
            transferred += n;
        }
        if (transferred != length) {
            throw new IOException("Expected to transfer " + length + " but actually transferred " + transferred);
        }
    }

    private WarcRevisit deduplicate(WarcRecord record) throws IOException {
        if (!(record instanceof WarcResponse)) return null;
        WarcResponse response = (WarcResponse) record;
        WarcPayload payload = response.payload().orElse(null);
        if (payload == null || payload.body().size() < minimumSize) return null;
        WarcDigest payloadDigest = response.payloadDigest().orElse(null);
        if (payloadDigest == null) return null;
        CdxRecord match = findMatchingRecord(response, payloadDigest.base32());
        if (match == null) return null;
        return new WarcRevisit.Builder(response.targetURI(), WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_0)
                .date(response.date())
                .refersTo((URI) null, match.targetURI(), match.date())
                .body(response.contentType(), response.http().serializeHeader())
                .payloadDigest(payloadDigest)
                .build();
    }

    private CdxRecord findMatchingRecord(WarcCaptureRecord capture, String digest) throws IOException {
        URL queryUrl = new URL(cdxServer + "?sort=reverse&rows=10&matchType=exact&url=" + URLEncoder.encode(capture.target(), UTF_8.name()));
        try (CdxReader response = new CdxReader(queryUrl.openStream())) {
            for (CdxRecord record : response) {
                if (digest.equalsIgnoreCase(record.digest())) {
                    return record;
                }
            }
        }
        return null;
    }

    public void setCdxServer(String cdxServer) {
        this.cdxServer = cdxServer;
    }

    private static Path determineOutputPath(Path infile) {
        String[] suffixes = new String[]{".warc.gz", ".warc", ".arc.gz", ".arc"};
        String filename = infile.getFileName().toString();
        Path dir = infile.getParent();
        if (dir == null) dir = Paths.get(".");
        for (String suffix : suffixes) {
            if (filename.endsWith(suffix)) {
                String basename = filename.substring(0, filename.length() - suffix.length());
                return dir.resolve(basename + "-dedup" + suffix);
            }
        }
        return dir.resolve(filename + ".dedup");
    }

    public static void main(String[] args) throws IOException {
        DedupeTool dedupeTool = new DedupeTool();
        List<Path> infiles = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i]) {
                    case "--cdx-server":
                        dedupeTool.setCdxServer(args[++i]);
                        break;
                    case "--minimum-size":
                        dedupeTool.setMinimumSize(Long.parseLong(args[++i]));
                        break;
                    case "-h":
                    case "--help":
                        System.out.println("Usage: jwarc dedupe [options] [warc-files...]");
                        System.out.println();
                        System.out.println("Options:");
                        System.out.println("      --cdx-server URL      De-deduplicate against a remote CDX server");
                        System.out.println("      --minimum-size BYTES  Minimum payload size to consider de-duplicating (default " + dedupeTool.minimumSize + ")");
                        System.out.println("  -v, --verbose             Verbose output");
                        return;
                    case "-v":
                    case "--verbose":
                        dedupeTool.verbose = true;
                        break;
                    default:
                        System.err.println("Unrecognized option: " + args[i]);
                        System.err.println("Try `jwarc dedupe --help` for usage information");
                        System.exit(1);
                        return;
                }
            } else {
                infiles.add(Paths.get(args[i]));
            }
        }

        for (Path infile : infiles) {
            dedupeTool.deduplicateWarcFile(infile, determineOutputPath(infile));
        }
    }

    public void setMinimumSize(long minimumSize) {
        this.minimumSize = minimumSize;
    }
}
