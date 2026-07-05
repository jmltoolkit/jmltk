/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.key;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

/**
 * @author Alexander Weigl
 * @version 1 (29.06.23)
 */
public class KeYParsingTests {
    @Test
    void testGhostModifier() {
        Statement x = StaticJavaParser.parseStatement("/*@ ghost */ int x;");
    }

    @Test
    void testDLEscape() {
        Statement x = StaticJavaParser.parseStatement("x = \\dl_union(y,z);");
    }
}
