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
 * Test suite for JML invariants.
 * Covers: class invariants, initially clauses.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlInvariantsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Simple Invariants ====================
    @Test
    void testSimpleInvariant() {
        String code = """
            public class Example {
                int x;
                /*@ public invariant x >= 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testPrivateInvariant() {
        String code = """
            public class Example {
                int internalState;
                /*@ private invariant internalState >= 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testProtectedInvariant() {
        String code = """
            public class Example {
                int value;
                /*@ protected invariant value != Integer.MIN_VALUE; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testPackageInvariant() {
        String code = """
            public class Example {
                int data;
                /*@ package invariant data >= 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Invariants with Quantifiers ====================
    @Test
    void testInvariantWithForall() {
        String code = """
            public class ArrayWrapper {
                int[] elements;
                /*@ public invariant (\\forall int i; 0 <= i && i < elements.length; elements[i] >= 0); @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInvariantNonNull() {
        String code = """
            public class Container {
                Object data;
                /*@ public invariant data != null; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInvariantArrayNotNull() {
        String code = """
            public class Example {
                int[] array;
                /*@ public invariant array != null; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Multiple Invariants ====================
    @Test
    void testMultipleInvariants() {
        String code = """
            public class Counter {
                int count;
                int limit;
                /*@ public invariant count >= 0;
                  @ public invariant count <= limit;
                  @ public invariant limit > 0;
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInvariantCombined() {
        String code = """
            public class Range {
                int lower, upper;
                /*@ public invariant lower <= upper && lower >= 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Initially Clauses ====================
    @Test
    void testSimpleInitially() {
        String code = """
            public class Example {
                int counter;
                /*@ public initially counter == 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInitiallyMultipleFields() {
        String code = """
            public class Point {
                int x, y;
                /*@ public initially x == 0 && y == 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInitiallyWithArray() {
        String code = """
            public class Example {
                int[] data;
                int size;
                /*@ public initially data.length == size; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Invariant with Named Clause ====================
    @Test
    void testNamedInvariant() {
        String code = """
            public class Example {
                int value;
                /*@ public invariant nonNegative: value >= 0; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMultipleNamedInvariants() {
        String code = """
            public class BoundedCounter {
                int count;
                int max;
                /*@ public invariant nonNegative: count >= 0;
                  @ public invariant bounded: count <= max;
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Invariant with Model Fields ====================
    @Test
    void testInvariantWithModelField() {
        String code = """
            public class Stack {
                private Object[] elements;
                private int size;
                /*@ model @*/ int abstractSize;
                
                /*@ public invariant abstractSize == size; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInvariantRepresents() {
        String code = """
            public class Set {
                private Object[] elements;
                private int size;
                
                /*@ represents contents = elements[0..size]; @*/
                /*@ public invariant size >= 0 && size <= elements.length; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Complete Class with Invariants ====================
    @Test
    void testCompleteClassWithInvariants() {
        String code = """
            public class BankAccount {
                private double balance;
                private boolean active;
                
                /*@ public invariant balance >= 0;
                  @ public invariant active ==> balance >= 0;
                  @ public initially balance == 0 && !active;
                  @*/
                
                /*@ requires amount > 0;
                  @ assignable balance;
                  @ ensures balance == \\old(balance) + amount;
                  @*/
                void deposit(double amount) { balance += amount; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testInvariantWithTypeExpression() {
        String code = """
            public class Example {
                Object[] data;
                /*@ public invariant \\typeof(data) == \\type(Object[]); @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}