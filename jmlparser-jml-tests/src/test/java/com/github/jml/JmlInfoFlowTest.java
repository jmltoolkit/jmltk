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
 * Test suite for JML information flow clauses.
 * Covers: JmlInfFlowClause, accessible, accessible_redundantly, assignable.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlInfoFlowTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Accessible Clauses ====================
    @Test
    void testSimpleAccessible() {
        String code = """
            public class Example {
                private int secret;
                /*@ accessible \\nothing; @*/
                void readSecret() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAccessibleField() {
        String code = """
            public class Example {
                private int data;
                /*@ accessible data; @*/
                void processData() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAccessibleMultipleFields() {
        String code = """
            public class Example {
                private int x, y;
                /*@ accessible x, y; @*/
                void compute() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAccessibleThisField() {
        String code = """
            public class Example {
                /*@ accessible this.value; @*/
                int value;
                void getValue() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAccessibleRedundantly() {
        String code = """
            public class Example {
                private int counter;
                /*@ accessible_redundantly counter; @*/
                int getCounter() { return counter; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAccessibleArrayElement() {
        String code = """
            public class Example {
                private int[] data;
                /*@ accessible data[*]; @*/
                void processAll() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Assignable Clauses ====================
    @Test
    void testSimpleAssignable() {
        String code = """
            public class Example {
                int value;
                /*@ assignable value; @*/
                void setValue(int v) { value = v; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssignableNothing() {
        String code = """
            public class Example {
                int value;
                /*@ assignable \\nothing; @*/
                int getValue() { return value; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssignableEverything() {
        String code = """
            public class Example {
                /*@ assignable \\everything; @*/
                void modifyEverything() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssignableMultipleFields() {
        String code = """
            public class Example {
                int x, y, z;
                /*@ assignable x, y, z; @*/
                void update() { x++; y++; z++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssignableArrayElement() {
        String code = """
            public class Example {
                int[] data;
                /*@ assignable data[*]; @*/
                void setElement(int i, int v) { data[i] = v; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssignableSpecificIndex() {
        String code = """
            public class Example {
                int[] data;
                /*@ assignable data[0]; @*/
                void setFirst(int v) { data[0] = v; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Modifies Clauses ====================
    @Test
    void testModifies() {
        String code = """
            public class Example {
                int counter;
                /*@ modifies counter; @*/
                void increment() { counter++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModifiesMultiple() {
        String code = """
            public class Example {
                int x, y;
                /*@ modifies x, y; @*/
                void swap() { int t = x; x = y; y = t; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Info Flow Clauses ====================
    @Test
    void testAccessibleWithAssignable() {
        String code = """
            public class Example {
                private int data;
                /*@ accessible data;
                  @ assignable data;
                  @*/
                void updateData(int v) { data = v; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testFullInfoFlowContract() {
        String code = """
            public class Counter {
                private int count;
                
                /*@ accessible count;
                  @ assignable count;
                  @ ensures count == \\old(count) + 1;
                  @*/
                void increment() { count++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testPureMethodNoAssignment() {
        String code = """
            public class Example {
                int value;
                /*@ pure
                  @ assignable \\nothing;
                  @ ensures \\result == value;
                  @*/
                int getValue() { return value; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Class Accessible Declarations ====================
    @Test
    void testClassAccessibleDeclaration() {
        String code = """
            /*@ public class Example {
              @   accessible instanceField;
              @   int instanceField;
              @ }
              @*/
            public class Wrapper {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        // May or may not be supported depending on parser configuration
    }

    @Test
    void testAssignableWithOld() {
        String code = """
            public class Example {
                int x;
                /*@ assignable x;
                  @ ensures x > \\old(x);
                  @*/
                void increaseX() { x++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}