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
 * Test suite for JML signals clauses.
 * Covers: JmlSignalsClause, JmlSignalsOnlyClause.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlSignalsClausesTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Basic Signals Clauses ====================
    @Test
    void testSimpleSignals() {
        String code = """
            public class Example {
                /*@ signals (Exception e) true; @*/
                void method() throws Exception {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsWithSpecificException() {
        String code = """
            public class Example {
                /*@ signals (IllegalArgumentException e) x < 0; @*/
                void setX(int x) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsWithCondition() {
        String code = """
            public class Example {
                /*@ signals (NullPointerException e) obj == null; @*/
                void process(Object obj) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsWithOld() {
        String code = """
            public class Counter {
                int value;
                /*@ signals (IllegalStateException e) value > \\old(value) + 100; @*/
                void increment() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Signals Only Clauses ====================
    @Test
    void testSignalsOnly() {
        String code = """
            public class Example {
                /*@ signals_only (Exception e); @*/
                void method() throws Exception {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsOnlyMultipleExceptions() {
        String code = """
            public class Example {
                /*@ signals_only (IOException e, RuntimeException re); @*/
                void riskyMethod() throws IOException {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Signals with Other Clauses ====================
    @Test
    void testRequiresSignals() {
        String code = """
            public class Example {
                /*@ requires divisor != 0;
                  @ signals (ArithmeticException e) divisor == 0;
                  @*/
                int divide(int dividend, int divisor) { return dividend / divisor; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testEnsuresSignals() {
        String code = """
            public class Example {
                /*@ ensures \\result >= 0;
                  @ signals (IllegalArgumentException e) x < 0;
                  @*/
                int absOrThrow(int x) { return Math.abs(x); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testFullContractWithSignals() {
        String code = """
            public class Stack {
                int size;
                
                /*@ requires !isEmpty();
                  @ ensures \\result == topElement;
                  @ signals (EmptyStackException e) isEmpty();
                  @*/
                Object pop() throws EmptyStackException { return null; }
                
                /*@ model @*/ boolean isEmpty() { return size == 0; }
            }
            
            class EmptyStackException extends Exception {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsWithQuantifier() {
        String code = """
            public class Example {
                /*@ signals (IndexOutOfBoundsException e) 
                  @   index < 0 || index >= arr.length;
                  @*/
                int get(int[] arr, int index) { return arr[index]; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Multiple Signal Clauses ====================
    @Test
    void testMultipleSignalsClauses() {
        String code = """
            public class Example {
                /*@ signals (NullPointerException e) arg1 == null;
                  @ signals (IllegalArgumentException e) arg2 < 0;
                  @*/
                void validate(Object arg1, int arg2) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSignalsWithAssignable() {
        String code = """
            public class Example {
                /*@ signals (RuntimeException e) true;
                  @ assignable \\nothing;
                  @*/
                void checkState() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}