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
 * Test suite for JML behavioral cases.
 * Covers: normal_behavior, exceptional_behavior, also.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlBehaviorCasesTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Normal Behavior ====================
    @Test
    void testNormalBehavior() {
        String code = """
            public class Example {
                /*@ normal_behavior
                  @ requires x >= 0;
                  @ ensures \\result == x + 1;
                  @*/
                int increment(int x) { return x + 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNormalBehaviorWithAssignable() {
        String code = """
            public class Counter {
                int count;
                /*@ normal_behavior
                  @ requires n > 0;
                  @ assignable count;
                  @ ensures count == \\old(count) + n;
                  @*/
                void add(int n) { count += n; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNormalBehaviorComplete() {
        String code = """
            public class MathUtils {
                /*@ normal_behavior
                  @ requires a >= 0 && b > 0;
                  @ ensures \\result * b <= a && a < (\\result + 1) * b;
                  @ ensures \\result >= 0;
                  @*/
                int divide(int a, int b) { return a / b; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Exceptional Behavior ====================
    @Test
    void testExceptionalBehavior() {
        String code = """
            public class Example {
                /*@ exceptional_behavior
                  @ requires x < 0;
                  @ signals (IllegalArgumentException e) true;
                  @*/
                void setPositive(int x) throws IllegalArgumentException {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testExceptionalBehaviorWithCondition() {
        String code = """
            public class Stack {
                int size;
                /*@ exceptional_behavior
                  @ requires size == 0;
                  @ signals (EmptyStackException e) size == 0;
                  @*/
                Object pop() throws EmptyStackException { return null; }
            }
            class EmptyStackException extends Exception {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testExceptionalBehaviorNull() {
        String code = """
            public class Example {
                /*@ exceptional_behavior
                  @ requires obj == null;
                  @ signals (NullPointerException e) obj == null;
                  @*/
                void process(Object obj) throws NullPointerException {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Also (Combining Behaviors) ====================
    @Test
    void testAlsoNormalAndExceptional() {
        String code = """
            public class Example {
                /*@ normal_behavior
                  @ requires x >= 0;
                  @ ensures \\result == x;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires x < 0;
                  @ signals (IllegalArgumentException e) x < 0;
                  @*/
                int checkNonNegative(int x) throws IllegalArgumentException { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAlsoMultipleNormalBehaviors() {
        String code = """
            public class Example {
                /*@ normal_behavior
                  @ requires x > 0;
                  @ ensures \\result > 0;
                  @*/
                /*@ also
                  @ normal_behavior
                  @ requires x == 0;
                  @ ensures \\result == 0;
                  @*/
                int handleNonNegative(int x) { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAlsoThreeBehaviors() {
        String code = """
            public class SignChecker {
                /*@ normal_behavior
                  @ requires x > 0;
                  @ ensures \\result == 1;
                  @*/
                /*@ also
                  @ normal_behavior
                  @ requires x == 0;
                  @ ensures \\result == 0;
                  @*/
                /*@ also
                  @ normal_behavior
                  @ requires x < 0;
                  @ ensures \\result == -1;
                  @*/
                int signum(int x) { return Integer.signum(x); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Complete Behavioral Cases ====================
    @Test
    void testDivisionComplete() {
        String code = """
            public class Divider {
                /*@ normal_behavior
                  @ requires divisor != 0;
                  @ ensures dividend == \\result * divisor + (dividend % divisor);
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires divisor == 0;
                  @ signals (ArithmeticException e) divisor == 0;
                  @*/
                int divide(int dividend, int divisor) throws ArithmeticException {
                    return dividend / divisor;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testArrayAccessComplete() {
        String code = """
            public class ArrayWrapper {
                int[] data;
                
                /*@ normal_behavior
                  @ requires 0 <= index && index < data.length;
                  @ ensures \\result == data[index];
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires index < 0 || index >= data.length;
                  @ signals (IndexOutOfBoundsException e) 
                  @   index < 0 || index >= data.length;
                  @*/
                int get(int index) throws IndexOutOfBoundsException {
                    return data[index];
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testStackPushComplete() {
        String code = """
            public class BoundedStack {
                Object[] elements;
                int size;
                int capacity;
                
                /*@ normal_behavior
                  @ requires size < capacity && elem != null;
                  @ assignable elements[size], size;
                  @ ensures size == \\old(size) + 1;
                  @ ensures elements[\\old(size)] == elem;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires size >= capacity;
                  @ signals (StackOverflowException e) size >= capacity;
                  @*/
                void push(Object elem) throws StackOverflowException {}
            }
            class StackOverflowException extends Exception {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Also with Invariants ====================
    @Test
    void testAlsoWithClassInvariants() {
        String code = """
            public class BoundedCounter {
                int value;
                int limit;
                
                /*@ public invariant 0 <= value && value <= limit;
                  @ public invariant limit > 0;
                  @*/
                
                /*@ normal_behavior
                  @ requires value < limit;
                  @ assignable value;
                  @ ensures value == \\old(value) + 1;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires value >= limit;
                  @ signals (IllegalStateException e) value >= limit;
                  @*/
                void increment() throws IllegalStateException {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMethodWithAlsoAndEnsures() {
        String code = """
            public class Calculator {
                /*@ normal_behavior
                  @ requires a >= 0 && b >= 0;
                  @ ensures \\result >= a && \\result >= b;
                  @ ensures \\result == a || \\result == b;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires a < 0 || b < 0;
                  @ signals (IllegalArgumentException e) a < 0 || b < 0;
                  @*/
                int max(int a, int b) throws IllegalArgumentException {
                    return Math.max(a, b);
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}