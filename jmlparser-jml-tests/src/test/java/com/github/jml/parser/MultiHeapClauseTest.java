/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.jml.body.JmlClassAccessibleDeclaration;
import com.github.javaparser.ast.jml.clauses.JmlClause;
import com.github.javaparser.ast.jml.clauses.JmlClauseKeyword;
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause;
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JML clause may carry several heap specifiers, e.g.
 *
 * <pre>{@code
 * assignable<heap><savedHeap> bArray[bOffset..bOffset+length-1];
 * }</pre>
 *
 * <p>All specifiers belong to the clause. The grammar used to overwrite the heap list on every
 * iteration, keeping only the last specifier ({@code savedHeap} above) and silently dropping
 * {@code heap}. That made KeY treat the clause as missing for the base heap and fall back to
 * {@code \everything}.
 *
 * <p>The same specifier list must also reach loop-invariant clauses and class-level
 * {@code accessible} declarations, and the pretty printer must render each specifier in angle
 * brackets.
 */
class MultiHeapClauseTest {

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setProcessJml(true));

    private CompilationUnit parse(String source) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        assertThat(result.isSuccessful()).as(result.getProblems()::toString).isTrue();
        return result.getResult().orElseThrow();
    }

    private JmlMultiExprClause parseSingleClause(String jmlClause) {
        CompilationUnit cu = parse("""
                class Foo {
                    int[] a;
                    /*@ normal_behavior
                      @ %s
                      @*/
                    void m() { }
                }
                """.formatted(jmlClause));
        MethodDeclaration m =
                cu.getClassByName("Foo").orElseThrow().getMethodsByName("m").get(0);
        List<JmlClause> clauses = m.getContracts().get(0).getClauses();
        assertThat(clauses).hasSize(1);
        assertThat(clauses.get(0)).isInstanceOf(JmlMultiExprClause.class);
        return (JmlMultiExprClause) clauses.get(0);
    }

    private static List<String> heapNames(List<SimpleName> heaps) {
        return heaps.stream().map(SimpleName::asString).toList();
    }

    @Test
    void assignableKeepsAllHeapSpecifiers() {
        JmlMultiExprClause clause = parseSingleClause("assignable<heap><savedHeap> a[0..3];");

        assertThat(heapNames(clause.getHeaps().orElseThrow())).containsExactly("heap", "savedHeap");
        assertThat(clause.getExpressions()).hasSize(1);
    }

    @Test
    void accessibleKeepsAllHeapSpecifiers() {
        JmlMultiExprClause clause = parseSingleClause("accessible<heap><savedHeap> a;");

        assertThat(heapNames(clause.getHeaps().orElseThrow())).containsExactly("heap", "savedHeap");
    }

    @Test
    void singleHeapSpecifierStillWorks() {
        JmlMultiExprClause clause = parseSingleClause("assignable<heap> a;");

        assertThat(heapNames(clause.getHeaps().orElseThrow())).containsExactly("heap");
    }

    @Test
    void multiHeapClausePrintsEverySpecifierInAngleBrackets() {
        JmlMultiExprClause clause = parseSingleClause("assignable<heap><savedHeap> a;");

        assertThat(clause.toString()).contains("assignable<heap><savedHeap>");
    }

    @Test
    void loopInvariantKeepsAllHeapSpecifiers() {
        CompilationUnit cu = parse("""
                class Foo {
                    boolean[] a;
                    void m() {
                        /*@ loop_invariant<heap><savedHeap> a != null;
                          @*/
                        while (a.length > 0) { }
                    }
                }
                """);
        JmlSimpleExprClause invariant = cu.findAll(JmlSimpleExprClause.class).stream()
                .filter(c -> c.getKind().getValue() == JmlClauseKeyword.LOOP_INVARIANT)
                .findFirst()
                .orElseThrow();

        assertThat(heapNames(invariant.getHeaps())).containsExactly("heap", "savedHeap");
    }

    @Test
    void classAccessibleKeepsAllHeapSpecifiers() {
        CompilationUnit cu = parse("""
                class Foo {
                    int[] a;
                    //@ accessible<heap><savedHeap> a : a;
                    void m() { }
                }
                """);
        JmlClassAccessibleDeclaration accessible =
                cu.findFirst(JmlClassAccessibleDeclaration.class).orElseThrow();

        assertThat(heapNames(accessible.getHeaps())).containsExactly("heap", "savedHeap");
        assertThat(accessible.toString()).contains("accessible<heap><savedHeap>");
    }
}
