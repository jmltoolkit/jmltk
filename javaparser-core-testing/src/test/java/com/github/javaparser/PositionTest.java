/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PositionTest {

    @Test
    public void testOrIfInvalid() {
        Position p1 = new Position(1, 1);
        Position p2 = new Position(2, 2);

        assertEquals(p1, p1.orIfInvalid(p2));

        Position invalid = new Position(0, 0);
        Position invalid2 = new Position(0, 1);

        assertEquals(p1, invalid.orIfInvalid(p1));
        assertEquals(invalid2, invalid2.orIfInvalid(invalid));
    }

    @Test
    public void testPositionExceptionFormat() {
        IllegalArgumentException thrown1 =
                Assertions.assertThrows(IllegalArgumentException.class, () -> new Position(-10, 1));

        assertEquals("Can't position at line -10", thrown1.getMessage());

        IllegalArgumentException thrown2 =
                Assertions.assertThrows(IllegalArgumentException.class, () -> new Position(1, -10));

        assertEquals("Can't position at column -10", thrown2.getMessage());
    }
}
