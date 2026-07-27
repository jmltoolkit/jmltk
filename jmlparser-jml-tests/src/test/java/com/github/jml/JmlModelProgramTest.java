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
 * Test suite for JML model programs.
 * Covers: JmlModelProgramBlock, model program declarations.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlModelProgramTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Simple Model Programs ====================
    @Test
    void testSimpleModelProgram() {
        String code = """
            public class Example {
                /*@ model_program {
                  @   int x = 0;
                  @   while (x < 10) {
                  @     x = x + 1;
                  @   }
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelProgramWithGhostVariables() {
        String code = """
            public class Counter {
                /*@ model_program {
                  @   /*@ ghost @*/ int count = 0;
                  @   while (count < 100) {
                  @     count = count + 1;
                  @   }
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testModelProgramWithInvariants() {
        String code = """
            public class BoundedCounter {
                int limit;
                
                /*@ model_program {
                  @   int current = 0;
                  @   /*@ loop_invariant 0 <= current && current <= limit; @*/
                  @   while (current < limit) {
                  @     current = current + 1;
                  @   }
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Methods ====================
    @Test
    void testModelProgramWithHelperMethods() {
        String code = """
            public class Stack {
                /*@ model_program {
                  @   push(1);
                  @   push(2);
                  @   push(3);
                  @   pop();
                  @ }
                  
                  @ model void push(int x) {}
                  @ model int pop() { return 0; }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Specifications ====================
    @Test
    void testModelProgramWithSpec() {
        String code = """
            public class Example {
                /*@ model_program
                  @ requires initial >= 0;
                  @ ensures result >= initial;
                  @ {
                  @   int result = initialValue;
                  @   while (result < target) {
                  @     result = result + 1;
                  @   }
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Multiple Model Programs ====================
    @Test
    void testMultipleModelPrograms() {
        String code = """
            public class MultiProgram {
                /*@ model_program {
                  @   int x = 0;
                  @   x = x + 1;
                  @ }
                  @*/
                
                /*@ model_program {
                  @   int y = 10;
                  @   y = y - 1;
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Quantifiers ====================
    @Test
    void testModelProgramWithQuantifier() {
        String code = """
            public class ArrayInit {
                /*@ model_program {
                  @   int[] arr = new int[10];
                  @   /*@ assert (\\forall int i; 0 <= i && i < arr.length; arr[i] == 0); @*/
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program for Abstract Data Types ====================
    @Test
    void testModelProgramForSet() {
        String code = """
            public class AbstractSet {
                /*@ model_program {
                  @   Set s = emptySet();
                  @   s = add(s, 1);
                  @   s = add(s, 2);
                  @   /*@ assert contains(s, 1) && contains(s, 2); @*/
                  @ }
                  @*/
            }
            
            interface Set {}
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Loop Contracts ====================
    @Test
    void testModelProgramWithLoopContract() {
        String code = """
            public class Summation {
                /*@ model_program {
                  @   int sum = 0;
                  @   int i = 0;
                  @   /*@ loop_invariant sum == (\\sum int j; 0 <= j && j < i; j);
                  @     @ decreasing 10 - i;
                  @     @*/
                  @   while (i < 10) {
                  @     sum = sum + i;
                  @     i = i + 1;
                  @   }
                  @   /*@ assert sum == 45; @*/
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Exception Handling ====================
    @Test
    void testModelProgramWithException() {
        String code = """
            public class SafeDivision {
                /*@ model_program {
                  @   try {
                  @     int result = divide(10, 2);
                  @     /*@ assert result == 5; @*/
                  @   } catch (ArithmeticException e) {
                  @     /*@ assert false; @*/
                  @   }
                  @ }
                  @*/
                
                model int divide(int a, int b) throws ArithmeticException { return a / b; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Named Model Programs ====================
    @Test
    void testNamedModelProgram() {
        String code = """
            public class NamedPrograms {
                /*@ model_program initialization {
                  @   int state = 0;
                  @ }
                  @*/
                
                /*@ model_program transition {
                  @   int state = 1;
                  @   state = state + 1;
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Model Program with Conditional Logic ====================
    @Test
    void testModelProgramWithConditionals() {
        String code = """
            public class ConditionalProgram {
                /*@ model_program {
                  @   int x = 5;
                  @   if (x > 0) {
                  @     x = x * 2;
                  @   } else {
                  @     x = 0;
                  @   }
                  @   /*@ assert x == 10; @*/
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Complete Model Program Example ====================
    @Test
    void testCompleteModelProgramExample() {
        String code = """
            public class QueueModel {
                /*@ model @*/ int head;
                /*@ model @*/ int tail;
                /*@ model @*/ int capacity;
                
                /*@ model_program queueOperations {
                  @   head = 0;
                  @   tail = 0;
                  @   
                  @   // Enqueue operations
                  @   while (tail < capacity) {
                  @     tail = tail + 1;
                  @   }
                  @   
                  @   /*@ loop_invariant head <= tail; @*/
                  @   // Dequeue operations
                  @   while (head < tail) {
                  @     head = head + 1;
                  @   }
                  @   
                  @   /*@ assert head == tail && head == capacity; @*/
                  @ }
                  @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}