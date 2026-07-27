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
 * Test suite for JML special expressions.
 * Covers: \old, \result, \typeof, \type, \not_assigned, \fresh,
 *         array ranges, sequence operations, model casts.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlSpecialExpressionsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== \old Expression ====================
    @Test
    void testOldSimple() {
        String code = """
            public class Example {
                int x;
                /*@ ensures x == \\old(x) + 1; @*/
                void increment() { x++; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testOldInRequires() {
        String code = """
            public class Counter {
                int value;
                /*@ requires value > \\old(value); @*/
                void checkIncrease() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testOldWithFieldAccess() {
        String code = """
            public class Point {
                int x, y;
                /*@ ensures x == \\old(y) && y == \\old(x); @*/
                void swap() { int t = x; x = y; y = t; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testOldWithArrayAccess() {
        String code = """
            public class Example {
                int[] data;
                /*@ ensures data[0] == \\old(data[1]); @*/
                void swapFirstTwo() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== \result Expression ====================
    @Test
    void testResultSimple() {
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
    void testResultEquals() {
        String code = """
            public class Example {
                /*@ ensures \\result == x * y; @*/
                int multiply(int x, int y) { return x * y; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testResultWithOld() {
        String code = """
            public class Counter {
                int count;
                /*@ ensures \\result == \\old(count); @*/
                int getCount() { return count; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testResultArray() {
        String code = """
            public class Example {
                /*@ ensures \\result.length == arr.length + 1; @*/
                int[] addElement(int[] arr) { return arr; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== \fresh Expression ====================
    @Test
    void testFreshSimple() {
        String code = """
            public class Example {
                /*@ ensures \\fresh(\\result); @*/
                Object createNew() { return new Object(); }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testFreshArray() {
        String code = """
            public class Example {
                /*@ ensures \\fresh(\\result); @*/
                int[] newArray(int size) { return new int[size]; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testFreshWithOld() {
        String code = """
            public class Factory {
                Object cached;
                /*@ ensures \\result != null && (\\fresh(\\result) || \\result == \\old(cached)); @*/
                Object getInstance() { return cached; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== \typeof and \type Expressions ====================
    @Test
    void testTypeof() {
        String code = """
            public class Example {
                Object obj;
                /*@ ensures \\typeof(obj) == \\type(String); @*/
                void setString(String s) { obj = s; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testTypeExpression() {
        String code = """
            public class Example {
                /*@ invariant \\typeof(this) == \\type(Example); @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Array Range Expressions ====================
    @Test
    void testArrayRange() {
        String code = """
            public class Example {
                int[] data;
                /*@ represents subarray = data[low..high]; @*/
                void processRange(int low, int high) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testArrayRangeOpenEnd() {
        String code = """
            public class Example {
                int[] data;
                /*@ ensures data[0..] != null; @*/
                void method() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testArraySegment() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i; 0 <= i && i < arr[0..n].length; arr[0..n][i] >= 0); @*/
                void allPositive(int[] arr, int n) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Sequence Operations ====================
    @Test
    void testSequenceConcat() {
        String code = """
            public class Example {
                /*@ ensures \\result == seq1.concat(seq2); @*/
                int[] concatenate(int[] seq1, int[] seq2) { return seq1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSequenceItem() {
        String code = """
            public class Example {
                /*@ ensures s.item(i) == arr[i]; @*/
                void access(int[] arr, int i) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSequenceUpdate() {
        String code = """
            public class Example {
                /*@ ensures \\result == s.update(i, v); @*/
                int[] updateElement(int[] s, int i, int v) { return s; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSequenceSize() {
        String code = """
            public class Example {
                /*@ ensures s.size() == arr.length; @*/
                void checkSize(int[] arr) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== \not_assigned Expression ====================
    @Test
    void testNotAssigned() {
        String code = """
            public class Example {
                int x, y;
                /*@ ensures \\not_assigned(x, y); @*/
                void readOnly() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNotAssignedArray() {
        String code = """
            public class Example {
                int[] data;
                /*@ ensures \\not_assigned(data[*]); @*/
                void dontModify() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Casts ====================
    @Test
    void testModelCast() {
        String code = """
            public class Example {
                Object obj;
                /*@ ensures ((String)obj).length() > 0; @*/
                void processString() {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Special Expressions ====================
    @Test
    void testOldAndResult() {
        String code = """
            public class Counter {
                int count;
                /*@ ensures \\result == \\old(count) + 1 && count == \\old(count) + 1; @*/
                int incrementAndGet() { count++; return count; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testComplexPostcondition() {
        String code = """
            public class Stack {
                Object[] elements;
                int size;
                
                /*@ requires size > 0;
                  @ ensures \\result == \\old(elements[size-1]);
                  @ ensures size == \\old(size) - 1;
                  @ ensures \\fresh(\\result) == false;
                  @*/
                Object pop() { return null; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}