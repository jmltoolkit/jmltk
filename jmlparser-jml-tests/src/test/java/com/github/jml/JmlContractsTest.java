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
 * Test suite for JML contracts.
 * Covers: JmlContract, requires, ensures, diverges, terminates, measured_by.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlContractsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Requires (Preconditions) ====================
    @Test
    void testSimpleRequires() {
        String code = """
            public class Example {
                /*@ requires x > 0; @*/
                void method(int x) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMultipleRequires() {
        String code = """
            public class Example {
                /*@ requires x > 0;
                  @ requires y >= 0;
                  @ requires x + y < Integer.MAX_VALUE;
                  @*/
                int add(int x, int y) { return x + y; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRequiresWithNullCheck() {
        String code = """
            public class Example {
                /*@ requires obj != null; @*/
                void process(Object obj) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRequiresWithArrayLength() {
        String code = """
            public class Example {
                /*@ requires arr.length > 0;
                  @ requires 0 <= index && index < arr.length;
                  @*/
                int get(int[] arr, int index) { return arr[index]; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testRequiresWithQuantifier() {
        String code = """
            public class Example {
                /*@ requires (\\forall int i; 0 <= i && i < arr.length; arr[i] >= 0); @*/
                int sum(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Ensures (Postconditions) ====================
    @Test
    void testSimpleEnsures() {
        String code = """
            public class Example {
                /*@ ensures \\result >= 0; @*/
                int absoluteValue(int x) { return Math.abs(x); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testEnsuresWithOld() {
        String code = """
            public class Counter {
                int value;
                /*@ ensures value == \\old(value) + 1; @*/
                void increment() { value++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testEnsuresWithResult() {
        String code = """
            public class Example {
                /*@ ensures \\result == x * 2; @*/
                int doubleValue(int x) { return x * 2; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testEnsuresWithMultipleConditions() {
        String code = """
            public class Example {
                /*@ ensures \\result >= 0;
                  @ ensures \\result <= x;
                  @ ensures \\result <= y;
                  @*/
                int min(int x, int y) { return Math.min(x, y); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testEnsuresWithQuantifier() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i; 0 <= i && i < \\result.length; \\result[i] == arr[i] * 2); @*/
                int[] doubleAll(int[] arr) { return arr; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Diverges/Terminates ====================
    @Test
    void testDiverges() {
        String code = """
            public class Example {
                /*@ diverges true; @*/
                void infiniteLoop() { while(true) {} }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testTerminates() {
        String code = """
            public class Example {
                /*@ terminates; @*/
                int getValue() { return 42; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testTerminatesWithCondition() {
        String code = """
            public class Example {
                /*@ terminates_when x >= 0; @*/
                int compute(int x) { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Measured By (Termination Measures) ====================
    @Test
    void testMeasuredBy() {
        String code = """
            public class Example {
                /*@ measured_by n; @*/
                int factorial(int n) { return 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMeasuredByExpression() {
        String code = """
            public class Example {
                /*@ measured_by upper - lower; @*/
                int binarySearch(int[] arr, int lower, int upper) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Contracts ====================
    @Test
    void testRequiresEnsuresCombined() {
        String code = """
            public class Example {
                /*@ requires divisor != 0;
                  @ ensures \\result * divisor == dividend;
                  @*/
                int divide(int dividend, int divisor) { return dividend / divisor; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testFullContract() {
        String code = """
            public class Counter {
                int value;
                
                /*@ requires n > 0;
                  @ ensures value == \\old(value) + n;
                  @ ensures \\result == value;
                  @ terminates;
                  @*/
                int add(int n) { 
                    value += n; 
                    return value; 
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testCallableClause() {
        String code = """
            public class Example {
                /*@ callable; @*/
                void safeMethod() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}