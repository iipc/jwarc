package org.netpreserve.jwarc.cdx;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.ParsingException;
import org.netpreserve.jwarc.URIs;
import org.netpreserve.jwarc.WarcCaptureRecord;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcRequest;
import org.netpreserve.jwarc.WarcResource;
import org.netpreserve.jwarc.WarcResponse;
import org.netpreserve.jwarc.WarcRevisit;

public class CdxProcessor {

    
    public static void process(boolean printHeader, boolean postAppend, List<Path> files,                 
            CdxFormat.Builder cdxFormatBuilder, PrintStream printStream) throws IOException {       
            CdxFormat cdxFormat = cdxFormatBuilder.build();
                
        if (printHeader) {
            printStream.println(" CDX " + cdxFormat.legend());
        }

        for (Path file: files) {
            try (WarcReader reader = new WarcReader(file)) {
                reader.onWarning(System.err::println);
                WarcRecord record = reader.next().orElse(null);

                while (record != null) {
                    try {
                        if ( (record instanceof WarcResponse || record instanceof WarcResource) && 
                             ((WarcCaptureRecord) record).payload().isPresent()
                             || (record instanceof WarcRevisit && cdxFormatBuilder.isRevisitsIncluded()) ) {                                 
                            long position = reader.position();
                            WarcCaptureRecord capture = (WarcCaptureRecord) record;
                            URI id = record.version().getProtocol().equals("ARC") ? null : record.id();

                            // advance to the next record so we can calculate the length
                            record = reader.next().orElse(null);
                            long length = reader.position() - position;

                            String urlKey = null;
                            if (postAppend) {
                                // check for a corresponding request record
                                while (urlKey == null && record instanceof WarcCaptureRecord
                                        && ((WarcCaptureRecord) record).concurrentTo().contains(id)) {
                                    if (record instanceof WarcRequest) {
                                        HttpRequest httpRequest = ((WarcRequest) record).http();
                                        String encodedRequest = CdxRequestEncoder.encode(httpRequest);
                                        if (encodedRequest != null) {
                                            String rawUrlKey = capture.target() +
                                                    (capture.target().contains("?") ? '&' : '?')
                                                    + encodedRequest;
                                            urlKey = URIs.toNormalizedSurt(rawUrlKey);
                                        }
                                    }

                                    record = reader.next().orElse(null);
                                }
                            }
                             
                            String line=cdxFormat.format(capture, file, position, length, urlKey);
                            printStream.println(line);                            

                        } else {
                            record = reader.next().orElse(null);
                        }
                    } catch (ParsingException e) {
                        System.err.println("ParsingException at record " + reader.position() + ": " + e.getMessage());
                        record = reader.next().orElse(null);
                    }
               
                    if (record instanceof WarcRevisit) {
                        ((WarcRevisit)record).http(); // ensure http header is parsed before advancing. Revisits has no payload, but we still need the HTTP status.
                   }
                
                }
            }
        }
    }   
}