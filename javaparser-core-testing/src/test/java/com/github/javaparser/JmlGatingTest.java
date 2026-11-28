/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmlGatingTest {

    @Test
    void plainJavaWithAndWithoutJmlProcessingParsesEqually() {
        String source = "class X { void m() { int x = (1); } }";
        var off = new JavaParser(new ParserConfiguration().setProcessJml(false)).parse(source);
        var on = new JavaParser(new ParserConfiguration().setProcessJml(true)).parse(source);
        assertTrue(off.isSuccessful());
        assertTrue(on.isSuccessful());
        assertEquals(
                on.getResult().orElseThrow().toString(),
                off.getResult().orElseThrow().toString());
    }

    @Test
    void jmlMarkersWithProcessingOffStayParseable() {
        String source = "class X { //@ invariant true;\n void m() {} }";
        assertDoesNotThrow(() -> new JavaParser(new ParserConfiguration().setProcessJml(false)).parse(source));
    }
}
