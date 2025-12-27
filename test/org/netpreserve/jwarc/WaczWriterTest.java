package org.netpreserve.jwarc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class WaczWriterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {
        Path waczFile = temporaryFolder.newFile("test.wacz").toPath();
        Path warcFile = temporaryFolder.newFile("test.warc.gz").toPath();

        try (WarcWriter warcWriter = new WarcWriter(warcFile)) {
            warcWriter.write(new WarcResponse.Builder("http://example.org/")
                    .date(java.time.Instant.now())
                    .body(new HttpResponse.Builder(200, "OK")
                            .body(MediaType.HTML, "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                            .build())
                    .build());
        }
        String warcSha256 = sha256(warcFile);

        try (WaczWriter waczWriter = new WaczWriter(Files.newOutputStream(waczFile))) {
            waczWriter.writeResource("archive/test.warc.gz", warcFile);
        }

        try (ZipFile zipFile = new ZipFile(waczFile.toFile())) {
            ZipEntry warcEntry = zipFile.getEntry("archive/test.warc.gz");
            assertNotNull(warcEntry);
            assertEquals(ZipEntry.STORED, warcEntry.getMethod());

            ZipEntry cdxEntry = zipFile.getEntry("indexes/index.cdx.gz");
            assertNotNull(cdxEntry);
            try (InputStream is = zipFile.getInputStream(cdxEntry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))) {
                String line = reader.readLine();
                assertNotNull(line);
                assertTrue(line.contains("example.org"));
                assertTrue(line.startsWith("org,example)/"));
                assertTrue(line.endsWith("}")); // CDXJ format
            }

            ZipEntry datapackageEntry = zipFile.getEntry("datapackage.json");
            assertNotNull(datapackageEntry);
            Map<String, Object> metadata = (Map<String, Object>) Json.read(zipFile.getInputStream(datapackageEntry));
            assertEquals("1.1.1", metadata.get("wacz_version"));
            assertEquals("data-package", metadata.get("profile"));

            List<Map<String, Object>> resources = (List<Map<String, Object>>) metadata.get("resources");
            Map<String, Object> resource = resources.get(0);
            assertEquals("archive/test.warc.gz", resource.get("path"));
            assertEquals("test.warc.gz", resource.get("name"));
            assertEquals("sha256:" + warcSha256, resource.get("hash"));
            
            ZipEntry pagesEntry = zipFile.getEntry("pages/pages.jsonl");
            assertNotNull(pagesEntry);
            try (InputStream is = zipFile.getInputStream(pagesEntry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String header = reader.readLine();
                assertNotNull(header);
                assertTrue(header.contains("json-pages-1.0"));
                String page = reader.readLine();
                assertNotNull(page);
                assertTrue(page.contains("http://example.org/"));
                assertTrue(page.contains("\"url\":"));
                assertTrue(page.contains("\"ts\":"));
            }

            String datapackageDigest = sha256(zipFile.getInputStream(datapackageEntry));
            ZipEntry digestEntry = zipFile.getEntry("datapackage-digest.json");
            assertNotNull(digestEntry);
            Map<String, Object> digestMetadata = (Map<String, Object>) Json.read(zipFile.getInputStream(digestEntry));
            assertEquals("datapackage.json", digestMetadata.get("path"));
            assertEquals("sha256:" + datapackageDigest, digestMetadata.get("hash"));
        }
    }

    @Test
    public void testManualPages() throws Exception {
        Path waczFile = temporaryFolder.newFile("manual.wacz").toPath();
        Path pagesFile = temporaryFolder.newFile("pages.jsonl").toPath();
        Files.write(pagesFile, Arrays.asList("{\"format\": \"json-pages-1.0\", \"id\": \"pages\", \"title\": \"Manual Pages\"}",
                "{\"url\": \"http://example.org/manual\", \"ts\": \"2023-01-01T00:00:00Z\"}"), StandardCharsets.UTF_8);

        try (WaczWriter waczWriter = new WaczWriter(Files.newOutputStream(waczFile))) {
            waczWriter.setAutoPages(false);
            waczWriter.writeResource("pages/pages.jsonl", pagesFile);
        }

        try (ZipFile zipFile = new ZipFile(waczFile.toFile())) {
            ZipEntry pagesEntry = zipFile.getEntry("pages/pages.jsonl");
            assertNotNull(pagesEntry);
            try (InputStream is = zipFile.getInputStream(pagesEntry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String header = reader.readLine();
                assertTrue(header.contains("Manual Pages"));
                String page = reader.readLine();
                assertTrue(page.contains("http://example.org/manual"));
                assertNull(reader.readLine());
            }
        }
    }

    public String sha256(Path path) throws Exception {
        try (InputStream stream = Files.newInputStream(path)) {
            return sha256(stream);
        }
    }

    private static String sha256(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        while (true) {
            int n = stream.read(buffer);
            if (n < 0) break;
            digest.update(buffer, 0, n);
        }
        return WarcDigest.hexEncode(digest.digest());
    }
}