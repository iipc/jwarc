package org.netpreserve.jwarc;

import java.util.*;
import java.util.regex.Pattern;

/**
 * The `HeaderValidator` class validates MessageHeaders based on predefined rules. Rules can include which records types
 * a header field is allowed, forbidden or mandatory on and whether the value matches a regular expression.
 */
public class HeaderValidator {
    private final Map<String, FieldRule> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> mandatoryFields = new HashSet<>();
    private final Map<String, Set<String>> mandatoryFieldsByRecordType = new HashMap<>();
    private boolean forbidUnknownFields = false;

    class FieldRule {
        final String name;
        Pattern pattern;
        boolean repeatable;
        final Set<String> forbiddenOn = new HashSet<>();

        public FieldRule(String name) {
            this.name = name;
        }

        public FieldRule mandatory() {
            mandatoryFields.add(name);
            return this;
        }

        public FieldRule pattern(Pattern pattern) {
            this.pattern = pattern;
            return this;
        }

        public FieldRule repeatable() {
            this.repeatable = true;
            return this;
        }

        public FieldRule forbidOn(String... recordTypes) {
            Collections.addAll(forbiddenOn, recordTypes);
            return this;
        }

        public FieldRule requireOn(String... recordTypes) {
            for (String recordType : recordTypes) {
                mandatoryFieldsByRecordType
                        .computeIfAbsent(recordType, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                        .add(name);
            }
            return this;
        }
    }

    private HeaderValidator() {
    }

    /**
     * Creates and configures a HeaderValidator object for the WARC 1.1 standard. Extension headers and values will
     * be ignored.
     */
    public static HeaderValidator warc_1_1() {
        return warc_1_1(false);
    }

    /**
     * Creates and configures a HeaderValidator object for the WARC 1.1 standard.
     * <p>
     * The validation of field values is slightly relaxed from the grammar in the WARC 1.1 standard for
     * backwards compatibility with WARC 1.0 and in other cases recommended by the
     * <a href="https://iipc.github.io/warc-specifications/specifications/warc-format/warc-1.1-annotated/">
     * community annotations</a>.
     *
     * @param forbidExtensions if true unknown headers are forbidden and only standard values are accepted for
     *                        WARC-Type, WARC-Truncated and WARC-Profile.
     */
    public static HeaderValidator warc_1_1(boolean forbidExtensions) {
        // TODO: more complete URI validation
        String uriRegex = "(?:[a-zA-Z][a-zA-Z0-9+.-]*:)?.*";
        // we allow '<' URI '>' in some fields for backwards compatibility with WARC 1.0
        Pattern backwardsCompatibleUri = Pattern.compile("<" + uriRegex + ">|" + uriRegex);
        Pattern uri = Pattern.compile(uriRegex);
        
        Pattern recordId = Pattern.compile("<" + uriRegex + ">");
        Pattern nonNegativeInteger = Pattern.compile("[0-9]+");
        Pattern date = Pattern.compile("\\d{4}(-\\d{2}(-\\d{2}(T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?)?)?)?)?");

        String ows = "[ \\t]*";
        String token = "[-!#$%&'*+.^_`|~0-9A-Za-z]+";
        String quotedString = "\"(?:[^\"\\x00-\\x1F\\x7F]|\\\\.)*\"";
        String value = token + "|" + quotedString;
        String parameter = token + "=" + "(" + value + ")";
        // WARC 1.1 errata: allow OWS in media type parameters
        Pattern mediaType = Pattern.compile(token + "/" + token + ows + "(?:;" + ows + parameter + ")*");
        // community recommendation #48: allow / and @ for compatibility with Base32 and Base64
        String digestValue = "[-!#$%&'*+.^_`|~0-9A-Za-z/@]+";
        Pattern labelledDigest = Pattern.compile(token + ":" + digestValue);

        HeaderValidator v = new HeaderValidator();
        v.forbidUnknownFields = forbidExtensions;
        v.field("WARC-Record-ID").mandatory().pattern(recordId);
        v.field("Content-Length").mandatory().pattern(nonNegativeInteger);
        v.field("WARC-Date").mandatory().pattern(date);
        v.field("WARC-Type").mandatory().pattern(forbidExtensions ? Pattern.compile(
                "warcinfo|response|resource|request|metadata|revisit|conversion|continuation") : null);
        v.field("Content-Type").pattern(mediaType);
        v.field("WARC-Concurrent-To").pattern(recordId).repeatable()
                .forbidOn("warcinfo", "conversion", "continuation");
        v.field("WARC-Block-Digest").pattern(labelledDigest);
        v.field("WARC-Payload-Digest").pattern(labelledDigest);
        // TODO: ip address pattern
        v.field("WARC-IP-Address")
                .forbidOn("warcinfo", "conversion", "continuation");
        v.field("WARC-Refers-To")
                .pattern(recordId)
                .forbidOn("warcinfo", "response", "resource", "request", "continuation");
        v.field("WARC-Refers-To-Target-URI")
                .pattern(uri)
                .forbidOn("warcinfo", "response", "metadata", "conversion", "resource", "request", "continuation");
        v.field("WARC-Refers-To-Date")
                .pattern(date)
                .forbidOn("warcinfo", "response", "metadata", "conversion", "resource", "request", "continuation");
        v.field("WARC-Target-URI")
                .pattern(backwardsCompatibleUri)
                .forbidOn("warcinfo")
                .requireOn("response", "resource", "request", "revisit", "conversion", "continuation");
        v.field("WARC-Truncated")
                .pattern(forbidExtensions ? Pattern.compile("length|time|disconnect|unspecified") : null);
        v.field("WARC-Warcinfo-ID").pattern(recordId).forbidOn("warcinfo");
        v.field("WARC-Filename")
                .forbidOn("revisit", "response", "metadata", "conversion", "resource", "request", "continuation");
        FieldRule profileField = v.field("WARC-Profile")
                .requireOn("revisit");
        if (forbidExtensions) {
            profileField.pattern(Pattern.compile(
                    "\\Qhttp://netpreserve.org/warc/1.1/revisit/identical-payload-digest\\E"
                    + "|\\Qhttp://netpreserve.org/warc/1.1/revisit/server-not-modified\\E"));
            profileField.forbidOn("warcinfo", "response", "metadata", "conversion", "resource", "request", "continuation");
        } else {
            profileField.pattern(backwardsCompatibleUri);
        }
        v.field("WARC-Identified-Payload-Type").pattern(mediaType);
        v.field("WARC-Segment-Number").pattern(nonNegativeInteger);
        v.field("WARC-Segment-Origin-ID").pattern(nonNegativeInteger)
                .requireOn("continuation")
                .forbidOn("warcinfo", "response", "metadata", "conversion", "resource", "request", "revisit");
        v.field("WARC-Segment-Total-Length").pattern(nonNegativeInteger)
                    .forbidOn("warcinfo", "response", "metadata", "conversion", "resource", "request", "revisit");
        return v;
    }

    private FieldRule field(String name) {
        return fields.computeIfAbsent(name, FieldRule::new);
    }

    /**
     * Validates the given set of message headers.
     *
     * @param headers the MessageHeaders object containing headers to be validated
     * @return a list of strings describing any validation violations found
     */
    public List<String> validate(MessageHeaders headers) {
        List<String> violations = new ArrayList<>();

        String recordType = headers.first("WARC-Type").orElse(null);

        for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();

            FieldRule fieldRule = fields.get(name);
            if (fieldRule == null) {
                if (forbidUnknownFields) {
                    violations.add("Unknown field: " + name);
                }
                continue;
            }

            if (!fieldRule.repeatable && values.size() > 1) {
                violations.add("Field must not be repeated: " + name);
            }

            if (recordType != null && fieldRule.forbiddenOn.contains(recordType)) {
                violations.add("Field not allowed on " + recordType + " record: " + name);
            }

            if (fieldRule.pattern != null) {
                for (String value : values) {
                    if (!fieldRule.pattern.matcher(value).matches()) {
                        violations.add("Field has invalid value: " + value);
                    }
                }
            }
        }

        // Check for fields mandatory on all records
        Set<String> names = headers.map().keySet();
        for (String field : mandatoryFields) {
            if (!names.contains(field)) {
                violations.add("Missing mandatory field: " + field);
            }
        }

        // Check for fields mandatory on specific record types
        if (recordType != null) {
            for (String name : mandatoryFieldsByRecordType.getOrDefault(recordType, Collections.emptySet())) {
                if (!names.contains(name)) {
                    violations.add("Missing mandatory field for " + recordType + " record: " + name);
                }
            }
        }

        return violations;
    }

}
