/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.expr.LambdaExpr
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 *
 * @author Alexander Weigl
 * @version 1 (26.08.26)
 */
class LambdaExprTests {
    private val config = ParserConfiguration().also {
        it.isProcessJml = true
        it.languageLevel = ParserConfiguration.LanguageLevel.JAVA_25
    }
    private val javaParser = JavaParser(config)

    @Test
    fun `lambda with simple ensures contract`() {
        val code = """
            interface Calculator {
                int compute(int x);
            }
            public class Test {
                public void test() {
                    Calculator c = /*@ ensures \result >= 0; @*/ (int x) -> x * 2;
                }
            }
        """.trimIndent()

        val result = javaParser.parse(code)
        assertTrue(result.isSuccessful, "Parsing should succeed")

        val cu = result.result.get()
        val lambdaExpr = cu.findAll(LambdaExpr::class.java).first()

        assertEquals(1, lambdaExpr.contracts.size, "Should have one contract")
        val contract = lambdaExpr.contracts[0]
        assertEquals(1, contract.clauses.size, "Contract should have one clause")
        assertEquals(JmlClauseKind.ENSURES, contract.clauses[0].kind, "Should be an ensures clause")
    }

    @Test
    fun `lambda with multiple parameters and requires contract`() {
        val code = """
            interface BinaryOp {
                int apply(int a, int b);
            }
            public class Test {
                public void test() {
                    BinaryOp op = /*@ requires a > 0; @*/ (int a, int b) -> a + b;
                }
            }
        """.trimIndent()

        val result = javaParser.parse(code)
        assertTrue(result.isSuccessful, "Parsing should succeed")

        val cu = result.result.get()
        val lambdaExpr = cu.findAll(LambdaExpr::class.java).first()

        assertEquals(1, lambdaExpr.contracts.size, "Should have one contract")
        val contract = lambdaExpr.contracts[0]
        assertEquals(1, contract.clauses.size, "Contract should have one clause")
        assertEquals(JmlClauseKind.REQUIRES, contract.clauses[0].kind, "Should be a requires clause")
        assertEquals(2, lambdaExpr.parameters.size, "Lambda should have 2 parameters")
    }

    @Test
    fun `lambda with block body and ensures contract`() {
        val code = """
            interface Mapper {
                String map(int value);
            }
            public class Test {
                public void test() {
                    Mapper m = /*@ ensures \result != null; @*/ (int v) -> {
                        return String.valueOf(v);
                    };
                }
            }
        """.trimIndent()

        val result = javaParser.parse(code)
        assertTrue(result.isSuccessful, "Parsing should succeed")

        val cu = result.result.get()
        val lambdaExpr = cu.findAll(LambdaExpr::class.java).first()

        assertEquals(1, lambdaExpr.contracts.size, "Should have one contract")
        assertNotNull(lambdaExpr.body, "Lambda should have a body")
        assertTrue(lambdaExpr.body.isBlockStmt, "Body should be a block statement")
    }

    @Test
    fun `lambda with both requires and ensures contracts`() {
        val code = """
            interface Divider {
                int divide(int a, int b);
            }
            public class Test {
                public void test() {
                    Divider d = /*@ requires b != 0; ensures \result * b == a; @*/
                                (a, b) -> a / b;
                }
            }
        """.trimIndent()

        val result = javaParser.parse(code)
        assertTrue(result.isSuccessful, "Parsing should succeed")

        val cu = result.result.get()
        val lambdaExpr = cu.findAll(LambdaExpr::class.java).first()

        assertEquals(1, lambdaExpr.contracts.size, "Should have one contract")
        val contract = lambdaExpr.contracts[0]
        assertEquals(2, contract.clauses.size, "Contract should have two clauses")
        assertEquals(JmlClauseKind.REQUIRES, contract.clauses[0].kind, "First clause should be requires")
        assertEquals(JmlClauseKind.ENSURES, contract.clauses[1].kind, "Second clause should be ensures")
    }

    @Test
    fun `lambda without parameters with contract`() {
        val code = """
            interface Supplier {
                int get();
            }
            public class Test {
                public void test() {
                    Supplier s = /*@ ensures \result == 42; @*/ () -> 42;
                }
            }
        """.trimIndent()

        val result = javaParser.parse(code)
        assertTrue(result.isSuccessful, "Parsing should succeed")

        val cu = result.result.get()
        val lambdaExpr = cu.findAll(LambdaExpr::class.java).first()

        assertEquals(1, lambdaExpr.contracts.size, "Should have one contract")
        assertEquals(0, lambdaExpr.parameters.size, "Lambda should have no parameters")
        assertTrue(lambdaExpr.isEnclosingParameters, "Should have enclosing parentheses")
    }
}
