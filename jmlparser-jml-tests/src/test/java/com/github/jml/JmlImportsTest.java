/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JML model import declarations.
 * Covers: model import statements for specification types.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlImportsTest {
    private final JavaParser javaParser = new JavaParser();

    @Test
    void testSimpleModelImport() {
        String code = """
            package example;
            //@ model import java.util.Set;
            public class Example {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMultipleModelImports() {
        String code = """
            package example;
            //@ model import java.util.Set;
            //@ model import java.util.Map;
            public class Example {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelImportWithWildcard() {
        String code = """
            package example;
            //@ model import java.util.*;
            public class Example {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMixedRegularAndModelImports() {
        String code = """
            package example;
            import java.util.List;
            //@ model import java.util.Set;
            import java.util.HashMap;
            //@ model import java.util.Map;
            public class Example {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testStaticModelImport() {
        String code = """
            package example;
            //@ model import static java.lang.Math.PI;
            public class Example {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}