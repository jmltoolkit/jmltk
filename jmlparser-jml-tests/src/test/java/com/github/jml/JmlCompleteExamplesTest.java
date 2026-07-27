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
 * Test suite with complete JML examples combining multiple features.
 * Covers: Full specifications for common data structures and algorithms.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlCompleteExamplesTest {
    private final JavaParser javaParser = new JavaParser();

    // ==================== Bounded Buffer Example ====================
    @Test
    void testBoundedBuffer() {
        String code = """
            public class BoundedBuffer<T> {
                private Object[] elements;
                private int head;
                private int tail;
                private int count;
                
                /*@ public invariant 0 <= head && head < elements.length;
                  @ public invariant 0 <= tail && tail < elements.length;
                  @ public invariant 0 <= count && count <= elements.length;
                  @ public invariant count == (tail >= head ? tail - head : elements.length - head + tail);
                  @*/
                
                /*@ represents contents = elements[head..count]; @*/
                
                /*@ ensures elements != null && elements.length == capacity;
                  @ ensures head == 0 && tail == 0 && count == 0;
                  @*/
                BoundedBuffer(int capacity) {
                    elements = new Object[capacity];
                    head = 0;
                    tail = 0;
                    count = 0;
                }
                
                /*@ normal_behavior
                  @ requires count < elements.length && elem != null;
                  @ assignable elements[tail], tail, count;
                  @ ensures count == \\old(count) + 1;
                  @ ensures elements[\\old(tail)] == elem;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires count >= elements.length;
                  @ signals (IllegalStateException e) count >= elements.length;
                  @*/
                void put(T elem) throws IllegalStateException {
                    if (count >= elements.length) throw new IllegalStateException();
                    elements[tail] = elem;
                    tail = (tail + 1) % elements.length;
                    count++;
                }
                
                /*@ normal_behavior
                  @ requires count > 0;
                  @ assignable count, head;
                  @ ensures \\result == \\old(elements[head]);
                  @ ensures count == \\old(count) - 1;
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires count == 0;
                  @ signals (IllegalStateException e) count == 0;
                  @*/
                T take() throws IllegalStateException {
                    if (count == 0) throw new IllegalStateException();
                    T result = (T) elements[head];
                    head = (head + 1) % elements.length;
                    count--;
                    return result;
                }
                
                /*@ ensures \\result == count; @*/
                int size() { return count; }
                
                /*@ ensures \\result == (count == 0); @*/
                boolean isEmpty() { return count == 0; }
                
                /*@ ensures \\result == (count == elements.length); @*/
                boolean isFull() { return count == elements.length; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Sorted List Example ====================
    @Test
    void testSortedList() {
        String code = """
            public class SortedList {
                private int[] elements;
                private int size;
                
                /*@ public invariant 0 <= size && size <= elements.length;
                  @ public invariant (\\forall int i; 0 <= i && i < size - 1; elements[i] <= elements[i+1]);
                  @*/
                
                /*@ represents contents = elements[0..size]; @*/
                
                /*@ ensures size == 0; @*/
                SortedList() {
                    elements = new int[10];
                    size = 0;
                }
                
                /*@ normal_behavior
                  @ assignable elements[*], size;
                  @ ensures size == \\old(size) + 1;
                  @ ensures (\\forall int i; 0 <= i && i < size - 1; elements[i] <= elements[i+1]);
                  @*/
                void add(int value) {
                    if (size == elements.length) {
                        int[] newElements = new int[elements.length * 2];
                        System.arraycopy(elements, 0, newElements, 0, size);
                        elements = newElements;
                    }
                    int i = size - 1;
                    while (i >= 0 && elements[i] > value) {
                        elements[i + 1] = elements[i];
                        i--;
                    }
                    elements[i + 1] = value;
                    size++;
                }
                
                /*@ normal_behavior
                  @ requires 0 <= index && index < size;
                  @ ensures \\result == elements[index];
                  @*/
                /*@ also
                  @ exceptional_behavior
                  @ requires index < 0 || index >= size;
                  @ signals (IndexOutOfBoundsException e) index < 0 || index >= size;
                  @*/
                int get(int index) throws IndexOutOfBoundsException {
                    if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
                    return elements[index];
                }
                
                /*@ ensures \\result == size; @*/
                int size() { return size; }
                
                /*@ ensures \\result == (size == 0); @*/
                boolean isEmpty() { return size == 0; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Binary Search Tree Example ====================
    @Test
    void testBinarySearchTree() {
        String code = """
            public class BinarySearchTree {
                static class Node {
                    int key;
                    Node left, right;
                    Node(int k) { key = k; }
                }
                
                private Node root;
                
                /*@ public invariant (root == null) <==> (size() == 0);
                  @ public invariant (\\forall Node n; n != null; 
                  @   (n.left == null || n.left.key <= n.key) &&
                  @   (n.right == null || n.right.key > n.key));
                  @*/
                
                /*@ ensures \\result == (\\exists Node n; n != null && reachable(root, n); n.key == key); @*/
                boolean contains(int key) {
                    Node current = root;
                    while (current != null) {
                        if (key == current.key) return true;
                        else if (key < current.key) current = current.left;
                        else current = current.right;
                    }
                    return false;
                }
                
                /*@ assignable root;
                  @ ensures (\\exists Node n; n != null && reachable(\\new(root), n); n.key == key);
                  @*/
                void insert(int key) {
                    root = insertRec(root, key);
                }
                
                private Node insertRec(Node node, int key) {
                    if (node == null) return new Node(key);
                    if (key < node.key) node.left = insertRec(node.left, key);
                    else if (key > node.key) node.right = insertRec(node.right, key);
                    return node;
                }
                
                /*@ model @*/ int size() { return 0; }
                /*@ model @*/ boolean reachable(Node from, Node to) { return true; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Counter with History Example ====================
    @Test
    void testCounterWithHistory() {
        String code = """
            public class CounterWithHistory {
                private int value;
                /*@ ghost @*/ int[] history;
                /*@ ghost @*/ int historySize;
                
                /*@ public invariant value >= 0;
                  @ public invariant historySize >= 0;
                  @ public invariant (\\forall int i; 0 <= i && i < historySize; history[i] >= 0);
                  @*/
                
                /*@ represents currentValue = value;
                  @ represents allValues = history[0..historySize];
                  @*/
                
                /*@ ensures value == 0;
                  @ ensures historySize == 1;
                  @ ensures history[0] == 0;
                  @*/
                CounterWithHistory() {
                    value = 0;
                    history = new int[100];
                    history[0] = 0;
                    historySize = 1;
                }
                
                /*@ normal_behavior
                  @ requires n > 0;
                  @ assignable value, history[historySize], historySize;
                  @ ensures value == \\old(value) + n;
                  @ ensures history[\\old(historySize)] == value;
                  @ ensures historySize == \\old(historySize) + 1;
                  @*/
                void increment(int n) {
                    value += n;
                    history[historySize] = value;
                    historySize++;
                }
                
                /*@ pure
                  @ ensures \\result == value;
                  @*/
                int getValue() { return value; }
                
                /*@ pure
                  @ ensures \\result == historySize;
                  @*/
                /*@ ghost @*/ int getHistorySize() { return historySize; }
                
                /*@ pure
                  @ requires 0 <= index && index < historySize;
                  @ ensures \\result == history[index];
                  @*/
                /*@ ghost @*/ int getHistoryAt(int index) { return history[index]; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Immutable Pair Example ====================
    @Test
    void testImmutablePair() {
        String code = """
            public class ImmutablePair<T, U> {
                private final T first;
                private final U second;
                
                /*@ public invariant first != null;
                  @ public invariant second != null;
                  @*/
                
                /*@ ensures this.first == f && this.second == s; @*/
                ImmutablePair(T f, U s) {
                    first = f;
                    second = s;
                }
                
                /*@ pure
                  @ ensures \\result == first;
                  @*/
                T getFirst() { return first; }
                
                /*@ pure
                  @ ensures \\result == second;
                  @*/
                U getSecond() { return second; }
                
                /*@ pure
                  @ ensures \\result == (o instanceof ImmutablePair);
                  @ ensures o instanceof ImmutablePair ==> 
                  @   ((ImmutablePair<?,?>)o).first.equals(first) &&
                  @   ((ImmutablePair<?,?>)o).second.equals(second);
                  @*/
                public boolean equals(Object o) {
                    if (!(o instanceof ImmutablePair)) return false;
                    ImmutablePair<?,?> other = (ImmutablePair<?,?>) o;
                    return first.equals(other.first) && second.equals(other.second);
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Array Utilities Example ====================
    @Test
    void testArrayUtilities() {
        String code = """
            public class ArrayUtils {
                /*@ pure
                  @ requires arr != null;
                  @ ensures \\result == (\\sum int i; 0 <= i && i < arr.length; arr[i]);
                  @*/
                public static int sum(int[] arr) {
                    int sum = 0;
                    /*@ loop_invariant 0 <= i && i <= arr.length;
                      @ loop_invariant sum == (\\sum int j; 0 <= j && j < i; arr[j]);
                      @ decreasing arr.length - i;
                      @*/
                    for (int i = 0; i < arr.length; i++) {
                        sum += arr[i];
                    }
                    return sum;
                }
                
                /*@ pure
                  @ requires arr != null;
                  @ ensures \\result == (\\max int i; 0 <= i && i < arr.length; arr[i]);
                  @*/
                public static int max(int[] arr) {
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
                
                /*@ pure
                  @ requires arr != null;
                  @ ensures (\\forall int i; 0 <= i && i < arr.length; arr[i] == \\old(arr[arr.length - 1 - i]);
                  @*/
                public static void reverse(int[] arr) {
                    int left = 0, right = arr.length - 1;
                    /*@ loop_invariant 0 <= left && right < arr.length && left + right == arr.length - 1;
                      @ decreasing right - left;
                      @*/
                    while (left < right) {
                        int temp = arr[left];
                        arr[left] = arr[right];
                        arr[right] = temp;
                        left++;
                        right--;
                    }
                }
                
                /*@ pure
                  @ requires arr1 != null && arr2 != null;
                  @ ensures \\result <==> (arr1.length == arr2.length &&
                  @   (\\forall int i; 0 <= i && i < arr1.length; arr1[i] == arr2[i]));
                  @*/
                public static boolean equals(int[] arr1, int[] arr2) {
                    if (arr1.length != arr2.length) return false;
                    /*@ loop_invariant 0 <= i && i <= arr1.length;
                      @ loop_invariant (\\forall int j; 0 <= j && j < i; arr1[j] == arr2[j]);
                      @ decreasing arr1.length - i;
                      @*/
                    for (int i = 0; i < arr1.length; i++) {
                        if (arr1[i] != arr2[i]) return false;
                    }
                    return true;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Reader-Writer Lock Example ====================
    @Test
    void testReaderWriterLock() {
        String code = """
            public class ReaderWriterLock {
                private int readers;
                private boolean writer;
                
                /*@ public invariant readers >= 0;
                  @ public invariant !writer || readers == 0;
                  @*/
                
                /*@ initially readers == 0 && !writer; @*/
                
                /*@ normal_behavior
                  @ requires !writer;
                  @ assignable readers;
                  @ ensures readers == \\old(readers) + 1;
                  @*/
                void readLock() {
                    readers++;
                }
                
                /*@ normal_behavior
                  @ assignable readers;
                  @ ensures readers == \\old(readers) - 1;
                  @*/
                void readUnlock() {
                    readers--;
                }
                
                /*@ normal_behavior
                  @ requires readers == 0 && !writer;
                  @ assignable writer;
                  @ ensures writer;
                  @*/
                void writeLock() {
                    writer = true;
                }
                
                /*@ normal_behavior
                  @ assignable writer;
                  @ ensures !writer;
                  @*/
                void writeUnlock() {
                    writer = false;
                }
                
                /*@ pure ensures \\result == readers; @*/
                int getReaders() { return readers; }
                
                /*@ pure ensures \\result == writer; @*/
                boolean hasWriter() { return writer; }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Fibonacci with Termination ====================
    @Test
    void testFibonacciWithTermination() {
        String code = """
            public class Fibonacci {
                /*@ pure
                  @ requires n >= 0;
                  @ ensures \\result == fib(n);
                  @ terminates;
                  @ measured_by n;
                  @*/
                int fibonacci(int n) {
                    if (n <= 1) return n;
                    return fibonacci(n - 1) + fibonacci(n - 2);
                }
                
                /*@ model pure @*/ int fib(int n) {
                    if (n <= 1) return n;
                    return fib(n - 1) + fib(n - 2);
                }
                
                /*@ pure
                  @ requires n >= 0;
                  @ ensures \\result == fib(n);
                  @ terminates;
                  @ measured_by n - i;
                  @*/
                int fibonacciIterative(int n) {
                    if (n <= 1) return n;
                    int a = 0, b = 1;
                    /*@ loop_invariant 0 < i && i <= n;
                      @ loop_invariant a == fib(i - 1) && b == fib(i);
                      @ decreasing n - i;
                      @*/
                    for (int i = 1; i < n; i++) {
                        int temp = a + b;
                        a = b;
                        b = temp;
                    }
                    return b;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}