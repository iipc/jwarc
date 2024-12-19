package org.netpreserve.jwarc;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * A set for testing whether WARC records are concurrent (i.e. part of the same capture event).
 */
public class ConcurrentRecordSet {
    private final Set<URI> set = new HashSet<>();

    /**
     * Adds a record to the set.
     */
    public void add(WarcRecord record) {
        set.add(record.id());
        if (record instanceof WarcCaptureRecord) {
            set.addAll(((WarcCaptureRecord) record).concurrentTo());
        }
    }

    /**
     * Tests if the given record is concurrent to any previously added record.
     */
    public boolean contains(WarcRecord record) {
        if (set.contains(record.id())) return true;
        if (record instanceof WarcCaptureRecord) {
            for (URI id : ((WarcCaptureRecord) record).concurrentTo()) {
                if (set.contains(id)) return true;
            }
        }
        return false;
    }

    /**
     * Removes all records from the set.
     */
    public void clear() {
        set.clear();
    }
}
