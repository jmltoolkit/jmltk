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
 * Test suite for JML loop contracts.
 * Covers: JmlLoopInvariant, JmlDecreasingClause, JmlVariants.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlLoopContractsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Loop Invariants ====================
    @Test
    void testSimpleLoopInvariant() {
        String code = """
            public class Example {
                void sum(int[] arr) {
                    int sum = 0;
                    /*@ loop_invariant 0 <= i && i <= arr.length; @*/
                    for (int i = 0; i < arr.length; i++) {
                        sum += arr[i];
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testLoopInvariantMultiple() {
        String code = """
            public class Example {
                int factorial(int n) {
                    int result = 1;
                    int i = 1;
                    /*@ loop_invariant 1 <= i && i <= n + 1;
                      @ loop_invariant result == (\\product int j; 1 <= j && j < i; j);
                      @*/
                    while (i <= n) {
                        result *= i;
                        i++;
                    }
                    return result;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testLoopInvariantWithQuantifier() {
        String code = """
            public class Example {
                boolean allPositive(int[] arr) {
                    boolean result = true;
                    /*@ loop_invariant (\\forall int j; 0 <= j && j < i; arr[j] > 0); @*/
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i] <= 0) result = false;
                    }
                    return result;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testLoopInvariantAccumulator() {
        String code = """
            public class Example {
                int sum(int[] arr) {
                    int sum = 0;
                    /*@ loop_invariant sum == (\\sum int j; 0 <= j && j < i; arr[j]); @*/
                    for (int i = 0; i < arr.length; i++) {
                        sum += arr[i];
                    }
                    return sum;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Decreasing Clauses (Termination) ====================
    @Test
    void testDecreasingSimple() {
        String code = """
            public class Example {
                void countdown(int n) {
                    /*@ decreasing n - i; @*/
                    for (int i = 0; i < n; i++) {
                        // loop body
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testDecreasingWhile() {
        String code = """
            public class Example {
                int gcd(int a, int b) {
                    /*@ decreasing a + b; @*/
                    while (a != b) {
                        if (a > b) a = a - b;
                        else b = b - a;
                    }
                    return a;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testDecreasingArrayIndex() {
        String code = """
            public class Example {
                void reverse(int[] arr) {
                    int left = 0, right = arr.length - 1;
                    /*@ decreasing right - left; @*/
                    while (left < right) {
                        int temp = arr[left];
                        arr[left] = arr[right];
                        arr[right] = temp;
                        left++;
                        right--;
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Loop Invariant and Decreasing ====================
    @Test
    void testLoopInvariantAndDecreasing() {
        String code = """
            public class Example {
                int findMax(int[] arr) {
                    int max = arr[0];
                    /*@ loop_invariant 0 < i && i <= arr.length;
                      @ loop_invariant max == (\\max int j; 0 <= j && j < i; arr[j]);
                      @ decreasing arr.length - i;
                      @*/
                    for (int i = 1; i < arr.length; i++) {
                        if (arr[i] > max) max = arr[i];
                    }
                    return max;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testBinarySearchLoop() {
        String code = """
            public class Example {
                int binarySearch(int[] arr, int target) {
                    int low = 0, high = arr.length - 1;
                    /*@ loop_invariant 0 <= low && low <= high + 1 && high < arr.length;
                      @ loop_invariant (\\forall int j; 0 <= j && j < low; arr[j] < target);
                      @ loop_invariant (\\forall int j; high < j && j < arr.length; arr[j] > target);
                      @ decreasing high - low + 1;
                      @*/
                    while (low <= high) {
                        int mid = (low + high) / 2;
                        if (arr[mid] == target) return mid;
                        else if (arr[mid] < target) low = mid + 1;
                        else high = mid - 1;
                    }
                    return -1;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Do-While Loop Contracts ====================
    @Test
    void testDoWhileLoop() {
        String code = """
            public class Example {
                int readUntilZero() {
                    int sum = 0;
                    int x;
                    /*@ loop_invariant sum >= 0;
                      @ decreasing 1000 - sum;
                      @*/
                    do {
                        x = getValue();
                        sum += x;
                    } while (x != 0);
                    return sum;
                }
                int getValue() { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Nested Loop Contracts ====================
    @Test
    void testNestedLoops() {
        String code = """
            public class Matrix {
                int[][] multiply(int[][] A, int[][] B, int n) {
                    int[][] C = new int[n][n];
                    /*@ loop_invariant 0 <= i && i <= n; @*/
                    for (int i = 0; i < n; i++) {
                        /*@ loop_invariant 0 <= j && j <= n; @*/
                        for (int j = 0; j < n; j++) {
                            /*@ loop_invariant 0 <= k && k <= n; @*/
                            for (int k = 0; k < n; k++) {
                                C[i][j] += A[i][k] * B[k][j];
                            }
                        }
                    }
                    return C;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Loop Invariant with Old ====================
    @Test
    void testLoopInvariantWithOld() {
        String code = """
            public class Example {
                void incrementAll(int[] arr) {
                    /*@ loop_invariant (\\forall int j; 0 <= j && j < i; arr[j] == \\old(arr[j]) + 1); @*/
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = arr[i] + 1;
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Complete Method with Loop Contract ====================
    @Test
    void testCompleteMethodWithLoopContract() {
        String code = """
            public class Sorter {
                /*@ requires arr != null;
                  @ ensures (\\forall int i; 0 <= i && i < arr.length - 1; arr[i] <= arr[i+1]);
                  @*/
                void bubbleSort(int[] arr) {
                    int n = arr.length;
                    /*@ loop_invariant 0 <= sorted && sorted <= n;
                      @ loop_invariant (\\forall int i; sorted <= i && i < n; arr[i] >= arr[sorted]);
                      @ decreasing n - sorted;
                      @*/
                    for (int sorted = 0; sorted < n - 1; sorted++) {
                        /*@ loop_invariant 0 <= i && i < n - sorted - 1 || i == n - sorted - 1; @*/
                        for (int i = 0; i < n - sorted - 1; i++) {
                            if (arr[i] > arr[i + 1]) {
                                int temp = arr[i];
                                arr[i] = arr[i + 1];
                                arr[i + 1] = temp;
                            }
                        }
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}