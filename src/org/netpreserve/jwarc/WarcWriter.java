/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
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
        if (compression == WarcCompression.GZIP) {
            throw new UnsupportedOperationException("Writing of GZIP WARC files is not currently supported");
        }
        this.channel = channel;
        this.compression = compression;

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
        // TODO: buffer headers, compression
        position.addAndGet(channel.write(ByteBuffer.wrap(record.serializeHeader())));
        MessageBody body = record.body();
        while (body.read(buffer) >= 0) {
            buffer.flip();
            position.addAndGet(channel.write(buffer));
            buffer.compact();
        }
        position.addAndGet(channel.write(ByteBuffer.wrap(TRAILER)));
    }

    /**
     * Downloads a remote resource recording the request and response as WARC records.
     */
    public void fetch(URI uri) throws IOException {
        HttpRequest httpRequest = new HttpRequest.Builder("GET", uri.getRawPath())
                .addHeader("Host", uri.getHost())
                .addHeader("User-Agent", "jwarc")
                .addHeader("Connection", "close")
                .build();
        Path tempPath = Files.createTempFile("jwarc", ".tmp");
        try (FileChannel tempFile = FileChannel.open(tempPath, READ, WRITE, DELETE_ON_CLOSE, TRUNCATE_EXISTING)) {
            Instant date = Instant.now();
            InetAddress ip;
            try (Socket socket = connect(uri.getScheme(), uri.getHost(), uri.getPort())) {
                ip = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
                socket.getOutputStream().write(httpRequest.serializeHeader());
                IOUtils.copy(socket.getInputStream(), Channels.newOutputStream(tempFile));
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

    private static Socket connect(String scheme, String host, int port) throws IOException {
        Objects.requireNonNull(host);
        if ("http".equalsIgnoreCase(scheme)) {
            return new Socket(host, port < 0 ? 80 : port);
        } else if ("https".equalsIgnoreCase(scheme)) {
            return SSLSocketFactory.getDefault().createSocket(host, port < 0 ? 443 : port);
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
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
