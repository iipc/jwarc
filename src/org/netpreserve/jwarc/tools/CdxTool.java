package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public class CdxTool {
    public static void main(String[] args) throws IOException {
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
                        String digest = payload.digest().map(WarcDigest::base32).orElse("-");
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
}
