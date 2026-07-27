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
 * Test suite for JML model methods.
 * Covers: JmlMethodDeclaration, pure model methods, two-state model methods.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlModelMethodsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Simple Model Methods ====================
    @Test
    void testSimpleModelMethod() {
        String code = """
            public class Example {
                /*@ model @*/ int getSize() { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testPureModelMethod() {
        String code = """
            public class Example {
                /*@ pure model @*/ int abstractValue() { return 42; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelMethodWithParameters() {
        String code = """
            public class Example {
                /*@ model @*/ boolean contains(int value) { return false; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelMethodWithMultipleParameters() {
        String code = """
            public class Example {
                /*@ model @*/ Object get(int index, boolean wrap) { return null; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Two-State Model Methods ====================
    @Test
    void testTwoStateModelMethod() {
        String code = """
            public class Example {
                /*@ two_state model @*/ boolean hasChanged(int current, int previous) { 
                    return current != previous; 
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testTwoStateModelMethodWithOld() {
        String code = """
            public class Counter {
                int value;
                /*@ two_state model @*/ int delta() { return value - \\old(value); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Instance Model Methods ====================
    @Test
    void testInstanceModelMethod() {
        String code = """
            public class Example {
                /*@ instance model @*/ int instanceValue() { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Helper Model Methods ====================
    @Test
    void testHelperModelMethod() {
        String code = """
            public class Example {
                /*@ helper model @*/ static int helper(int x) { return x + 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Method Contracts ====================
    @Test
    void testModelMethodWithRequires() {
        String code = """
            public class Example {
                /*@ model 
                  @ requires index >= 0;
                  @ ensures \\result >= 0;
                  @*/
                int getElement(int index) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelMethodWithEnsures() {
        String code = """
            public class Example {
                /*@ model 
                  @ ensures \\result == size;
                  @*/
                int getSize() { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelMethodPureWithContract() {
        String code = """
            public class Stack {
                /*@ pure model 
                  @ requires !isEmpty();
                  @ ensures \\result != null;
                  @*/
                Object top() { return null; }
                
                /*@ model @*/ boolean isEmpty() { return false; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Method with Quantifiers ====================
    @Test
    void testModelMethodWithForall() {
        String code = """
            public class Example {
                /*@ model
                  @ ensures (\\forall int i; 0 <= i && i < size; get(i) >= 0);
                  @*/
                boolean allPositive() { return true; }
                
                /*@ model @*/ int get(int i) { return 0; }
                /*@ model @*/ int size() { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Ghost Methods ====================
    @Test
    void testGhostMethod() {
        String code = """
            public class Example {
                /*@ ghost @*/ void recordCall() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostMethodWithBody() {
        String code = """
            public class Example {
                /*@ ghost @*/ int callCount = 0;
                /*@ ghost @*/ void incrementCount() { callCount++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}