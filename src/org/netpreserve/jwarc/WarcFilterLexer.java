package org.netpreserve.jwarc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WarcFilterLexer {
    private static Pattern REGEX = Pattern.compile("([a-zA-Z0-9:_-]+)|(&&|[|][|]|!=|==|!~|=~|[<>]=?|!?[(]|[)])|\"([^\"]*)\"|(\\s+)");
    private static final int TOKEN = 1, OPERATOR = 2, STRING = 3, WHITESPACE = 4;

    private final String input;
    private final Matcher matcher;

    WarcFilterLexer(String input) {
        this.input = input;
        this.matcher = REGEX.matcher(input);
    }

    Object stringOrNumber() {
        Object value = peek().group(STRING);
        if (value == null) {
            String token = matcher.group(TOKEN);
            if (token != null) {
                try {
                    value = Long.parseLong(matcher.group(TOKEN));
                } catch (NumberFormatException e) {
                    // not a number
                }
            }
        }
        if (value == null) throw error("expected string or integer");
        advance();
        return value;
    }

    String string() {
        String str = peek().group(STRING);
        if (str == null) throw error("expected string");
        advance();
        return str;
    }

    String token() {
        String field = peek().group(TOKEN);
        if (field == null) throw error("expected field name");
        advance();
        return field;
    }

    String operator() {
        String operator = peekOperator();
        if (operator == null) throw error("expected operator");
        advance();
        return operator;
    }

    String peekOperator() {
        return peek().group(OPERATOR);
    }

    private Matcher peek() {
        while (true) {
            if (atEnd()) throw error("unexpected end of input");
            if (!matcher.lookingAt()) throw error("syntax error");
            if (matcher.group(WHITESPACE) == null) return matcher;
            advance();
        }
    }

    void advance() {
        matcher.region(matcher.end(), matcher.regionEnd());
    }

    WarcFilterException error(String message) {
        return new WarcFilterException(message, matcher.regionStart(), input);
    }

    boolean atEnd() {
        return matcher.regionStart() == matcher.regionEnd();
    }
}
