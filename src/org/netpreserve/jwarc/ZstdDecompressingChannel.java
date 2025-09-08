/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdBufferDecompressingStream;
import com.github.luben.zstd.ZstdDictDecompress;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * A byte channel that decompresses a zstd file. Checks for a skippable frame at the start of the file
 * containing a dictionary as per <a href="https://iipc.github.io/warc-specifications/specifications/warc-zstd/">warc-zstd</a>.
 */
class ZstdDecompressingChannel implements ReadableByteChannel, DecompressingChannel {
    private static final int ZSTD_MAGIC = 0xFD2FB528;
    private static final int DICT_MAGIC = 0x184D2A5D;
    private static final int DICT_ID_FLAG_MASK = 3;
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private ZstdBufferDecompressingStream zstdStream;
    private final ZstdDictDecompress dictionary;
    private long channelBytesRead;

    public ZstdDecompressingChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        this.channel = channel;
        this.buffer = buffer;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        dictionary = readDictionaryIfPresent(channel, buffer);
    }

    /**
     * Reads a dictionary frame if present or required by the current frame. Returns null if no dictionary frame was
     * found.
     */
    private ZstdDictDecompress readDictionaryIfPresent(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        final ZstdDictDecompress dictionary;
        channelBytesRead += IOUtils.ensureAvailable(channel, buffer, 8);
        int magic = buffer.getInt(buffer.position());
        if (magic == DICT_MAGIC) {
            buffer.position(buffer.position() + 4);
            int frameSize = buffer.getInt();
            dictionary = readDictionary(channel, buffer, frameSize);
        } else if (magic == ZSTD_MAGIC) {
            boolean requiresDictionary = (buffer.get(buffer.position() + 4) & DICT_ID_FLAG_MASK) != 0;
            if (requiresDictionary) {
                if (channel instanceof SeekableByteChannel) {
                    dictionary = readDictionaryFromStartOfChannel((SeekableByteChannel) channel);
                } else {
                    throw new IOException("dictionary required but channel is not seekable");
                }
            } else {
                dictionary = null;
            }
        } else {
            throw new IOException(String.format("unexpected zstd magic number 0x%X (expected 0x%X or 0x%X)", magic,
                    ZSTD_MAGIC, DICT_MAGIC));
        }
        return dictionary;
    }

    /**
     * Seeks to the start of the channel and reads a dictionary frame. Returns null if no dictionary frame was found.
     * Restores the channel's original position.
     */
    private ZstdDictDecompress readDictionaryFromStartOfChannel(SeekableByteChannel channel) throws IOException {
        final ZstdDictDecompress dictionary;
        long savedPosition = channel.position();
        try {
            channel.position(0);
            ByteBuffer frameBuffer = ByteBuffer.allocate(8);
            frameBuffer.order(ByteOrder.LITTLE_ENDIAN);
            frameBuffer.flip();
            IOUtils.ensureAvailable(channel, frameBuffer, 8);
            if (frameBuffer.getInt() == DICT_MAGIC) {
                int frameSize = frameBuffer.getInt();
                if (frameSize < 0) throw new IOException("dictionary frame too large");
                dictionary = readDictionary(channel, frameBuffer, frameSize);
            } else {
                dictionary = null;
            }
        } finally {
            channel.position(savedPosition);
        }
        return dictionary;
    }

    private ZstdDictDecompress readDictionary(ReadableByteChannel channel, ByteBuffer buffer, int length) throws IOException {
        ByteBuffer dictBuffer = ByteBuffer.allocateDirect(length);
        dictBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.transfer(buffer, dictBuffer);
        while (dictBuffer.hasRemaining()) {
            int bytesRead = channel.read(dictBuffer);
            if (bytesRead == -1) throw new EOFException("EOF reached before end of dictionary");
            if (channel == this.channel) channelBytesRead += bytesRead;
        }
        dictBuffer.flip();

        if (dictBuffer.getInt(0) == ZSTD_MAGIC) { // dictionary is compressed
            dictBuffer = decompressSingleFrame(dictBuffer);
        }

        return new ZstdDictDecompress(dictBuffer, true);
    }

    private static ByteBuffer decompressSingleFrame(ByteBuffer dictBuffer) throws IOException {
        long size = Zstd.getFrameContentSize(dictBuffer);
        if (size < 0) throw new IOException("error reading frame size: " + Zstd.getErrorName(size));
        if (size == 0) throw new IOException("frame content size unknown");
        if (size > Integer.MAX_VALUE) throw new IOException("uncompressed frame is too large");
        return Zstd.decompress(dictBuffer, (int) size);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!buffer.hasRemaining()) {
            buffer.compact();
            int n = channel.read(buffer);
            buffer.flip();
            if (n > 0) channelBytesRead += n;
        }
        if (zstdStream == null) {
            zstdStream = new ZstdBufferDecompressingStream(buffer);
            if (dictionary != null) zstdStream.setDict(dictionary);
        }
        return zstdStream.read(dst);
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (zstdStream != null) zstdStream.close();
        if (dictionary != null) dictionary.close();
        channel.close();
    }

    @Override
    public long inputPosition() {
        return channelBytesRead + buffer.position();
    }

    @Override
    public void reset() throws IOException {
        if (zstdStream != null) zstdStream.close();
        zstdStream = null;
        channelBytesRead = 0;
        buffer.clear();
        buffer.flip();
    }
}
