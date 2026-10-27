/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr;
import com.github.javaparser.jml.JmlQuantifierDesugarer;
import com.github.javaparser.jml.JmlQuantifierDesugarer.Kind;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@link JmlQuantifierDesugarer} rewrites single-binding element quantifiers
 * into the classic index form:
 *
 * <pre>{@code
 * (\forall int i : 0..n; a[i])                    ≡ (\forall int i; 0 <= i && i < n; a[i])
 * (\forall int e : a; e > 0)                       ≡ (\forall int _i; 0 <= _i && _i < a.length; a[_i] > 0)
 * }</pre>
 *
 * <p>Range domains are decided syntactically; array domains need the resolved
 * type of the domain expression. {@code Iterable} domains (kind 4) and classic
 * bindings are left untouched.
 *
 * @author Alexander Weigl
 */
class JmlQuantifierDesugarerTest {

    private static final String SOURCE = String.join("\n",
            "class Domains {",
            "    int n;",
            "    int[] a;",
            "    int[][] m;",
            "    Object[] objs;",
            "    java.util.List<String> words;",
            "    //@ invariant (\\forall int e : a; e > 0);",
            "    //@ invariant (\\exists int[] row : m; row.length == n);",
            "    //@ invariant (\\sum int e : a; e * e);",
            "    //@ invariant (\\forall String s : words; s.length() > 0);",
            "    //@ invariant (\\forall int i : 0..n; a[i] > 0);",
            "    //@ invariant (\\forall var i : 0..n; a[i] > 1);",
            "    //@ invariant (\\forall Object o : objs; o != null; o + a[0] != null);",
            "}");

    private final TypeSolver typeSolver = new ReflectionTypeSolver(true);
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration()
            .setProcessJml(true)
            .setSymbolResolver(new JavaSymbolSolver(typeSolver)));

    private List<JmlQuantifiedExpr> elementQuantifiers() {
        ParseResult<CompilationUnit> r = javaParser.parse(SOURCE);
        assertThat(r.getProblems()).as("parse problems").isEmpty();
        return r.getResult().get().findAll(JmlQuantifiedExpr.class);
    }

    @Test
    void rangeDomainBecomesBoundFilter() {
        List<JmlQuantifiedExpr> qs = elementQuantifiers();
        assertThat(desugar(qs.get(0))).isEqualTo("(\\forall int _i; 0 <= _i && _i < a.length; a[_i] > 0)");
        // range 0..n (index 4) keeps its name and adds the half-open bound filter
        assertThat(desugar(qs.get(4))).isEqualTo("(\\forall int i; 0 <= i && i < n; a[i] > 0)");
        // var in a range domain becomes int
        assertThat(desugar(qs.get(5))).isEqualTo("(\\forall int i; 0 <= i && i < n; a[i] > 1)");
    }

    @Test
    void arrayDomainSubstitutesTheBoundVariable() {
        List<JmlQuantifiedExpr> qs = elementQuantifiers();
        // T[][] iterates components: m[_i] is int[] (binder is preserved)
        assertThat(desugar(qs.get(1))).isEqualTo("(\\exists int _i; 0 <= _i && _i < m.length; m[_i].length == n)");
        // substitution applies to every occurrence
        assertThat(desugar(qs.get(2))).isEqualTo("(\\sum int _i; 0 <= _i && _i < a.length; a[_i] * a[_i])");
        // filters and body are rewritten too
        assertThat(desugar(qs.get(6))).isEqualTo(
                "(\\forall int _i; 0 <= _i && _i < objs.length; objs[_i] != null; objs[_i] + a[0] != null)");
    }

    @Test
    void iterableAndClassicDomainsAreLeftUntouched() {
        List<JmlQuantifiedExpr> qs = elementQuantifiers();
        // kind 4 (Iterable) — formal model deferred
        assertThat(JmlQuantifierDesugarer.desugar(qs.get(3))).isEmpty();

        ParseResult<Expression> classic = new JavaParser().parseJmlExpression("(\\forall int x; 0 <= x < 5; a[x])");
        assertThat(JmlQuantifierDesugarer.desugar(classic.getResult().get().asJmlQuantifiedExpr())).isEmpty();
    }

    @Test
    void unresolvableDomainDegradesGracefully() {
        // no symbol resolver configured -> array domains cannot be classified
        ParseResult<Expression> r = new JavaParser().parseJmlExpression("(\\forall int e : a; e > 0)");
        assertThat(JmlQuantifierDesugarer.desugar(r.getResult().get().asJmlQuantifiedExpr())).isEmpty();
    }

    @Test
    void kindClassification() {
        List<JmlQuantifiedExpr> qs = elementQuantifiers();
        assertThat(JmlQuantifierDesugarer.kindOf(qs.get(0))).isEqualTo(Kind.ARRAY);
        assertThat(JmlQuantifierDesugarer.kindOf(qs.get(1))).isEqualTo(Kind.ARRAY);
        assertThat(JmlQuantifierDesugarer.kindOf(qs.get(3))).isEqualTo(Kind.ITERABLE);
        assertThat(JmlQuantifierDesugarer.kindOf(qs.get(4))).isEqualTo(Kind.RANGE);
        assertThat(JmlQuantifierDesugarer.kindOf(qs.get(5))).isEqualTo(Kind.RANGE);
        assertThat(JmlQuantifierDesugarer.isElementQuantifier(qs.get(6))).isTrue();

        ParseResult<Expression> classic = new JavaParser().parseJmlExpression("(\\forall int x; 0 <= x < 5; a[x])");
        assertThat(JmlQuantifierDesugarer.kindOf(classic.getResult().get().asJmlQuantifiedExpr()))
                .isEqualTo(Kind.CLASSIC);
    }

    private String desugar(JmlQuantifiedExpr n) {
        Optional<JmlQuantifiedExpr> d = JmlQuantifierDesugarer.desugar(n);
        assertThat(d).as("desugar(%s)", n).isPresent();
        return d.get().toString();
    }
}
