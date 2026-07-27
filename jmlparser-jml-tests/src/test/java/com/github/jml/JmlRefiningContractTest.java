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
 * Test suite for JML contract refining.
 * Covers: also, refining, method overriding contracts.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlRefiningContractTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Method Overriding with Contracts ====================
    @Test
    void testOverrideWithWeakerRequires() {
        String code = """
            public class Parent {
                /*@ requires x > 0;
                  @ ensures \\result >= 0;
                  @*/
                int method(int x) { return x; }
            }
            
            class Child extends Parent {
                /*@ requires x >= 0;
                  @ ensures \\result >= 0;
                  @*/
                @Override
                int method(int x) { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testOverrideWithStrongerEnsures() {
        String code = """
            public class Parent {
                /*@ ensures \\result >= 0; @*/
                int getValue() { return 0; }
            }
            
            class Child extends Parent {
                /*@ ensures \\result > 0; @*/
                @Override
                int getValue() { return 1; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testOverrideWithSameAssignable() {
        String code = """
            public class Parent {
                int value;
                /*@ assignable value; @*/
                void modify() { value++; }
            }
            
            class Child extends Parent {
                /*@ assignable value; @*/
                @Override
                void modify() { value += 2; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Refining with Also ====================
    @Test
    void testRefiningWithAlso() {
        String code = """
            public class Base {
                /*@ normal_behavior
                  @ requires x > 0;
                  @ ensures \\result == x;
                  @*/
                int process(int x) { return x; }
            }
            
            class Derived extends Base {
                /*@ also
                  @ normal_behavior
                  @ requires x == 0;
                  @ ensures \\result == 0;
                  @*/
                @Override
                int process(int x) { return x; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Abstract Method Implementation ====================
    @Test
    void testAbstractMethodImplementation() {
        String code = """
            public abstract class AbstractClass {
                /*@ requires x >= 0;
                  @ ensures \\result >= 0;
                  @*/
                abstract int compute(int x);
            }
            
            class ConcreteClass extends AbstractClass {
                /*@ ensures \\result == x * 2; @*/
                @Override
                int compute(int x) { return x * 2; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Interface Implementation ====================
    @Test
    void testInterfaceImplementation() {
        String code = """
            public interface Calculator {
                /*@ requires a >= 0 && b >= 0;
                  @ ensures \\result >= 0;
                  @*/
                int add(int a, int b);
            }
            
            class CalculatorImpl implements Calculator {
                /*@ ensures \\result == a + b; @*/
                @Override
                public int add(int a, int b) { return a + b; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Strengthening Invariants in Subclass ====================
    @Test
    void testStrongerInvariantInSubclass() {
        String code = """
            public class Parent {
                int value;
                /*@ public invariant value >= 0; @*/
            }
            
            class Child extends Parent {
                /*@ public invariant value <= 100; @*/
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Multiple Levels of Inheritance ====================
    @Test
    void testMultipleLevelsOfInheritance() {
        String code = """
            public class Base {
                /*@ requires x > 0; @*/
                void method(int x) {}
            }
            
            class Middle extends Base {
                /*@ requires x >= 0; @*/
                @Override
                void method(int x) {}
            }
            
            class Leaf extends Middle {
                /*@ requires x >= -1; @*/
                @Override
                void method(int x) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Covariant Return Types ====================
    @Test
    void testCovariantReturnTypes() {
        String code = """
            public class Parent {
                /*@ ensures \\result != null; @*/
                Object create() { return new Object(); }
            }
            
            class Child extends Parent {
                /*@ ensures \\result != null; @*/
                @Override
                String create() { return "hello"; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Adding Signals in Override ====================
    @Test
    void testAddingSignalsInOverride() {
        String code = """
            public class Parent {
                /*@ signals (Exception e) false; @*/
                void riskyMethod() throws Exception {}
            }
            
            class Child extends Parent {
                /*@ signals (RuntimeException e) true;
                  @ signals (Exception e) false;
                  @*/
                @Override
                void riskyMethod() throws RuntimeException, Exception {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Refining with Different Visibility ====================
    @Test
    void testRefiningVisibility() {
        String code = """
            public class Parent {
                /*@ protected @*/
                /*@ requires x > 0; @*/
                void protectedMethod(int x) {}
            }
            
            class Child extends Parent {
                /*@ public @*/
                /*@ requires x >= 0; @*/
                @Override
                void protectedMethod(int x) {}
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Complete Refinement Example ====================
    @Test
    void testCompleteRefinementExample() {
        String code = """
            public interface Container {
                /*@ requires elem != null;
                  @ ensures size == \\old(size) + 1;
                  @*/
                void add(Object elem);
                
                /*@ requires !isEmpty();
                  @ ensures \\result != null;
                  @*/
                Object remove();
                
                /*@ ensures \\result >= 0; @*/
                boolean isEmpty();
            }
            
            class BoundedContainer implements Container {
                private Object[] data;
                private int size;
                private int capacity;
                
                /*@ public invariant 0 <= size && size <= capacity;
                  @ public invariant capacity > 0;
                  @*/
                
                /*@ requires elem != null && size < capacity;
                  @ assignable data[size], size;
                  @ ensures size == \\old(size) + 1;
                  @ ensures data[\\old(size)] == elem;
                  @*/
                @Override
                public void add(Object elem) { data[size++] = elem; }
                
                /*@ requires size > 0;
                  @ assignable size;
                  @ ensures \\result == \\old(data[size-1]);
                  @ ensures size == \\old(size) - 1;
                  @*/
                @Override
                public Object remove() { return data[--size]; }
                
                /*@ ensures \\result == (size == 0); @*/
                @Override
                public boolean isEmpty() { return size == 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}