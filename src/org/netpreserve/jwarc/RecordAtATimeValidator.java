package org.netpreserve.jwarc;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The `RecordAtCompression` class validates the records to be compressed and encapsulated into
 * the gzip.
 */
public class RecordAtATimeValidator {

    private RecordAtATimeValidator() {
    }

    private static final int READ_BUFFER_SIZE = 64 * 1024;

    private static class StopReadingException extends IOException {
    }


    /**
     * Counts gzip members in the file. If stopAfter > 0, stops early once that many
     * members have been found (avoids decompressing the entire file). Pass
     * stopAfter = 0 to count all members.
     */
    private static int countGzipMembers(Path inputWarc, int stopAfter) throws IOException {
        int[] memberCount = {0};

        try (InputStream fis = Files.newInputStream(inputWarc);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gz = GzipCompressorInputStream.builder().setDecompressConcatenated(true)
                     .setOnMemberEnd(x -> {
                         memberCount[0]++;
                         if (stopAfter > 0 && memberCount[0] >= stopAfter) {
                             throw new StopReadingException();
                         }
                     }).setInputStream(bis).get()) {

            byte[] buf = new byte[READ_BUFFER_SIZE];
            while (gz.read(buf) != -1) {
                // drain the stream to trigger member callbacks
            }
        } catch (StopReadingException e) {
            // early exit — threshold reached
        }

        return memberCount[0];
    }

    public static int getWarcCompressionInformation(Path inputWarc) throws IOException {
        return countGzipMembers(inputWarc, 0);
    }

    /**
     * Fast check: returns true if the gzip file contains more than one member.
     * Stops reading as soon as the second member is detected, avoiding full
     * decompression of large files.
     */
    public static boolean isMultiMemberGzip(Path inputWarc) throws IOException {
        return countGzipMembers(inputWarc, 2) >= 2;
    }

    public static void validateRandomAccessWarcOrFail(Path inputWarc) throws IOException {
        if (!isMultiMemberGzip(inputWarc)) {
            throw new IOException("Non-chunked gzip file detected, gzip block continues\n"
                    + "    beyond single record. File must be record-compressed (multi-member gzip).");
        }
    }

}
