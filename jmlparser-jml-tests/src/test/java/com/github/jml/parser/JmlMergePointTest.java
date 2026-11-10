/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.jml.stmt.JmlMergePointStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parser tests for the JML {@code merge_point} statement, e.g.
 * <pre>{@code
 * //@ merge_point \\by "MergeByPredicateAbstraction"
 * //@     \\lattice conjunctive
 * //@     \\predicate (int x) : x >= 0, x == \\old(b) || x == -\\old(b);
 * }</pre>
 */
class JmlMergePointTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setProcessJml(true));

    private JmlMergePointStmt parseMergePoint(String jml) {
        ParseResult<CompilationUnit> result = javaParser.parse("class Foo {\nvoid m(int b) {\n//@ " + jml + "\n}\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        return result.getResult()
                .orElseThrow()
                .findFirst(JmlMergePointStmt.class)
                .orElseThrow();
    }

    @Test
    void bareMergePoint() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point;");
        assertThat(stmt.getBy()).isEmpty();
        assertThat(stmt.getLattice()).isEmpty();
        assertThat(stmt.getPredicateVariable()).isEmpty();
        assertThat(stmt.getPredicates()).isEmpty();
    }

    @Test
    void byAsStringLiteral() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\by \"MergeByIfThenElse\";");
        assertThat(stmt.getBy())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("MergeByIfThenElse");
        assertThat(stmt.getLattice()).isEmpty();
        assertThat(stmt.getPredicateVariable()).isEmpty();
        assertThat(stmt.getPredicates()).isEmpty();
    }

    @Test
    void byAsIdentifierIsNormalisedToLiteral() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\by MergeByIfThenElse;");
        assertThat(stmt.getBy())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("MergeByIfThenElse");
    }

    @Test
    void latticeAsIdentifier() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\lattice conjunctive;");
        assertThat(stmt.getLattice())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("conjunctive");
    }

    @Test
    void latticeAsStringLiteral() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\lattice \"conjunctive\";");
        assertThat(stmt.getLattice())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("conjunctive");
    }

    @Test
    void fullExample() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\by \"MergeByPredicateAbstraction\"\n"
                + "//@ \\lattice conjunctive\n"
                + "//@ \\predicate (int x) : x >= 0, x == \\old(b) || x == -\\old(b);");
        assertThat(stmt.getBy())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("MergeByPredicateAbstraction");
        assertThat(stmt.getLattice())
                .isPresent()
                .get()
                .extracting(StringLiteralExpr::getValue)
                .isEqualTo("conjunctive");
        assertThat(stmt.getPredicateVariable()).isPresent();
        assertThat(stmt.getPredicateVariable().orElseThrow().getType().asString())
                .isEqualTo("int");
        assertThat(stmt.getPredicateVariable().orElseThrow().getNameAsString()).isEqualTo("x");
        assertThat(stmt.getPredicates()).isPresent();
        assertThat(stmt.getPredicates().orElseThrow())
                .map(Object::toString)
                .containsExactly("x >= 0", "x == \\old(b) || x == -\\old(b)");
    }

    @Test
    void roundTripPrinting() {
        JmlMergePointStmt stmt = parseMergePoint("merge_point \\by \"MergeByPredicateAbstraction\"\n"
                + "//@ \\lattice conjunctive\n"
                + "//@ \\predicate (int x) : x >= 0, x == \\old(b) || x == -\\old(b);");
        assertThat(stmt.toString().trim())
                .isEqualTo("/*@ merge_point \\by \"MergeByPredicateAbstraction\" "
                        + "\\lattice \"conjunctive\" \\predicate (int x) : "
                        + "x >= 0, x == \\old(b) || x == -\\old(b);*/");
    }

    @Test
    void dollarForm() {
        JmlMergePointStmt stmt = parseMergePoint("$mergePoint;");
        assertThat(stmt.getBy()).isEmpty();
        assertThat(stmt.getLattice()).isEmpty();
        assertThat(stmt.getPredicateVariable()).isEmpty();
        assertThat(stmt.getPredicates()).isEmpty();
    }

    @Test
    void proofJavaCallFormParsesAsMethodCall() {
        // `merge_point` is not a reserved word: with a following "( ... )" it is an
        // ordinary method invocation, not a merge point.
        JavaParser plain = new JavaParser();
        ParseResult<CompilationUnit> result = plain.parse("class Foo {\nvoid m() {\nmerge_point ( x );\n}\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        Statement stmt = result.getResult()
                .orElseThrow()
                .findFirst(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .orElseThrow()
                .getMethods()
                .get(0)
                .getBody()
                .orElseThrow()
                .getStatements()
                .get(0);
        assertThat(stmt).isInstanceOf(ExpressionStmt.class);
        assertThat(((ExpressionStmt) stmt).getExpression()).isInstanceOf(MethodCallExpr.class);
        MethodCallExpr call = (MethodCallExpr) ((ExpressionStmt) stmt).getExpression();
        assertThat(call.getNameAsString()).isEqualTo("merge_point");
        assertThat(call.getArguments()).hasSize(1);
        assertThat(call.getScope()).isEmpty();
    }

    @Test
    void mergePointIsUsableAsIdentifier() {
        JavaParser plain = new JavaParser();
        ParseResult<CompilationUnit> result = plain.parse("class Foo {\nint merge_point = 5;\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        assertThat(result.getResult().orElseThrow().findFirst(JmlMergePointStmt.class))
                .isEmpty();
    }
}
