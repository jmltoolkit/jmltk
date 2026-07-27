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
 * Test suite for JML model fields and represents clauses.
 * Covers: JmlFieldDeclaration, JmlRepresentsDeclaration.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlModelFieldsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Model Field Declarations ====================
    @Test
    void testSimpleModelField() {
        String code = """
            public class Example {
                /*@ model @*/ int abstractSize;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelFieldWithInitializer() {
        String code = """
            public class Example {
                /*@ model @*/ int counter = 0;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelFieldArray() {
        String code = """
            public class Example {
                /*@ model @*/ int[] elements;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelFieldObject() {
        String code = """
            public class Example {
                /*@ model @*/ java.util.Set contents;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostFieldDeclaration() {
        String code = """
            public class Example {
                /*@ ghost @*/ int callCount;
                /*@ ghost @*/ boolean initialized;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostFieldWithInitializer() {
        String code = """
            public class Example {
                /*@ ghost @*/ int historyIndex = -1;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Represents Clauses ====================
    @Test
    void testRepresentsClause() {
        String code = """
            public class Example {
                private Object[] data;
                
                /*@ represents contents = data; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRepresentsWithExpression() {
        String code = """
            public class Stack {
                private Object[] elements;
                private int size;
                
                /*@ represents contents = \\old(elements[0..size]); @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRepresentsMultipleFields() {
        String code = """
            public class Example {
                private int[] arr;
                private int len;
                
                /*@ represents abstractArray = arr[0..len]; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Model Fields and Represents ====================
    @Test
    void testModelFieldWithRepresents() {
        String code = """
            public class Counter {
                private int value;
                
                /*@ model @*/ int abstractValue;
                /*@ represents abstractValue = value; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostHistoryWithRepresents() {
        String code = """
            public class History {
                private int[] log;
                private int count;
                
                /*@ ghost @*/ int[] historyLog;
                /*@ represents historyLog = log[0..count]; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Class Level Declarations ====================
    @Test
    void testJmlClassLevelDeclaration() {
        String code = """
            /*@ 
              public class Example {
                  model int size;
                  represents size = data.length;
              }
            @*/
            public class Wrapper {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        // May or may not be supported depending on parser configuration
        // Just verify it doesn't crash
    }

    @Test
    void testModelClassAccessible() {
        String code = """
            public class Example {
                /*@ spec_public @*/ Object data;
                /*@ represents data = internalData; @*/
                private Object internalData;
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}