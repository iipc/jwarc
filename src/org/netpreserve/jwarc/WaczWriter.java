/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writer for Web Archive Collection Zipped (WACZ) files.
 *
 * @see <a href="https://specs.webrecorder.net/wacz/latest/">WACZ Specification</a>
 */
public class WaczWriter implements Closeable {
    private final ZipOutputStream zip;
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private final ArrayList<Map<String, Object>> resources = new ArrayList<>();
    private final MessageDigest messageDigest;
    private boolean finished;

    public WaczWriter(Path path) throws IOException {
        this(Files.newOutputStream(path));
    }

    public WaczWriter(OutputStream out) {
        this.zip = new ZipOutputStream(out);
        set("profile", "data-package");
        set("wacz_version", "1.1.1");
        try  {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Writes the content of the provided source file to a WACZ resource at the specified path.
     * The source file is read twice as the zip CRC needs to be calculated before writing the
     * ZIP entry.
     *
     * @param path   the path of the resource where the content will be written
     * @param source the {@code Path} of the file whose content is to be copied to the resource
     * @throws IOException if an I/O error occurs while writing the resource or reading the source file
     * @throws IllegalArgumentException if a resource with the same path has already been written
     * @throws IllegalStateException if the WACZ file has already been finished
     */
    public void writeResource(String path, Path source) throws IOException {
        try (FileChannel channel = FileChannel.open(source)) {
            writeResource(path, channel);
        }
    }

    private void writeResource(String path, SeekableByteChannel source) throws IOException {
        if (finished) throw new IllegalStateException("WACZ file is already finished");

        ZipEntry entry = new ZipEntry(path);

        // https://specs.webrecorder.net/wacz/1.1.1/#zip-compression
        // All archive/ files should be stored in ZIP with 'STORE' mode.
        // All index/*.cdx.gz files should be stored in ZIP with 'STORE' mode.
        if (path.startsWith("archive/") || (path.startsWith("indexes/") && path.endsWith(".cdx.gz"))) {
            entry.setMethod(ZipEntry.STORED);

            // method STORED requires size and CRC to be set before writing the entry
            long start = source.position();
            long crc32 = calculateCrc32(Channels.newInputStream(source));
            long size = source.position() - start;
            source.position(start);

            entry.setCrc(crc32);
            entry.setCompressedSize(size);
            entry.setSize(size);
        }

        zip.putNextEntry(entry);
        messageDigest.reset();
        byte[] buffer = new byte[8192];
        long size = 0;
        while (true) {
            int n = source.read(ByteBuffer.wrap(buffer));
            if (n < 0) break;
            zip.write(buffer, 0, n);
            messageDigest.update(buffer, 0, n);
            size += n;
        }
        zip.closeEntry();

        Map<String, Object> resource = new LinkedHashMap<>();
        int slash = path.lastIndexOf('/');
        resource.put("name", slash >= 0 ? path.substring(slash + 1) : path);
        resource.put("path", path);
        resource.put("hash", "sha256:" + WarcDigest.hexEncode(messageDigest.digest()));
        resource.put("bytes", size);
        resources.add(resource);
    }

    private static long calculateCrc32(InputStream stream) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        while (true) {
            int n = stream.read(buffer);
            if (n < 0) break;
            crc.update(buffer, 0, n);
        }
        return crc.getValue();
    }

    private void writeDatapackageJson() throws IOException {
        // https://specs.webrecorder.net/wacz/1.1.1/#datapackage-json
        zip.putNextEntry(new ZipEntry("datapackage.json"));
        metadata.put("resources", resources);
        messageDigest.reset();
        Json.write(new DigestOutputStream(zip, messageDigest), metadata);
        zip.closeEntry();

        // https://specs.webrecorder.net/wacz/1.1.1/#datapackage-digest-json
        Map<String, Object> digestMetadata = new LinkedHashMap<>();
        digestMetadata.put("path", "datapackage.json");
        digestMetadata.put("hash", "sha256:" + WarcDigest.hexEncode(messageDigest.digest()));
        zip.putNextEntry(new ZipEntry("datapackage-digest.json"));
        Json.write(zip, digestMetadata);
        zip.closeEntry();
    }

    /**
     * Finishes writing the WACZ file without closing the underlying output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void finish() throws IOException {
        if (finished) return;
        writeDatapackageJson();
        zip.finish();
        metadata.clear();
        resources.clear();
        resources.trimToSize();
        finished = true;
    }

    /**
     * Finishes writing the WACZ file and closes the underlying output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        try {
            finish();
        } finally {
            zip.close();
        }
    }

    /**
     * Sets a metadata property to be included in datapackage.json.
     *
     * @param name  the name of the property to be set
     * @param value the value of the property to be set (must be JSON serializable)
     */
    public void set(String name, Object value) {
        metadata.put(name, value);
    }
}
