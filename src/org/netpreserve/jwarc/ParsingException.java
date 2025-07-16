/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;

public class ParsingException extends IOException {
    RecordSource recordSource;

    public ParsingException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        if (recordSource != null) {
            return super.getMessage() + " (" + recordSource + ")";
        } else {
            return super.getMessage();
        }
    }

    public String getBaseMessage() {
        return super.getMessage();
    }
}
