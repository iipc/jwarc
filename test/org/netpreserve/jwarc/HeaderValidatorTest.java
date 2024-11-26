package org.netpreserve.jwarc;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class HeaderValidatorTest {
    private HeaderValidator headerValidator = HeaderValidator.warc_1_1();

    @Test
    public void testValid() {
        MessageHeaders headers = MessageHeaders.of(
                "WARC-Record-ID", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f544d>",
                "Content-Length", "123456",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Type", "response",
                "WARC-Target-URI", "http://example.com/",
                "Content-Type", "application/http; msgtype=response",
                "WARC-Concurrent-To", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f5441>",
                "WARC-Concurrent-To", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f5442>"
        );
        assertEquals(Collections.emptyList(), headerValidator.validate(headers));
    }

    @Test
    public void testMissingMandatoryFields() {
        MessageHeaders headers = MessageHeaders.of(
                "Content-Length", "123456",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Type", "response"
        );
        List<String> validationErrors = headerValidator.validate(headers);
        assertFalse(validationErrors.isEmpty());
        assertTrue(validationErrors.contains("Missing mandatory field: WARC-Record-ID"));
    }

    @Test
    public void testInvalidPatternValidation() {
        MessageHeaders headers = MessageHeaders.of(
                "WARC-Record-ID", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f544d>",
                "Content-Length", "123456",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Type", "response",
                "Content-Type", "invalid_content_type"
        );
        List<String> validationErrors = headerValidator.validate(headers);
        assertFalse(validationErrors.isEmpty());
        assertTrue(validationErrors.contains("Field has invalid value: invalid_content_type"));
    }

    @Test
    public void testNonRepeatableField() {
        MessageHeaders headers = MessageHeaders.of(
                "WARC-Record-ID", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f544d>",
                "Content-Length", "123456",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Type", "response",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Date", "2020-01-02T00:00:00Z"
        );
        List<String> validationErrors = headerValidator.validate(headers);
        assertFalse(validationErrors.isEmpty());
        assertTrue(validationErrors.contains("Field must not be repeated: WARC-Date"));
    }

    @Test
    public void testForbiddenFieldsOnRecordType() {
        MessageHeaders headers = MessageHeaders.of(
                "WARC-Record-ID", "<urn:uuid:6c73a5d3-cab3-46b3-b0b6-3b4b617f544d>",
                "Content-Length", "123456",
                "WARC-Date", "2020-01-01T00:00:00Z",
                "WARC-Type", "response",
                "WARC-Filename", "test.warc.gz"
        );
        List<String> validationErrors = headerValidator.validate(headers);
        assertFalse(validationErrors.isEmpty());
        assertTrue(validationErrors.contains("Field not allowed on response record: WARC-Filename"));
    }
}