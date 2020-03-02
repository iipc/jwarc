/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.StandardOpenOption.*;

/**
 * Writes records to a WARC file.
 * <p>
 * Compression is not yet implemented.
 */
public class WarcWriter implements Closeable {
    private static final byte[] TRAILER = new byte[]{'\r', '\n', '\r', '\n'};
    private final WritableByteChannel channel;
    private final WarcCompression compression;
    private final ByteBuffer buffer = ByteBuffer.allocate(8192);

    private AtomicLong position = new AtomicLong(0);

    public WarcWriter(WritableByteChannel channel, WarcCompression compression) throws IOException {
        this.compression = compression;
        if (compression == WarcCompression.GZIP) {
            this.channel = new GzipChannel(channel);
        } else {
            this.channel = channel;
        }

        if (channel instanceof SeekableByteChannel) {
            position.set(((SeekableByteChannel) channel).position());
        }
    }

    public WarcWriter(WritableByteChannel channel) throws IOException {
        this(channel, WarcCompression.NONE);
    }

    public WarcWriter(OutputStream stream) throws IOException {
        this(Channels.newChannel(stream));
    }

    public synchronized void write(WarcRecord record) throws IOException {
        // TODO: buffer headers
        position.addAndGet(channel.write(ByteBuffer.wrap(record.serializeHeader())));
        MessageBody body = record.body();
        while (body.read(buffer) >= 0) {
            buffer.flip();
            position.addAndGet(channel.write(buffer));
            buffer.compact();
        }
        position.addAndGet(channel.write(ByteBuffer.wrap(TRAILER)));
        if (compression == WarcCompression.GZIP) {
            position.addAndGet(((GzipChannel) channel).finish());
        }
    }

    /**
     * Downloads a remote resource recording the request and response as WARC records.
     */
    public void fetch(URI uri) throws IOException {
        HttpRequest httpRequest = new HttpRequest.Builder("GET", uri.getRawPath())
                .version(MessageVersion.HTTP_1_0) // until we support chunked encoding
                .addHeader("Host", uri.getHost())
                .addHeader("User-Agent", "jwarc")
                .addHeader("Connection", "close")
                .build();
        fetch(uri, httpRequest, null);
    }

    /**
     * Downloads a remote resource recording the request and response as WARC records.
     * <p>
     * @param uri to download
     * @param httpRequest request to send
     * @param copyTo if not null will receive a copy of the (raw) http response bytes
     * @throws IOException
     */
    public void fetch(URI uri, HttpRequest httpRequest, OutputStream copyTo) throws IOException {
        Path tempPath = Files.createTempFile("jwarc", ".tmp");
        try (FileChannel tempFile = FileChannel.open(tempPath, READ, WRITE, DELETE_ON_CLOSE, TRUNCATE_EXISTING)) {
            Instant date = Instant.now();
            InetAddress ip;
            try (Socket socket = IOUtils.connect(uri.getScheme(), uri.getHost(), uri.getPort())) {
                ip = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
                socket.getOutputStream().write(httpRequest.serializeHeader());
                InputStream inputStream = socket.getInputStream();
                byte[] buf = new byte[8192];
                while (true) {
                    int n = inputStream.read(buf);
                    if (n < 0) break;
                    tempFile.write(ByteBuffer.wrap(buf, 0, n));
                    try {
                        if (copyTo != null) copyTo.write(buf, 0, n);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            tempFile.position(0);
            WarcRequest request = new WarcRequest.Builder(uri)
                    .date(date)
                    .body(httpRequest)
                    .ipAddress(ip)
                    .build();
            WarcResponse response = new WarcResponse.Builder(uri)
                    .date(date)
                    .body(MediaType.HTTP_RESPONSE, tempFile, tempFile.size())
                    .concurrentTo(request.id())
                    .ipAddress(ip)
                    .build();
            write(request);
            write(response);
        }
    }

    /**
     * Returns the byte position the next record will be written to.
     * <p>
     * If the underlying channel is not seekable the returned value will be relative to the position the channel
     * was in when the WarcWriter was created.
     */
    public long position() {
        return position.get();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
