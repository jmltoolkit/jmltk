/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
import com.github.javaparser.ast.jml.proof.JmlProofArg;
import com.github.javaparser.ast.jml.proof.JmlProofCmdStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parser tests for JML class-body {@code lemma} declarations and the
 * {@code use_lemma} proof command, e.g.
 *
 * <pre>{@code
 * //@ public static no_state lemma nospace(\seq s) \by { use_lemma nospace(x); auto; };
 * }</pre>
 */
class LemmaDeclarationTest {

    private final ParserConfiguration config = new ParserConfiguration().setProcessJml(true);
    private final JavaParser javaParser = new JavaParser(config);

    private JmlLemmaDeclaration parseLemma(String jml) {
        ParseResult<CompilationUnit> result = javaParser.parse("class Foo {\n//@ " + jml + "\n}\n");
        Assertions.assertTrue(result.isSuccessful(), () -> result.getProblems().toString());
        return result.getResult()
                .orElseThrow()
                .findAll(JmlLemmaDeclaration.class)
                .getFirst();
    }

    private ParseResult<CompilationUnit> parse(String jml) {
        return javaParser.parse("class Foo {\n//@ " + jml + "\n}\n");
    }

    @Test
    void scriptForm() {
        JmlLemmaDeclaration d = parseLemma("public static no_state lemma nospace(\\seq s) \\by { oss; auto; };");
        assertThat(d.getName().asString()).isEqualTo("nospace");
        assertThat(d.getScript()).hasSize(2);
        assertThat(d.getModifiers())
                .extracting(Modifier::getKeyword)
                .containsExactly(
                        Modifier.DefaultKeyword.PUBLIC,
                        Modifier.DefaultKeyword.STATIC,
                        Modifier.DefaultKeyword.JML_NO_STATE);
    }

    @Test
    void singleProofScriptStatement() {
        JmlLemmaDeclaration d = parseLemma("lemma L(int x) \\by { auto; };");
        assertThat(d.getScript()).hasSize(1);
        assertThat(d.getScript().getFirst()).isInstanceOf(JmlProofCmdStmt.class);
    }

    @Test
    void useLemmaInsideScriptIsProofCommand() {
        JmlLemmaDeclaration d = parseLemma("lemma L(int x) \\by { use_lemma nospace(x); auto; };");
        JmlProofCmdStmt use = (JmlProofCmdStmt) d.getScript().getFirst();
        assertThat(use.getName().asString()).isEqualTo("use_lemma");
        assertThat(use.getArgs()).hasSize(1);
        JmlProofArg arg = use.getArgs().getFirst();
        assertThat(arg.getLabel()).isEmpty();
        assertThat(arg.getExpr()).isInstanceOf(MethodCallExpr.class);
        assertThat(arg.getExpr().asMethodCallExpr().getNameAsString()).isEqualTo("nospace");
    }

    @Test
    void negativeDefinitionalFormIsRejected() {
        assertThat(parse("lemma P(int i) = i > 0;").isSuccessful()).isFalse();
    }

    @Test
    void negativeAssertedFormIsRejected() {
        assertThat(parse("lemma P(int i) assert i > 0 \\by { auto; };").isSuccessful())
                .isFalse();
    }

    @Test
    void negativeMissingByScript() {
        assertThat(parse("lemma helper();").isSuccessful()).isFalse();
    }

    @Test
    void negativeMissingSemicolon() {
        assertThat(parse("lemma P(int i) \\by { auto; }").isSuccessful()).isFalse();
    }

    @Test
    void negativeLemmaWithoutName() {
        assertThat(parse("lemma (int i) \\by { auto; };").isSuccessful()).isFalse();
    }
}
