package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import static java.util.Comparator.comparing;

public class CaptureIndex {
    private final NavigableSet<Capture> entries = new TreeSet<>(comparing(Capture::uri).thenComparing(Capture::date));
    private Capture entrypoint;

    public CaptureIndex(List<Path> warcs) throws IOException {
        for (Path warc : warcs) {
            try (WarcReader reader = new WarcReader(warc)) {
                for (WarcRecord record : reader) {
                    if ((record instanceof WarcResponse || record instanceof WarcResource)) {
                        WarcCaptureRecord capture = (WarcCaptureRecord) record;
                        if (URIs.hasHttpOrHttpsScheme(capture.target())) {
                            Capture entry = new Capture(capture.target(), capture.date(), warc, reader.position());
                            add(entry);
                            if (entrypoint == null && MediaType.HTML.equals(capture.payloadType().base())) {
                                entrypoint = entry;
                            }
                        }
                    }
                }
            }
        }
    }

    void add(Capture capture) {
        entries.add(capture);
    }

    NavigableSet<Capture> query(String uri) {
        return entries.subSet(new Capture(uri, Instant.MIN), true, new Capture(uri, Instant.MAX), true);
    }

    Capture entrypoint() {
        return entrypoint;
    }
}
