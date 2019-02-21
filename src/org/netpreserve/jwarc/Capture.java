package org.netpreserve.jwarc;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;

/**
 * An entry in the {@link CaptureIndex}.
 * <p>
 * Hods the location of a particular captured version of a resource.
 */
class Capture {
    private final String urikey;
    private final URI uri;
    private final Instant date;
    private final Path file;
    private final long position;

    Capture(URI uri, Instant date) {
        this(uri, date, null, -1);
    }

    Capture(URI uri, Instant date, Path file, long position) {
        urikey = uri.toString();
        this.uri = uri;
        this.date = date;
        this.file = file;
        this.position = position;
    }

    String uriKey() {
        return urikey;
    }

    Instant date() {
        return date;
    }

    URI uri() {
        return uri;
    }

    Path file() {
        return file;
    }

    long position() {
        return position;
    }
}
