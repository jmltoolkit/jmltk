/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.quality;

import static com.github.javaparser.quality.Preconditions.checkArgument;
import static com.github.javaparser.quality.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PreconditionsTest {

    @Test
    void checkArgument_withTrueExpression() {
        checkArgument(true);
    }

    @Test
    void checkArgument_withFalseExpression() {
        assertThrows(IllegalArgumentException.class, () -> checkArgument(false));
    }

    @Test
    void checkNotNull_withNonNull() {
        checkNotNull(new Object());
    }

    @Test
    void checkNotNull_withNull() {
        assertThrows(IllegalArgumentException.class, () -> checkNotNull(null));
    }
}
