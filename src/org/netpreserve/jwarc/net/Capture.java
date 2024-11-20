package org.netpreserve.jwarc.net;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;

/**
 * An entry in the {@link CaptureIndex}.
 * <p>
 * Hods the location of a particular captured version of a resource.
 */
class Capture {
    private final String uri;
    private final Instant date;
    private final Path file;
    private final long position;

    Capture(String uri, Instant date) {
        this(uri, date, null, -1);
    }

    Capture(String uri, Instant date, Path file, long position) {
        this.uri = uri;
        this.date = date;
        this.file = file;
        this.position = position;
    }

    Instant date() {
        return date;
    }

    String uri() {
        return uri;
    }

    Path file() {
        return file;
    }

    long position() {
        return position;
    }
}
