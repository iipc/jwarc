package jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

public class WarcReader {
    final ReadableByteChannel channel;
    final WarcParser parser = new WarcParser();
    final ByteBuffer buffer = ByteBuffer.allocate(8192);

    /**
     * Constructs a WarcReader from a byte channel. The reader will be seekable if the channel also implements the
     * SeekableByteChannel interface.
     */
    WarcReader(ReadableByteChannel channel) {
        this.channel = channel;
        buffer.flip();
    }

    /**
     * Constructs a non-seekable WarcReader from an InputStream.
     */
    WarcReader(InputStream stream) {
        this(Channels.newChannel(stream));
    }

    /**
     * Constructs a seekable WarcReader from a RandomAccessFile.
     */
    WarcReader(RandomAccessFile raf) {
        this(raf.getChannel());
    }

    /**
     *
     * @return the next record or null when the end of stream is reached
     * @throws IOException
     */
    public synchronized WarcHeader nextRecord() throws IOException {
        do {
            if (!buffer.hasRemaining()) {
                buffer.clear();
                int n = channel.read(buffer);
                buffer.flip();
                if (n == -1) {
                    return null; // EOF
                }
            }
            parser.feed(buffer);
        } while (!parser.isFinished());

        WarcHeader record = new WarcHeader(parser.versionMajor, parser.versionMinor, parser.fields);
        return record;
    }

    private SeekableByteChannel seekable() {
        try {
            return (SeekableByteChannel) channel;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("channel is not seekable", e);
        }
    }

    /**
     * Returns the position of the reader in the underlying channel.
     *
     * @throws IOException when an I/O error occurs
     *         UnsupportedOperationException when the channel is not seekable
     */
    public long position() throws IOException {
        return seekable().position() - buffer.remaining();
    }

    /**
     * Seeks to a new byte position within the underlying channel.
     *
     * @param position location to seek to
     * @throws IOException when I/O error occurs
     *         UnsupportedOperationException when the channel is not seekable
     */
    public synchronized void position(long position) throws IOException {
        seekable().position(position);
        buffer.clear();
    }
}
