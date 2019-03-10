package org.netpreserve.jwarc;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class WarcFilterCompiler {
    private final WarcFilterLexer lexer;

    WarcFilterCompiler(String expression) {
        lexer = new WarcFilterLexer(expression);
    }

    /*
     * predicate =  unary
     *            | unary "&&" predicate
     *            | unary "||" predicate;
     */
    Predicate<WarcRecord> predicate() {
        Predicate<WarcRecord> lhs = unary();
        if (lexer.atEnd()) return lhs;
        String operator = lexer.peekOperator();
        if (operator == null) throw lexer.error("expected operator");
        switch (operator) {
            case ")":
                return lhs;
            case "&&":
                lexer.advance();
                return lhs.and(predicate());
            case "||":
                lexer.advance();
                return lhs.or(predicate());
            default:
                throw lexer.error("operator not allowed here: " + operator);
        }
    }

    /*
     * unary = comparison
     *      | "(" predicate ")"
     *       | "!(" predicate ")";
     */
    private Predicate<WarcRecord> unary() {
        String operator = lexer.peekOperator();
        if (operator == null) {
            return comparison();
        } else if (operator.equals("!(") || operator.equals("(")) {
            lexer.advance();
            Predicate<WarcRecord> predicate = predicate();
            if (!")".equals(lexer.operator())) {
                throw lexer.error("')' expected");
            }
            return operator.startsWith("!") ? predicate.negate() : predicate;
        } else {
            throw lexer.error("operator not allowed here: " + operator);
        }
    }

    /*
     * comparison = field "==" (string/number)
     *            | field "!=" (string/number)
     *            | field "=~" string
     *            | field "!~" string
     *            | field "<" number
     *            | field ">" number
     *            | field "<=" number
     *            | field ">=" number;
     */
    private Predicate<WarcRecord> comparison() {
        Accessor lhs = accessor(lexer.token());
        String operator = lexer.operator();
        if (operator == null) throw lexer.error("expected operator");
        switch (operator) {
            case "==":
            case "!=": {
                Object rhs = lexer.stringOrNumber();
                Predicate<WarcRecord> pred;
                if (rhs instanceof String) {
                    pred = rec -> lhs.string(rec).equals(rhs);
                } else {
                    long value = (Long) rhs;
                    pred = rec -> lhs.integer(rec) == value;
                }
                return operator.equals("!=") ? pred.negate() : pred;
            }
            case "=~": {
                Pattern pattern = Pattern.compile(lexer.string());
                return rec -> pattern.matcher(lhs.string(rec)).matches();
            }
            case "!~": {
                Pattern pattern = Pattern.compile(lexer.string());
                return rec -> !pattern.matcher(lhs.string(rec)).matches();
            }
            case "<": {
                long value = Long.parseLong(lexer.token());
                return rec -> lhs.integer(rec) < value;
            }
            case "<=": {
                long value = Long.parseLong(lexer.token());
                return rec -> lhs.integer(rec) <= value;
            }
            case ">=": {
                long value = Long.parseLong(lexer.token());
                return rec -> lhs.integer(rec) >= value;
            }
            case ">": {
                long value = Long.parseLong(lexer.token());
                return rec -> lhs.integer(rec) > value;
            }
        }
        throw lexer.error("operator not allowed here: " + operator);
    }

    private interface Accessor {
        Optional<String> raw(WarcRecord record);

        default String string(WarcRecord record) {
            return raw(record).orElse("");
        }

        default long integer(WarcRecord record) {
            try {
                return Long.parseLong(raw(record).orElse("0"));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private Accessor accessor(String token) {
        if (":status".equals(token)) {
            return record -> {
                try {
                    if (!(record instanceof WarcResponse)) return Optional.empty();
                    if (!record.contentType().equals(MediaType.HTTP_RESPONSE)) return Optional.empty();
                    return Optional.of(Integer.toString(((WarcResponse) record).http().status()));
                } catch (Exception e) {
                    return Optional.empty();
                }
            };
        } else if (token.startsWith("http:")) {
            String field = token.substring("http:".length());
            return record -> {
                try {
                    if (record instanceof WarcRequest && record.contentType().equals(MediaType.HTTP_REQUEST)) {
                        return ((WarcRequest) record).http().headers().first(field);
                    } else if (record instanceof WarcResponse && record.contentType().equals(MediaType.HTTP_RESPONSE)) {
                        return ((WarcResponse) record).http().headers().first(field);
                    } else {
                        return Optional.empty();
                    }
                } catch (Exception e) {
                    return Optional.empty();
                }
            };
        } else {
            return record -> record.headers().first(token);
        }
    }
}
