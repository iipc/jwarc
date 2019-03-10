package org.netpreserve.jwarc;

import java.util.Arrays;

/**
 * Thrown when a syntax error is encountered when compiling a filter expression.
 */
public class WarcFilterException extends RuntimeException {
    private final String input;
    private final int position;

    public WarcFilterException(String message, int position, String input) {
        super(message);
        this.position = position;
        this.input = input;
    }

    /**
     * Returns the character position of the error within the input.
     */
    public int position() {
        return position;
    }

    /**
     * Returns the expression containing the error.
     */
    public String input() {
        return input;
    }

    /**
     * Returns a user-friendly error message.
     */
    public String prettyPrint() {
        char[] indent = new char[position];
        Arrays.fill(indent, ' ');
        return input + "\n" + new String(indent) + "^\nError: " + getMessage();
    }
}
