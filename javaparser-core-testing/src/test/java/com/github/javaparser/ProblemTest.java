/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemTest {
    @Test
    void testSimpleGetters() {
        Problem problem = new Problem("Parse error", TokenRange.INVALID, new Exception());

        assertEquals(TokenRange.INVALID, problem.getLocation().get());
        assertEquals("Parse error", problem.getMessage());
        assertInstanceOf(Exception.class, problem.getCause().get());
    }

    @Test
    void testVerboseMessage() {
        Problem problem = new Problem("Parse error", TokenRange.INVALID, null);

        assertEquals("(line ?,col ?) Parse error", problem.getVerboseMessage());
    }

    @Test
    void testVerboseMessageWithoutLocation() {
        Problem problem = new Problem("Parse error", null, null);

        assertEquals("Parse error", problem.getVerboseMessage());
    }
}
