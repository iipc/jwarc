package org.netpreserve.jwarc;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WarcFilterLexer {
    private static Pattern REGEX = Pattern.compile("([a-zA-Z0-9:_-]+)|(&&|[|][|]|!=|==|!~|=~|[<>]=?|!?[(]|[)])|\"([^\"]*)\"|(\\s+)");
    private static final int TOKEN = 1, OPERATOR = 2, STRING = 3, WHITESPACE = 4;

    private final Matcher matcher;

    WarcFilterLexer(String expression) {
        this.matcher = REGEX.matcher(expression);
    }

    Object stringOrNumber() throws ParseException {
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
        if (value == null) throw new ParseException("expected string or integer", position());
        advance();
        return value;
    }

    String string() throws ParseException {
        String str = peek().group(STRING);
        if (str == null) throw new ParseException("expected string", position());
        advance();
        return str;
    }

    String token() throws ParseException {
        String field = peek().group(TOKEN);
        if (field == null) throw new ParseException("expected field name", position());
        advance();
        return field;
    }

    String operator() throws ParseException {
        String operator = peekOperator();
        if (operator == null) throw new ParseException("expected operator", position());
        advance();
        return operator;
    }

    String peekOperator() throws ParseException {
        return peek().group(OPERATOR);
    }

    private Matcher peek() throws ParseException {
        while (true) {
            if (!matcher.lookingAt()) throw new ParseException("syntax error", position());
            if (matcher.group(WHITESPACE) == null) return matcher;
            advance();
        }
    }

    void advance() {
        matcher.region(matcher.end(), matcher.regionEnd());
    }

    int position() {
        return matcher.regionStart();
    }

    boolean atEnd() {
        return matcher.regionStart() == matcher.regionEnd();
    }
}
