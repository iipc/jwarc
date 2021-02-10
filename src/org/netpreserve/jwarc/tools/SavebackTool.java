package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;
import static java.time.temporal.ChronoUnit.SECONDS;

public class SavebackTool {
    private final List<String> passthroughHeaders = Arrays.asList("Content-Type", "Content-Disposition", "Content-Range");
    private boolean warcinfo = false;
    private boolean warcitHeaders = true;

    public static void main(String[] args) throws IOException {
        WarcCompression compression = WarcCompression.NONE;
        SavebackTool saveback = new SavebackTool();
        List<String> waybackUrls = new ArrayList<>();
        for (String arg: args) {
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "--gzip":
                        compression = WarcCompression.GZIP;
                        break;
                    case "--no-warcit-headers":
                        saveback.warcitHeaders = false;
                        break;
                    case "--warcinfo":
                        saveback.warcinfo = true;
                        break;
                    default:
                        usage();
                        return;
                }
            } else {
                waybackUrls.add(arg);
            }
        }
        if (waybackUrls.isEmpty()) {
            usage();
            return;
        }
        saveback.run(waybackUrls, new WarcWriter(Channels.newChannel(System.out), compression));
    }

    private static void usage() {
        System.err.println("Usage: jwarc saveback wayback-url ...");
        System.err.println("Reconstructs WARC records from wayback or pywb replayed pages");
        System.err.println("Intended to be used with the id_ option: .../wayback/20060101010000id_/http://example.org/");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --gzip               Emit GZIP-compressed records");
        System.err.println("  --no-warcit-headers  Don't emit the non-standard WARC-Source-URI and WARC-Creation-Date headers");
        System.err.println("  --warcinfo           Emit a warcinfo record");
        System.exit(1);
    }


    private void run(List<String> waybackUrls, WarcWriter warcWriter) throws IOException {
        if (warcinfo) {
            String version = Utils.getJwarcVersion();
            Map<String, List<String>> metadata = new HashMap<>();
            metadata.put("software", Collections.singletonList((version == null ? "jwarc" : "jwarc/" + version) + " saveback"));
            warcWriter.write(new Warcinfo.Builder().fields(metadata).build());
        }
        for (String waybackUrl: waybackUrls) {
            process(waybackUrl, warcWriter);
        }
    }

    private void process(String waybackUrl, WarcWriter warcWriter) throws IOException {
        Path temp = Files.createTempFile("jwarc-saveback", ".tmp");
        Instant now = Instant.now();
        HttpURLConnection connection = (HttpURLConnection) new URL(waybackUrl).openConnection();
        try (InputStream bodyStream = connection.getInputStream();
             SeekableByteChannel channel = Files.newByteChannel(temp, DELETE_ON_CLOSE, WRITE, READ, TRUNCATE_EXISTING)) {
            IOUtils.copy(bodyStream, Channels.newOutputStream(channel));
            channel.position(0);
            long bodyLength = channel.size();

            String originalUrl = getOriginalUrl(connection.getHeaderField("Link"));
            Instant timestamp = DateTimeFormatter.RFC_1123_DATE_TIME.parse(connection.getHeaderField("memento-datetime"), Instant::from);

            List<String> sourceUris = new ArrayList<>();
            sourceUris.add(waybackUrl);
            String xArchiveSrc = connection.getHeaderField("x-archive-src");
            if (waybackUrl.startsWith("https://web.archive.org/") &&
                    !(xArchiveSrc.startsWith("https://") || xArchiveSrc.startsWith("http://"))) {
                xArchiveSrc = "https://archive.org/download/" + xArchiveSrc;
            }

            HttpResponse.Builder httpBuilder = new HttpResponse.Builder(connection.getResponseCode(), connection.getResponseMessage());
            for (int i = 1; connection.getHeaderFieldKey(i) != null; i++) {
                String key = connection.getHeaderFieldKey(i);
                String value = connection.getHeaderField(i);
                if (key.toLowerCase(Locale.ROOT).startsWith("x-archive-orig-")) {
                    String name = key.substring("x-archive-orig-".length());

                    // omit content-encoding and content-length headers as we don't have access to the encoded message
                    String lowercaseName = name.toLowerCase(Locale.ROOT);
                    if (lowercaseName.equals("content-encoding")) continue;
                    if (lowercaseName.equals("content-length")) continue;

                    httpBuilder.addHeader(name, value);
                }
            }

            // some implementations don't include x-archive-orig- for headers like content-type
            for (String name : passthroughHeaders) {
                String value = connection.getHeaderField(name);
                if (value != null && connection.getHeaderField("x-archive-orig-" + name) == null) {
                    httpBuilder.setHeader(name, value);
                }
            }

            httpBuilder.body(null, channel, bodyLength);

            WarcResponse.Builder warcBuilder = new WarcResponse.Builder(URI.create(originalUrl));

            if (warcitHeaders) {
                warcBuilder.addHeader("WARC-Creation-Date", now.truncatedTo(SECONDS).toString());
                for (String sourceUri : sourceUris) {
                    warcBuilder.addHeader("WARC-Source-URI", sourceUri);
                }
                if (xArchiveSrc != null) {
                    warcBuilder.addHeader("WARC-Source-URI", xArchiveSrc);
                }
            }

            WarcResponse warcResponse = warcBuilder
                    .date(timestamp)
                    .body(httpBuilder.build())
                    .build();
            warcWriter.write(warcResponse);
        }
    }

    private static final Pattern originalLinkPattern = Pattern.compile("<([^>]+)>;\\s*rel=\"original\".*");

    private static String getOriginalUrl(String linkHeader) {
        Matcher m = originalLinkPattern.matcher(linkHeader);
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new RuntimeException("Unable to parse original url from Link header");
        }
    }
}
