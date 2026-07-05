/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class Issue3064Test {

    @Test
    public void test0() {
        String str = "import java.util.function.Supplier;\n" + "\n"
                + "public class MyClass {\n"
                + "\n"
                + "    public MyClass() {\n"
                + "        Supplier<String> aStringSupplier = false ? () -> \"\" : true ? () -> \"\" : () -> \"path\";\n"
                + "    }\n"
                + "}\n";

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> unitOpt = parser.parse(new StringReader(str));
        unitOpt.getProblems().stream().forEach(p -> System.err.println(p.toString()));
        CompilationUnit unit = unitOpt.getResult().orElseThrow(() -> new IllegalStateException("Could not parse file"));

        assertEquals(str, unit.toString());
    }

    @Test
    public void test1() {
        String str = "public class MyClass {\n" + "    {\n"
                + "        Supplier<String> aStringSupplier = false ? () -> \"F\" : true ? () -> \"T\" : () -> \"path\";\n"
                + "    }\n"
                + "}";
        CompilationUnit unit = StaticJavaParser.parse(str);
        assertEquals(str.replace("\n", ""), unit.toString().replace("\n", ""));
    }
}
