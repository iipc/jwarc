/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

public class WarcReader {
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

    public WarcReader(ReadableByteChannel channel) throws IOException {
        this.types = new HashMap<>(defaultTypes);
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();

        while (buffer.remaining() < 2) {
            buffer.compact();
            if (channel.read(buffer) < 0) {
                if (!buffer.hasRemaining()) {
                    this.channel = channel;
                    this.buffer = buffer;
                    compression = WarcCompression.NONE;
                    return;
                } else {
                    throw new EOFException();
                }
            }
            buffer.flip();
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

    public static void main(String args[]) throws IOException, DataFormatException {
        while (true) {
            long start = System.currentTimeMillis();
            try (FileChannel channel = FileChannel.open(Paths.get("/tmp/her.warc.gz"))) {
                WarcReader reader = new WarcReader(channel);
                while (true) {
                    WarcRecord record = reader.next();
                    if (record == null) break;
//                    System.out.println(record + " " + reader.position() + " " + record.body().size());
                }
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    public WarcRecord next() throws IOException {
        if (record != null) {
            record.body().consume();
            consumeTrailer();

            if (channel instanceof GunzipChannel) {
                position = ((GunzipChannel) channel).inputPosition();
            } else {
                position += headerLength + record.body().size() + 4;
            }
        }

        parser.reset();
        if (!parser.parse(channel, buffer)) {
            return null;
        }
        headerLength = parser.position();
        Headers headers = parser.headers();
        WarcBodyChannel body = new WarcBodyChannel(headers, channel, buffer);
        record = construct(parser.version(), headers, body);
        return record;
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

    public void registerType(String type, WarcRecord.Constructor<WarcRecord> constructor) {
        types.put(type, constructor);
    }

    public long position() {
        return position;
    }

    public WarcCompression compression() {
        return compression;
    }
}
