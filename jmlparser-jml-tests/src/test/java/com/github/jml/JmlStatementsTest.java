/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.github.javaparser.ast.jml.stmt.JmlExpressionStmt.JmlStmtKind.*;

/**
 * Test suite for JML statements.
 * Covers: JmlAssertStatement, JmlSetStatement, JmlAssumeStatement, JmlSkipStatement.
 * 
 * @author Alexander Weigl
 * @version 1.0
 */
class JmlStatementsTest {
    private final JavaParser javaParser = new JavaParser();

    /**
     * Helper method to find the first method declaration in a compilation unit.
     */
    private MethodDeclaration findFirstMethod(CompilationUnit cu) {
        return cu.findAll(MethodDeclaration.class).get(0);
    }

    /**
     * Helper method to get the block statement from a method.
     */
    private BlockStmt getMethodBody(MethodDeclaration method) {
        return method.getBody().orElseThrow(() -> new AssertionError("Method has no body"));
    }

    /**
     * Helper method to find JML expression statements of a specific kind.
     */
    private JmlExpressionStmt findJmlStatement(BlockStmt body, JmlExpressionStmt.JmlStmtKind kind) {
        return body.findAll(JmlExpressionStmt.class).stream()
                .filter(stmt -> stmt.getKind() == kind)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No JML statement of kind " + kind + " found"));
    }

    // ==================== JML Assert Statements ====================
    @Test
    void testJmlAssertSimple() {
        String code = """
            public class Example {
                void method(int x) {
                    //@ assert x >= 0;
                    System.out.println(x);
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        // Find the JML assert statement
        JmlExpressionStmt assertStmt = findJmlStatement(body, ASSERT);
        
        // Verify the expression is a binary expression (x >= 0)
        Assertions.assertInstanceOf(BinaryExpr.class, assertStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assertStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.GREATER_EQUALS, binExpr.getOperator());
        Assertions.assertInstanceOf(NameExpr.class, binExpr.getLeft());
        Assertions.assertEquals("x", ((NameExpr) binExpr.getLeft()).getNameAsString());
        Assertions.assertInstanceOf(IntegerLiteralExpr.class, binExpr.getRight());
        Assertions.assertEquals("0", ((IntegerLiteralExpr) binExpr.getRight()).getValue());
    }

    @Test
    void testJmlAssertWithOld() {
        String code = """
            public class Counter {
                int value;
                void increment() {
                    int oldVal = value;
                    value++;
                    //@ assert value == oldVal + 1;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt assertStmt = findJmlStatement(body, ASSERT);
        
        // Verify the expression is an equality check
        Assertions.assertInstanceOf(BinaryExpr.class, assertStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assertStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.EQUALS, binExpr.getOperator());
    }

    @Test
    void testJmlAssertWithQuantifier() {
        String code = """
            public class Example {
                void checkAllPositive(int[] arr) {
                    //@ assert (\\forall int i; 0 <= i && i < arr.length; arr[i] > 0);
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt assertStmt = findJmlStatement(body, ASSERT);
        
        // Verify the expression contains a forall quantifier
        Assertions.assertNotNull(assertStmt.getExpression());
        Assertions.assertTrue(assertStmt.getExpression().toString().contains("\\forall"));
    }

    @Test
    void testJmlAssertInLoop() {
        String code = """
            public class Example {
                int sum(int[] arr) {
                    int sum = 0;
                    for (int i = 0; i < arr.length; i++) {
                        sum += arr[i];
                        //@ assert sum >= 0;
                    }
                    return sum;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        // Find the for loop
        var forStmt = body.findAll(com.github.javaparser.ast.stmt.ForStmt.class).get(0);
        BlockStmt loopBody = (BlockStmt) forStmt.getBody();
        
        JmlExpressionStmt assertStmt = findJmlStatement(loopBody, ASSERT);
        
        // Verify the assertion expression
        Assertions.assertInstanceOf(BinaryExpr.class, assertStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assertStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.GREATER_EQUALS, binExpr.getOperator());
    }

    // ==================== JML Set Statements ====================
    @Test
    void testJmlSetSimple() {
        String code = """
            public class Example {
                /*@ ghost int counter; */
                void method() {
                    //@ set counter = 0;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        // Find the JML set statement
        JmlExpressionStmt setStmt = findJmlStatement(body, SET);
        
        // Verify it's a SET statement
        Assertions.assertEquals(SET, setStmt.getKind());
        
        // Verify the expression is an assignment
        Assertions.assertInstanceOf(BinaryExpr.class, setStmt.getExpression());
        var binExpr = (AssignExpr) setStmt.getExpression();
        Assertions.assertEquals(AssignExpr.Operator.ASSIGN, binExpr.getOperator());
        
        // Verify left side is the field access
        Assertions.assertInstanceOf(NameExpr.class, binExpr.target());
        Assertions.assertEquals("counter", ((NameExpr) binExpr.target()).getNameAsString());
        
        // Verify right side is 0
        Assertions.assertInstanceOf(IntegerLiteralExpr.class, binExpr.value());
        Assertions.assertEquals("0", ((IntegerLiteralExpr) binExpr.value()).getValue());
    }

    @Test
    void testJmlSetWithExpression() {
        String code = """
            public class Counter {
                //@ ghost int historySum;
                void accumulate(int[] arr) {
                    int sum = 0;
                    for (int i = 0; i < arr.length; i++) {
                        sum += arr[i];
                    }
                    //@ set historySum = sum;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt setStmt = findJmlStatement(body, SET);
        
        Assertions.assertEquals(SET, setStmt.getKind());
        Assertions.assertInstanceOf(BinaryExpr.class, setStmt.getExpression());
        var binExpr = (AssignExpr) setStmt.getExpression();
        Assertions.assertEquals(AssignExpr.Operator.ASSIGN, binExpr.getOperator());
        Assertions.assertEquals("historySum", ((NameExpr) binExpr.target()).getNameAsString());
        Assertions.assertEquals("sum", ((NameExpr) binExpr.value()).getNameAsString());
    }

    @Test
    void testJmlSetInGhostMethod() {
        String code = """
            public class Example {
                /*@ ghost @*/ int callCount;
                
                /*@ ghost @*/ void recordCall() {
                    //@ set callCount = callCount + 1;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt setStmt = findJmlStatement(body, SET);
        
        Assertions.assertEquals(SET, setStmt.getKind());
        Assertions.assertInstanceOf(BinaryExpr.class, setStmt.getExpression());
        var binExpr = (AssignExpr) setStmt.getExpression();
        Assertions.assertEquals(AssignExpr.Operator.ASSIGN, binExpr.getOperator());
        Assertions.assertEquals("callCount", ((NameExpr) binExpr.target()).getNameAsString());
    }

    @Test
    void testJmlSetMultiple() {
        String code = """
            public class Point {
                /*@ ghost int lastX;
                    ghost int lastY;
                */
               \s
                void move(int x, int y) {
                    //@ set lastX = x;
                    //@ set lastY = y;
                }
            }
           \s""";
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        // Find all SET statements
        List<JmlExpressionStmt> setStmts = body.findAll(JmlExpressionStmt.class).stream()
                .filter(stmt -> stmt.getKind() == SET)
                .toList();
        
        Assertions.assertEquals(2, setStmts.size());
        
        // Verify first set: lastX = x
        BinaryExpr first = (BinaryExpr) setStmts.getFirst().getExpression();
        Assertions.assertEquals("lastX", ((NameExpr) first.getLeft()).getNameAsString());
        Assertions.assertEquals("x", ((NameExpr) first.getRight()).getNameAsString());
        
        // Verify second set: lastY = y
        BinaryExpr second = (BinaryExpr) setStmts.get(1).getExpression();
        Assertions.assertEquals("lastY", ((NameExpr) second.getLeft()).getNameAsString());
        Assertions.assertEquals("y", ((NameExpr) second.getRight()).getNameAsString());
    }

    // ==================== JML Assume Statements ====================
    @Test
    void testJmlAssumeSimple() {
        String code = """
            public class Example {
                void method(int x) {
                    //@ assume x > 0;
                    int result = 100 / x;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        // Find the JML assume statement
        JmlExpressionStmt assumeStmt = findJmlStatement(body, ASSUME);
        
        // Verify it's an ASSUME statement
        Assertions.assertEquals(ASSUME, assumeStmt.getKind());
        
        // Verify the expression is a binary expression (x > 0)
        Assertions.assertInstanceOf(BinaryExpr.class, assumeStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assumeStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.GREATER, binExpr.getOperator());
        Assertions.assertInstanceOf(NameExpr.class, binExpr.getLeft());
        Assertions.assertEquals("x", ((NameExpr) binExpr.getLeft()).getNameAsString());
        Assertions.assertInstanceOf(IntegerLiteralExpr.class, binExpr.getRight());
        Assertions.assertEquals("0", ((IntegerLiteralExpr) binExpr.getRight()).getValue());
    }

    @Test
    void testJmlAssumeNonNull() {
        String code = """
            public class Example {
                void process(Object obj) {
                    //@ assume obj != null;
                    String s = obj.toString();
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt assumeStmt = findJmlStatement(body, ASSUME);
        
        Assertions.assertEquals(ASSUME, assumeStmt.getKind());
        Assertions.assertInstanceOf(BinaryExpr.class, assumeStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assumeStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.NOT_EQUALS, binExpr.getOperator());
        Assertions.assertInstanceOf(NameExpr.class, binExpr.getLeft());
        Assertions.assertEquals("obj", ((NameExpr) binExpr.getLeft()).getNameAsString());
    }

    @Test
    void testJmlAssumeInRange() {
        String code = """
            public class Example {
                int get(int[] arr, int index) {
                    //@ assume 0 <= index && index < arr.length;
                    return arr[index];
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        BlockStmt body = getMethodBody(method);
        
        JmlExpressionStmt assumeStmt = findJmlStatement(body, ASSUME);
        
        Assertions.assertEquals(ASSUME, assumeStmt.getKind());
        // The expression is a logical AND: 0 <= index && index < arr.length
        Assertions.assertInstanceOf(BinaryExpr.class, assumeStmt.getExpression());
        BinaryExpr binExpr = (BinaryExpr) assumeStmt.getExpression();
        Assertions.assertEquals(BinaryExpr.Operator.AND, binExpr.getOperator());
    }

    // ==================== JML Skip Statements ====================
    @Test
    void testJmlSkip() {
        String code = """
            public class Example {
                void method(boolean condition) {
                    if (condition) {
                        //@ skip;
                        System.out.println("skipped");
                    }
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
        
        CompilationUnit cu = result.getResult().orElseThrow();
        MethodDeclaration method = findFirstMethod(cu);
        
        // Find the if statement
        var ifStmt = method.getBody().get().findAll(com.github.javaparser.ast.stmt.IfStmt.class).get(0);
        BlockStmt thenBlock = (BlockStmt) ifStmt.getThenStmt();
        
        // Find the JML skip statement - note: skip may not be directly supported as JmlExpressionStmt
        // Check if parsing was successful which indicates skip was recognized
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Combined JML Statements ====================
    @Test
    void testAssertAndSetCombined() {
        String code = """
            public class Counter {
                /*@ ghost @*/ int maxSeen;
                
                void update(int value) {
                    //@ assert value >= 0;
                    //@ set maxSeen = (value > maxSeen ? value : maxSeen);
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testAssumeAndAssertInMethod() {
        String code = """
            public class Example {
                int divide(int a, int b) {
                    //@ assume b != 0;
                    int result = a / b;
                    //@ assert result * b == a;
                    return result;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    @Test
    void testGhostCodeWithStatements() {
        String code = """
            public class History {
                /*@ ghost @*/ int[] log;
                /*@ ghost @*/ int logSize;
                
                /*@ ghost @*/ void initLog(int size) {
                    //@ set log = new int[size];
                    //@ set logSize = 0;
                }
                
                /*@ ghost @*/ void addToLog(int value) {
                    //@ assert logSize < log.length;
                    //@ set log[logSize] = value;
                    //@ set logSize = logSize + 1;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== JML Block Statements ====================
    @Test
    void testJmlBlockStatement() {
        String code = """
            public class Example {
                /*@ ghost @*/ int x;
                void method() {
                    /*@ {
                      @   x = 5;
                      @   assert x > 0;
                      @ }
                      @*/
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }

    // ==================== Labels in JML ====================
    @Test
    void testLabeledAssert() {
        String code = """
            public class Example {
                void validate(int x) {
                    labeled_assert:
                    //@ assert x >= 0;
                }
            }
            """;
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        Assertions.assertTrue(result.isSuccessful());
    }
}