/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt;
import com.github.javaparser.ast.jml.stmt.JmlLabelStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parser tests for labelled JML assert/assume/set statements.
 *
 * <p>JMLtk supports both label positions:
 *
 * <ul>
 *   <li>KeY's label-after-keyword form {@code assert LAB: e;} — the label is stored on the
 *       {@link JmlExpressionStmt} itself.</li>
 *   <li>The JML-standard prefix form {@code LAB: assert e;} — represented by a {@link JmlLabelStmt}
 *       followed by the assert statement.</li>
 * </ul>
 */
class JmlLabeledAssertTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setProcessJml(true));

    private JmlExpressionStmt parseAssert(String jml) {
        ParseResult<CompilationUnit> result = javaParser.parse("class Foo {\nvoid m(int x) {\n//@ " + jml + "\n}\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        return result.getResult().orElseThrow().findAll(JmlExpressionStmt.class).get(0);
    }

    @Test
    void keYLabelAfterAssert() {
        JmlExpressionStmt stmt = parseAssert("assert LAB: x > 0;");
        assertThat(stmt.getKind()).isEqualTo(JmlExpressionStmt.JmlStmtKind.ASSERT);
        assertThat(stmt.getLabel()).isPresent();
        assertThat(stmt.getLabel().orElseThrow().asString()).isEqualTo("LAB");
        assertThat(stmt.getExpression().toString()).isEqualTo("x > 0");
    }

    @Test
    void keYLabelAfterAssertWithProofScript() {
        JmlExpressionStmt stmt = parseAssert("assert LAB: x > 0 \\by { assume x > 0; auto; };");
        assertThat(stmt.getLabel())
                .isPresent()
                .get()
                .extracting(s -> s.asString())
                .isEqualTo("LAB");
        assertThat(stmt.getScript()).isPresent();
        assertThat(stmt.getScript().orElseThrow()).hasSize(2);
    }

    @Test
    void unlabelledAssertStillParses() {
        JmlExpressionStmt stmt = parseAssert("assert x > 0;");
        assertThat(stmt.getLabel()).isEmpty();
        assertThat(stmt.getExpression().toString()).isEqualTo("x > 0");
    }

    @Test
    void labelledAssumeAndSet() {
        JmlExpressionStmt assume = parseAssert("assume LAB: x > 0;");
        assertThat(assume.getLabel())
                .isPresent()
                .get()
                .extracting(s -> s.asString())
                .isEqualTo("LAB");
        JmlExpressionStmt set = parseAssert("set SN: x = x + 1;");
        assertThat(set.getKind()).isEqualTo(JmlExpressionStmt.JmlStmtKind.SET);
        assertThat(set.getLabel())
                .isPresent()
                .get()
                .extracting(s -> s.asString())
                .isEqualTo("SN");
    }

    @Test
    void jmlPrefixLabelStillWorks() {
        ParseResult<CompilationUnit> result =
                javaParser.parse("class Foo {\nvoid m(int x) {\n//@ LAB: assert x > 0;\n}\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        CompilationUnit cu = result.getResult().orElseThrow();
        assertThat(cu.findAll(JmlLabelStmt.class)).hasSize(1);
        assertThat(cu.findFirst(JmlLabelStmt.class).orElseThrow().getLabel().asString())
                .isEqualTo("LAB");
        assertThat(cu.findAll(JmlExpressionStmt.class)).hasSize(1);
    }

    @Test
    void roundTripPrinting() {
        JmlExpressionStmt stmt = parseAssert("assert LAB: x > 0;");
        assertThat(stmt.toString().trim()).isEqualTo("/*@ assert LAB: x > 0;*/");
    }
}
