package org.netpreserve.jwarc;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class WarcRecordTest {
    @Test
    public void datePrecision() {
        Instant date = Instant.parse("2021-08-30T07:49:07.466148Z");
        WarcResource warc10Record = new WarcResource.Builder().version(MessageVersion.WARC_1_0).date(date).build();
        assertEquals(0, warc10Record.date().getNano());
        WarcResource warc11Record = new WarcResource.Builder().date(date).version(MessageVersion.WARC_1_1).build();
        assertEquals(466148000, warc11Record.date().getNano());
    }
}
