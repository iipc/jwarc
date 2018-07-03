/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;
import java.util.Optional;

public interface HasRefersTo {
    /**
     * The record which this one describes.
     */
    Optional<URI> refersTo();
}
