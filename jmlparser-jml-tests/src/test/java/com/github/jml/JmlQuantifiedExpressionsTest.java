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
 * Test suite for JML quantified expressions.
 * Covers: JmlQuantifiedExpr, \forall, \exists, \num_of, \sum, \product, \min, \max.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlQuantifiedExpressionsTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Universal Quantification (\forall) ====================
    @Test
    void testForallSimple() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i; 0 <= i && i < arr.length; arr[i] >= 0); @*/
                void method(int[] arr) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testForallMultipleVariables() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i, j; 0 <= i && i < n && 0 <= j && j < n; m[i][j] == m[j][i]); @*/
                void symmetricMatrix(int[][] m, int n) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testForallNested() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i; 0 <= i && i < n; 
                  @   (\\forall int j; 0 <= j && j < n; matrix[i][j] >= 0));
                  @*/
                void allPositive(int[][] matrix, int n) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Existential Quantification (\exists) ====================
    @Test
    void testExistsSimple() {
        String code = """
            public class Example {
                /*@ ensures (\\exists int i; 0 <= i && i < arr.length; arr[i] == target); @*/
                boolean contains(int[] arr, int target) { return false; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testExistsMultipleVariables() {
        String code = """
            public class Example {
                /*@ ensures (\\exists int i, j; 0 <= i && i < rows && 0 <= j && j < cols; matrix[i][j] == value); @*/
                boolean findInMatrix(int[][] matrix, int rows, int cols, int value) { return false; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testExistsWithCondition() {
        String code = """
            public class Example {
                /*@ requires arr.length > 0;
                  @ ensures (\\exists int i; 0 <= i && i < arr.length; arr[i] == \\result);
                  @*/
                int pickElement(int[] arr) { return arr[0]; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Counting (\num_of) ====================
    @Test
    void testNumOfSimple() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\num_of int i; 0 <= i && i < arr.length; arr[i] == target); @*/
                int countOccurrences(int[] arr, int target) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testNumOfInRange() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\num_of int i; 0 <= i && i < arr.length; arr[i] >= min && arr[i] <= max); @*/
                int countInRange(int[] arr, int min, int max) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Summation (\sum) ====================
    @Test
    void testSumSimple() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\sum int i; 0 <= i && i < arr.length; arr[i]); @*/
                int sum(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSumWithExpression() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\sum int i; 0 <= i && i < n; i * i); @*/
                int sumOfSquares(int n) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSumDoubleArray() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\sum int i; 0 <= i && i < arr.length; arr[i] * arr[i]); @*/
                int sumOfSquares(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Product (\product) ====================
    @Test
    void testProductSimple() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\product int i; 0 <= i && i < arr.length; arr[i]); @*/
                long product(int[] arr) { return 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testProductFactorial() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\product int i; 1 <= i && i <= n; i); @*/
                long factorial(int n) { return 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Minimum (\min) ====================
    @Test
    void testMinSimple() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\min int i; 0 <= i && i < arr.length; arr[i]); @*/
                int minimum(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMinWithRange() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\min int i; low <= i && i <= high; arr[i]); @*/
                int minInRange(int[] arr, int low, int high) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Maximum (\max) ====================
    @Test
    void testMaxSimple() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\max int i; 0 <= i && i < arr.length; arr[i]); @*/
                int maximum(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testMaxWithExpression() {
        String code = """
            public class Example {
                /*@ ensures \\result == (\\max int i; 0 <= i && i < arr.length; Math.abs(arr[i])); @*/
                int maxAbs(int[] arr) { return 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined Quantifiers ====================
    @Test
    void testForallAndExists() {
        String code = """
            public class Example {
                /*@ ensures (\\forall int i; 0 <= i && i < arr.length; arr[i] >= 0) ==>
                  @           (\\exists int j; 0 <= j && j < arr.length; arr[j] == 0);
                  @*/
                void method(int[] arr) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testSumAndCount() {
        String code = """
            public class Example {
                /*@ ensures average == (\\sum int i; 0 <= i && i < arr.length; arr[i]) / 
                  @                      (\\num_of int i; 0 <= i && i < arr.length; true);
                  @*/
                void computeAverage(int[] arr, double average) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testQuantifierInRequires() {
        String code = """
            public class Example {
                /*@ requires (\\forall int i; 0 <= i && i < arr.length; arr[i] > 0);
                  @ ensures \\result > 0;
                  @*/
                int productPositive(int[] arr) { return 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testQuantifierInvariant() {
        String code = """
            public class SortedArray {
                int[] data;
                int size;
                
                /*@ public invariant (\\forall int i, j; 
                  @   0 <= i && i < j && j < size; data[i] <= data[j]);
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}