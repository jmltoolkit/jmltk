/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parsePackageDeclaration;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

class ReplaceNodeTest {
    @Test
    void testSimplePropertyWithGenericReplace() {
        CompilationUnit cu = parse("package x; class Y {}");
        cu.replace(cu.getPackageDeclaration().get(), parsePackageDeclaration("package z;"));
        assertEquals(
                String.format("package z;%1$s" + "%1$s" + "class Y {%1$s" + "}%1$s", LineSeparator.SYSTEM),
                cu.toString());
    }

    @Test
    void testListProperty() {
        CompilationUnit cu = parse("package x; class Y {}");
        cu.replace(
                cu.getClassByName("Y").get(),
                parse("class B{int y;}").getClassByName("B").get());
        assertEquals(
                String.format(
                        "package x;%1$s" + "%1$s" + "class B {%1$s" + "%1$s" + "    int y;%1$s" + "}%1$s",
                        LineSeparator.SYSTEM),
                cu.toString());
    }
}
