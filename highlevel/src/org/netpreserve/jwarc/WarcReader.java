/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class WarcReader implements Iterable<WarcRecord>, Closeable {
    private static final int CRLFCRLF = 0x0d0a0d0a;
    private static final Map<String, WarcRecord.Constructor> defaultTypes = initDefaultTypes();
    private final HashMap<String, WarcRecord.Constructor> types;
    private final WarcParser parser = new WarcParser();
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private final WarcCompression compression;
    private WarcRecord record;
    private long position;
    private long headerLength;

    public WarcReader(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        this.types = new HashMap<>(defaultTypes);

        while (buffer.remaining() < 2) {
            buffer.compact();
            int n = channel.read(buffer);
            buffer.flip();
            if (n < 0) {
                if (!buffer.hasRemaining()) {
                    this.channel = channel;
                    this.buffer = buffer;
                    compression = WarcCompression.NONE;
                    return;
                } else {
                    throw new EOFException();
                }
            }
        }

        if (buffer.getShort(buffer.position()) == 0x1f8b) {
            this.channel = new GunzipChannel(channel, buffer);
            this.buffer = ByteBuffer.allocate(8192);
            this.buffer.flip();
            compression = WarcCompression.GZIP;
        } else {
            this.channel = channel;
            this.buffer = buffer;
            compression = WarcCompression.NONE;
        }
    }

    public WarcReader(ReadableByteChannel channel) throws IOException {
        this(channel, (ByteBuffer) ByteBuffer.allocate(8192).flip());
    }

    public WarcReader(InputStream stream) throws IOException {
        this(Channels.newChannel(stream));
    }

    private static Map<String, WarcRecord.Constructor> initDefaultTypes() {
        Map<String, WarcRecord.Constructor> types = new HashMap<>();
        types.put("default", WarcRecord::new);
        types.put("continuation", WarcContinuation::new);
        types.put("conversion", WarcConversion::new);
        types.put("metadata", WarcMetadata::new);
        types.put("request", WarcRequest::new);
        types.put("resource", WarcResource::new);
        types.put("response", WarcResponse::new);
        types.put("revisit", WarcRevisit::new);
        types.put("warcinfo", Warcinfo::new);
        return types;
    }

    /**
     * Reads the next WARC record.
     * <p>
     * This method will construct an appropriate subclass of <code>WarcRecord</code> based on the value of the
     * <code>WARC-Type</code> header. New types may be registered using
     * {@link #registerType(String, WarcRecord.Constructor)}.
     * <p>
     * The body channel of any previously read record will be closed.
     *
     * @return a instance of <code>WarcRecord</code> or an empty <code>Optional</code> at the end of the channel.
     * @throws IOException      if an I/O error occurs.
     * @throws ParsingException if the WARC record is invalid.
     */
    public Optional<WarcRecord> next() throws IOException {
        if (record != null) {
            record.body().consume();
            record.body().close();
            consumeTrailer();

            if (channel instanceof GunzipChannel) {
                position = ((GunzipChannel) channel).inputPosition();
            } else {
                position += headerLength + record.body().size() + 4;
            }
        }

        parser.reset();
        if (!parser.parse(channel, buffer)) {
            return Optional.empty();
        }
        headerLength = parser.position();
        Headers headers = parser.headers();
        WarcBodyChannel body = new WarcBodyChannel(headers, channel, buffer);
        record = construct(parser.version(), headers, body);
        return Optional.of(record);
    }

    private WarcRecord construct(ProtocolVersion version, Headers headers, WarcBodyChannel body) {
        String type = headers.sole("WARC-Type").orElse("default");
        WarcRecord.Constructor constructor = types.get(type);
        if (constructor == null) {
            constructor = types.get("default");
        }
        return constructor.construct(version, headers, body);
    }

    private void consumeTrailer() throws IOException {
        while (buffer.remaining() < 4) {
            buffer.compact();
            if (channel.read(buffer) < 0) {
                throw new EOFException("expected trailing CRLFCRLF");
            }
            buffer.flip();
        }
        int trailer = buffer.getInt();
        if (trailer != CRLFCRLF) { // CRLFCRLF
            throw new ParsingException("invalid trailer: " + Integer.toHexString(trailer));
        }
    }

    /**
     * Registers a new extension record type.
     * <p>
     * Builtin types like "resource" and "response" may be overridden with a subclass that adds extension methods.
     * The special type name "default" is used when a unregistered record type is encountered.
     *
     * @param type        a value of the WARC-Type header
     * @param constructor a constructor for a corresponding subclass of WarcRecord
     */
    public void registerType(String type, WarcRecord.Constructor<WarcRecord> constructor) {
        types.put(type, constructor);
    }

    /**
     * Returns the byte position of the most recently read record.
     * <p>
     * For compressed WARCs this method will only return a meaningful value if the compression was applied in such a way
     * that the start of a new record corresponds to the start of a compression block.
     */
    public long position() {
        return position;
    }

    /**
     * The type of WARC compression that was detected.
     */
    public WarcCompression compression() {
        return compression;
    }

    @Override
    public Iterator<WarcRecord> iterator() {
        return new Iterator<WarcRecord>() {
            WarcRecord next;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    try {
                        next = WarcReader.this.next().orElse(null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return next != null;
            }

            @Override
            public WarcRecord next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                WarcRecord temp = next;
                next = null;
                return temp;
            }
        };
    }

    /**
     * Closes the underlying channel.
     */
    @Override
    public void close() throws IOException {
        channel.close();
    }
}
