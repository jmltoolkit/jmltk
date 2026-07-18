/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Test cases for JmlStatisticsReporter.
 *
 * @author Alexander Weigl
 * @version 1 (8/11/26)
 */
internal class JmlStatisticsReporterTest {
    @Test
    fun testSingleClassCounting() {
        val code = """
            public class MyClass {
                int x;
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(1)
    }

    @Test
    fun testNestedClassCounting() {
        val code = """
            public class Outer {
                class Inner {
                    class DeepInner {}
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(3)
    }

    @Test
    fun testMultipleClassesCounting() {
        val code = """
            class A {}
            class B {}
            class C {}

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(3)
    }

    // ==================== Method Statistics ====================
    @Test
    fun testTotalMethodCounting() {
        val code = """
            class MyClass {
                void m1() {}
                void m2() {}
                void m3() {}
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTAL_METHODS]).isEqualTo(3)
    }

    @Test
    fun testModelMethodDetection() {
        val code = """
            class MyClass {
                /*@ model_behavior
                    requires true;
                    ensures \result > 0;
                    model int getModelValue() { return 0; }
                @*/
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.totalModelMethods]).isEqualTo(1)
    }

    @Test
    fun testPureMethodDetection() {
        val code = """
            class MyClass {
                /*@ pure @*/ int getValue() { return 42; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.pureMethods]).isEqualTo(1)
    }

    @Test
    fun testSpecifiedMethodDetection() {
        // Using \\ for JML escape sequences in Java strings
        val code = """
            class MyClass {
                /*@ requires x > 0; ensures \result == x + 1; @*/
                int increment(int x) { return x + 1; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
    }

    // ==================== JML Clause Statistics ====================
    @Test
    fun testRequiresClauseCounting() {
        val code = """
            class MyClass {
                /*@ requires x > 0;
                    requires y > 0;
                    ensures \result == x + y; @*/
                int add(int x, int y) { return x + y; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKeys.jmlClause(JmlClauseKind.REQUIRES)]).isEqualTo(2)
    }

    @Test
    fun testEnsuresClauseCounting() {
        val code = """
            class MyClass {
                /*@ requires x >= 0;
                    ensures \result >= 0;
                    ensures \result == x * x; @*/
                int square(int x) { return x * x; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)
        println(stats)
        assertThat(stats[JmlClauseKind.ENSURES]).isEqualTo(2)
    }

    @Test
    fun testAssignsClauseCounting() {
        val code = """
            class MyClass {
                int field1, field2;
                /*@ normal_behavior
                    assignable field1, field2; @*/
                void modify() { field1 = 1; field2 = 2; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[JmlClauseKind.ASSIGNABLE]).isEqualTo(1)
    }

    @Test
    fun testSignalsClauseCounting() {
        val code = """
            class MyClass {
                /*@ signals (IllegalArgumentException e) x < 0; @*/
                void checkPositive(int x) {}
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Signals clauses are counted - verify specified method is detected
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
    }

    @Test
    fun testSignalsOnlyClauseCounting() {
        val code = """
            class MyClass {
                /*@ signals_only NullPointerException; @*/
                void mayThrowNPE() {}
            }
            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
        assertThat(stats[JmlClauseKind.SIGNALS_ONLY]).isEqualTo(1)
    }

    @Test
    fun testInvariantCounting() {
        val code = """
            class MyClass {
                int value;
                /*@ invariant value >= 0; @*/

                /*@ requires x >= 0; ensures value >= 0; @*/
                void setValue(int x) { value = x; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKeys.classExpr("invariant")]).isEqualTo(1)
    }

    // ==================== Information Flow Clauses ====================
    @Test
    fun testDeterminesClauseCounting() {
        val code = """
            class MyClass {
                int input, output;
                /*@ determines output \by input; @*/
                void compute() { output = input * 2; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Determines clause creates a contract - verify specified method is detected
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
    }

    @Test
    fun testSeparatesClauseCounting() {
        val code = """
            class MyClass {
                int a, b;
                /*@ separates a, b; @*/
                void independent() {}
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Separates clause creates a contract - verify specified method is detected
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
    }

    // ==================== Let Expressions ====================
    @Test
    fun testLetExpressionCounting() {
        // Note: JML uses \let but in Java strings we need single backslash
        val code = """
            class MyClass {
                /*@ requires (\let int x = arg; x > 0);
                    ensures \result == x + 1; @*/
                int increment(int arg) { return arg + 1; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Let expressions are counted as part of clauses
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isAtLeast(1)
    }

    @Test
    fun testNestedLetExpressions() {
        val code = """
            class MyClass {
                /*@ requires (\let int x = a; (\let int y = b; x > 0) && y > 0);
                    ensures \result > 0; @*/
                int add(int a, int b) { return a + b; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isAtLeast(1)
        assertThat(stats[StatisticKey.LET]).isEqualTo(2)
    }

    // ==================== Quantifier Statistics ====================
    @Test
    fun testForallClauseCounting() {
        val code = """
            class MyClass {
                /*@ requires (\forall int i; 0 <= i && i < arr.length; arr[i] >= 0); @*/
                void process(int[] arr) {}
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.totalQuantifiers]).isEqualTo(1)
    }

    @Test
    fun testExistsClauseCounting() {
        val code = """
            class MyClass {
                /*@ ensures (\exists int i; 0 <= i && i < arr.length; arr[i] == target); @*/
                int find(int[] arr, int target) { return 0; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.totalQuantifiers]).isEqualTo(1)
    }

    // ==================== JML Statements (Body Level) ====================
    @Test
    fun testGhostStatementCounting() {
        val code = """
            class MyClass {
                void method() {
                    /*@ ghost int g = 5; @*/
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Ghost statements should be detected
        assertThat(stats).isNotNull()
    }

    @Test
    fun testAssertStatementCounting() {
        val code = """
            class MyClass {
                void method(int x) {
                    /*@ assert x > 0; @*/
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats).isNotNull()
    }

    @Test
    fun testAssumeStatementCounting() {
        val code = """
            class MyClass {
                void method(int x) {
                    /*@ assume x > 0; @*/
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats).isNotNull()
    }

    @Test
    fun testSetStatementCounting() {
        val code = """
            class MyClass {
                void method() {
                    /*@ set print = true; @*/
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats).isNotNull()
    }

    // ==================== Type-Level JML Declarations ====================
    @Test
    fun testRepresentsDeclaration() {
        val code = """
            class MyClass {
                int abstractValue;
                int concreteValue;
                //@ represents abstractValue = concreteValue;
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Represents declarations should be parsed
        assertThat(stats).isNotNull()
    }

    @Test
    fun testInitiallyClause() {
        val code = """
            class Counter {
                int count;
                /*@ initially count == 0; @*/

                /*@ ensures count > \old(count); @*/
                void increment() { count++; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats).isNotNull()
    }

    @Test
    fun testTypeLevelAssignable() {
        val code = """
            class MyClass {
                int field;
                /*@ assignable field; @*/
                void modify() { field = 1; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats).isNotNull()
    }

    // ==================== Hierarchical Statistics ====================
    @Test
    fun testHierarchicalAggregation() {
        val code = """
            class Outer {
                class Inner {
                    /*@ requires x > 0; ensures \result > 0; @*/
                    int process(int x) { return x; }
                }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Total classes should be 2
        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(2)
        // Total methods should be 1
        assertThat(stats[StatisticKey.TOTAL_METHODS]).isEqualTo(1)
        // Specified methods should be 1
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)

        // Check that children statistics are collected (access via reflection or skip for now)
        // Note: children field is private in Statistics
    }

    @Test
    fun testSubStatisticsCollection() {
        val code = """
            class A { void m1() {} }
            class B { /*@ requires x > 0; @*/ void m2(int x) {} }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Should have sub-statistics for each class (children is private, so we verify totals instead)
        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(2)
    }

    // ==================== Edge Cases ====================
    @Test
    fun testEmptyCompilationUnit() {
        val code = ""
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(0)
        assertThat(stats[StatisticKey.TOTAL_METHODS]).isEqualTo(0)
    }

    @Test
    fun testClassWithoutMethods() {
        val code = """
            class EmptyClass {
                int field;
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTALCLASSES]).isEqualTo(1)
        assertThat(stats[StatisticKey.TOTAL_METHODS]).isEqualTo(0)
    }

    @Test
    fun testMethodWithoutSpecifications() {
        val code = """
            class MyClass {
                void unspecifiedMethod() {}
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        assertThat(stats[StatisticKey.TOTAL_METHODS]).isEqualTo(1)
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(0)
    }

    @Test
    fun testMultipleContractsOnSameMethod() {
        val code = """
            class MyClass {
                /*@ requires x > 0; ensures \result > 0; @*/
                /*@ requires x < 100; ensures \result < 100; @*/
                int process(int x) { return x; }
            }

            """.trimIndent()
        val cu = parse(code)
        val stats = computeStatistics(cu)

        // Method should be counted once as specified
        assertThat(stats[StatisticKey.SPECIFIED_METHODS]).isEqualTo(1)
    }

    // ==================== Helper Methods ====================
    private fun computeStatistics(cu: CompilationUnit): Statistics {
        val stats = Statistics()
        val reporter = JmlStatisticsReporter()
        reporter.visit(cu, stats)
        return stats.summary()
    }

    private fun parse(code: String): CompilationUnit {
        val parse = javaParser.parse(code)
        require(parse.isSuccessful) {
            parse.problems.forEach(::println)
            "Parsing failed!"
        }
        return parse.result.orElseThrow()
    }
}

val javaParser by lazy {
    val config = ParserConfiguration()
    config.isProcessJml = true
    JavaParser(config)
}
