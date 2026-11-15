/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.jml.clauses.JmlClause;
import com.github.javaparser.ast.jml.clauses.JmlClauseKeyword;
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KeY's {@code assignable_free} clause is a JML extension (the free counterpart of
 * {@code assignable}). JMLtk does not know the clause keyword yet, but it must not silently
 * mis-parse it as a Java field declaration: in
 *
 * <pre>{@code
 * class Foo {
 *     int field;
 *     /*@ normal_behavior
 *       @ assignable_free field;
 *       @*&#47;
 *     void m() { field = 21; }
 * }
 * }</pre>
 *
 * the parser currently drops the clause and inserts {@code FieldDeclaration type=assignable_free
 * vars=[field]} into the class body. KeY's converter then tries to resolve the "type"
 * {@code assignable_free} and aborts the whole proof load with an
 * {@code UnsolvedSymbolException}.
 */
class AssignableFreeClauseTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setProcessJml(true));

    @Test
    void assignableFreeInMethodContractIsNotAMisplacedField() {
        ParseResult<CompilationUnit> r = javaParser.parse("""
                class Foo {
                    int field;
                    /*@ normal_behavior
                      @ ensures field == 21;
                      @ assignable \\strictly_nothing;
                      @ assignable_free field;
                      @*/
                    void m() { field = 21; }
                }
                """);

        assertThat(r.getResult()).isPresent();
        CompilationUnit cu = r.getResult().orElseThrow();

        assertThat(cu.findAll(FieldDeclaration.class))
                .as("assignable_free must not become a Java field declaration")
                .noneMatch(fd -> fd.getElementType().asString().equals("assignable_free"));

        boolean representedAsClause = cu.findAll(JmlClause.class).stream()
                .anyMatch(clause -> clause.toString().strip().startsWith("assignable_free"));
        assertThat(representedAsClause || !r.isSuccessful())
                .as("assignable_free must be represented as a clause or rejected loudly")
                .isTrue();
    }

    @Test
    void assignableFreeIsParsedAsAFreeAssignableClause() {
        ParseResult<CompilationUnit> r = javaParser.parse("""
                class Foo {
                    int field;
                    /*@ normal_behavior
                      @ ensures field == 21;
                      @ assignable \\strictly_nothing;
                      @ assignable_free field;
                      @*/
                    void m() { field = 21; }
                }
                """);

        CompilationUnit cu = r.getResult().orElseThrow();
        MethodDeclaration m =
                cu.getClassByName("Foo").orElseThrow().getMethodsByName("m").get(0);

        assertThat(m.getContracts()).hasSize(1);
        assertThat(m.getContracts().get(0).getClauses())
                .filteredOn(clause -> clause.getKind().getValue() == JmlClauseKeyword.ASSIGNABLE_FREE)
                .singleElement()
                .satisfies(clause -> assertThat(clause)
                        .as("assignable_free is an assignable-style multi-expression clause")
                        .isInstanceOf(JmlMultiExprClause.class));
    }
}
