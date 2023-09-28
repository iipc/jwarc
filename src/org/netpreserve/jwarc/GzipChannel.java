/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * A channel that compresses the input using gzip before writing
 * 
 * GzipChannel allows to write the data as sequence of independently compressed
 * gzip "members" in order to follow the "record-at-time compression"
 * recommendation of the <a href=
 * "https://iipc.github.io/warc-specifications/specifications/warc-format/warc-1.1/#record-at-time-compression">WARC
 * specification</a>, see {@link #finish()}.
 * 
 */
class GzipChannel implements WritableByteChannel {

    static final short GZIP_MAGIC = (short) 0x8b1f;
    static final int CM_DEFLATE = Deflater.DEFLATED;

    /** Default gzip header, 10 bytes long */
    private static final byte[] GZIP_HEADER = new byte[] { //
            (byte) GZIP_MAGIC, //
            (byte) (GZIP_MAGIC >> 8), //
            Deflater.DEFLATED, //
            0, 0, 0, 0, 0, 0, 0 };

    private final ByteBuffer gzipHeader = ByteBuffer.wrap(GZIP_HEADER);
    private boolean headerWritten = false;
    private boolean finished = false;
    private boolean dataWritten = false;
    private long outputPosition;
    private final WritableByteChannel channel;
    private final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    private final ByteBuffer buffer;
    private final CRC32 crc = new CRC32();

    public GzipChannel(WritableByteChannel channel) throws IOException {
        this(channel, ByteBuffer.allocate(8192));
    }

    public GzipChannel(WritableByteChannel channel, ByteBuffer buffer) throws IOException, IllegalArgumentException {
        this.channel = channel;
        this.buffer = buffer;
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("ByteBuffer must be array-backed and writable");
        }
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void checkStatus(boolean finish) throws IOException {
        if ((finish && !dataWritten) || (!finish && !headerWritten)) {
            writeHeader();
            dataWritten = true;
            finished = false;
        }
    }

    private void writeHeader() throws IOException {
        outputPosition += channel.write(gzipHeader);
        gzipHeader.rewind();
        headerWritten = true;
    }

    /**
     * Finish the current gzip member (independently compressed and decompressable).
     * The next call of {@link #write(ByteBuffer)} will automatically start the a
     * new gzip member.
     * 
     * @return The number of bytes written when finishing the current gzip member,
     *         zero if current member is already finished
     */
    public int finish() throws IOException {
        if (finished) {
            return 0;
        }

        checkStatus(true);

        deflater.finish();

        int clen;
        int cwritten = 0;
        while ((clen = deflater.deflate(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(),
                Deflater.FULL_FLUSH)) > 0) {
            cwritten += clen;
            buffer.position(buffer.position() + clen);
            buffer.flip();
            outputPosition += channel.write(buffer);
            buffer.compact();
        }

        // write CRC and uncompressed data length
        buffer.putInt((int) crc.getValue());
        buffer.putInt((int) deflater.getBytesRead());
        buffer.flip();
        outputPosition += channel.write(buffer);
        buffer.compact();

        deflater.reset();
        crc.reset();
        finished = true;
        headerWritten = false;

        return cwritten;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (!finished) {
            // finish current gzip member if not done explicitly by calling finish()
            finish();
        }
        channel.close();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        byte[] srcBytes;
        int off = src.position();
        int len = src.remaining();
        if (len == 0) {
            // nothing to write
            return 0;
        } else if ( src.hasArray() ) {
            srcBytes = src.array();
            src.position(len);
            src.limit(len);
        } else {
            off = 0;
            srcBytes = new byte[len];
            src.get(srcBytes);
        }

        crc.update(srcBytes, off, len);
        deflater.setInput(srcBytes, off, len);

        checkStatus(false);

        int clen;
        while (!deflater.needsInput()) {
            clen = deflater.deflate(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(),
                    Deflater.NO_FLUSH);
            if (clen > 0) {
                buffer.position(buffer.position() + clen);
                buffer.flip();
                outputPosition += channel.write(buffer);
                buffer.compact();
            }
        }

        return len;
    }

    public long outputPosition() {
        return outputPosition;
    }
}
