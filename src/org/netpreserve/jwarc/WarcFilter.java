package org.netpreserve.jwarc;

import java.util.function.Predicate;

/**
 * Filter expressions for matching WARC records.
 * <p>
 * Simplified grammar for the expression language:
 * <pre>
 * {@code
 * expression = "(" expression ")"         ; grouping
 *            | "!(" expression ")"        ; boolean NOT
 *            | expression "&&" expression ; boolean AND
 *            | expression "||" expression ; boolean OR
 *            | field "==" string          ; string equality
 *            | field "!=" string          ; string inequality
 *            | field "=~" string          ; regex match
 *            | field "!~" string          ; regex non-match
 *            | field "==" number          ; integer equality
 *            | field "!=" number          ; integer inequality
 *            | field "<"  number          ; integer less-than
 *            | field "<=" number          ; integer less-than-or-equal
 *            | field ">"  number          ; integer greater-than
 *            | field ">=" number          ; integer greater-than-or-equal
 *
 * field = ":status"          ; HTTP response code psuedo-field
 *       | "http:" field-name ; HTTP header field
 *       | field-name         ; WARC header field
 *
 * string = '"' [^"]* '"'
 * }
 * </pre>
 * Whitespace outside a string or field is ignored. Fields that do not exist are treated as an empty string when subject
 * to string comparison. Fields that do not contain a valid number are treated as zero when subject to integer
 * comparison.
 */
public class WarcFilter implements Predicate<WarcRecord> {
    private final String expression;
    private final Predicate<WarcRecord> predicate;

    private WarcFilter(String expression, Predicate<WarcRecord> predicate) {
        this.expression = expression;
        this.predicate = predicate;
    }

    /**
     * Compiles a filter expression from a string.
     *
     * @throws WarcFilterException when the expression contains a syntax error
     */
    public static WarcFilter compile(String expression) {
        return new WarcFilter(expression, new WarcFilterCompiler(expression).predicate());
    }

    @Override
    public boolean test(WarcRecord warcRecord) {
        return predicate.test(warcRecord);
    }

    @Override
    public String toString() {
        return expression;
    }
}
