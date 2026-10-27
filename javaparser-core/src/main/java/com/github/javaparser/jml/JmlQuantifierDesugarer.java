/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.jml;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedVariable;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Rewrites a single-binding, element-domain quantifier into the classic
 * index/bound form. The definitional equivalences (design {@code quantarray.md}
 * §4):
 *
 * <blockquote><pre>
 * (\binder int x : lo..hi; F…)  ≡  (\binder int x; lo &lt;= x &amp;&amp; x &lt; hi; F…)
 * (\binder T x : a; F…)          ≡  (\binder int _i; 0 &lt;= _i &amp;&amp; _i &lt; a.length; F[x := a[_i]])
 * </pre></blockquote>
 *
 * <p>A range domain ({@code BinaryExpr(Operator.RANGE)}) is detected
 * syntactically; an array domain needs the resolved type of the domain
 * expression (an {@code Iterable} domain is <em>not</em> desugared — its formal
 * model is deferred). When no symbol resolver is configured the array case must
 * degrade gracefully: resolving through {@link Expression#calculateResolvedType()}
 * fails and the quantifier is left untouched.
 *
 * <p>Used by the KeY front end and the LSP "expand quantifier" quick fix.
 *
 * @author Alexander Weigl
 */
public final class JmlQuantifierDesugarer {

    private JmlQuantifierDesugarer() {
    }

    /**
     * The four binding kinds of {@code quantarray.md} §4.
     */
    public enum Kind {
        /** Plain classic binding {@code (\binder int x; …)} — already classic. */
        CLASSIC,
        /** Range generator {@code lo..hi}. */
        RANGE,
        /** Array generator ({@code E} array-typed, not an array access). */
        ARRAY,
        /** {@code Iterable} generator — formal model deferred, never desugared. */
        ITERABLE
    }

    /**
     * @return {@code true} iff {@code n} is a single-binding quantifier whose
     *     binding carries an iterable domain ({@code (\binder T x : E; …)}).
     */
    public static boolean isElementQuantifier(JmlQuantifiedExpr n) {
        if (n.getVariables().size() != 1) {
            return false;
        }
        return n.getVariables().get(0) instanceof JmlQuantifiedVariable jqv
                && jqv.getIterable().isPresent();
    }

    /**
     * Binding kind of a quantifier. Ranges are decided purely syntactically;
     * array vs {@code Iterable} needs the resolved domain type and falls back to
     * {@link Kind#ITERABLE} when resolution is unavailable.
     */
    public static Kind kindOf(JmlQuantifiedExpr n) {
        return isElementQuantifier(n)
                ? kindOf((JmlQuantifiedVariable) n.getVariables().get(0))
                : Kind.CLASSIC;
    }

    /**
     * Binding kind of a single quantified binding, resolving the domain type
     * through {@code domainType} when the kind is not syntactically decidable.
     */
    public static Kind kindOf(JmlQuantifiedVariable v, Function<Expression, Optional<ResolvedType>> domainType) {
        if (v.getIterable().isEmpty()) {
            return Kind.CLASSIC;
        }
        Expression iterable = v.getIterable().get();
        if (isRange(iterable)) {
            return Kind.RANGE;
        }
        Optional<ResolvedType> resolved = domainType.apply(iterable);
        return resolved.map(type -> type.isArray() ? Kind.ARRAY : Kind.ITERABLE).orElse(Kind.ITERABLE);
    }

    private static Kind kindOf(JmlQuantifiedVariable v) {
        return kindOf(v, DEFAULT_RESOLVER);
    }

    private static boolean isRange(Expression e) {
        return e instanceof BinaryExpr be && be.getOperator() == BinaryExpr.Operator.RANGE;
    }

    /**
     * Desugars an element-domain quantifier into the classic index form.
     *
     * @param n the quantifier
     * @return the classic form for {@link Kind#RANGE}/{@link Kind#ARRAY}, or
     *     {@link Optional#empty()} for classic, {@code Iterable} or unresolvable
     *     domains
     */
    public static Optional<JmlQuantifiedExpr> desugar(JmlQuantifiedExpr n) {
        return desugar(n, DEFAULT_RESOLVER);
    }

    /**
     * {@link #desugar(JmlQuantifiedExpr)} with an explicit domain-type resolver.
     *
     * @param n the quantifier
     * @param domainType resolves the type of a domain expression
     *     ({@link Optional#empty()} if unresolvable)
     * @return the classic form for {@link Kind#RANGE}/{@link Kind#ARRAY}, or
     *     {@link Optional#empty()} otherwise
     */
    public static Optional<JmlQuantifiedExpr> desugar(
            JmlQuantifiedExpr n, Function<Expression, Optional<ResolvedType>> domainType) {
        if (!isElementQuantifier(n)) {
            return Optional.empty();
        }
        JmlQuantifiedVariable v = (JmlQuantifiedVariable) n.getVariables().get(0);
        Expression iterable = v.getIterable().get();
        switch (kindOf(v, domainType)) {
            case RANGE:
                return Optional.of(desugarRange(n, v, (BinaryExpr) iterable));
            case ARRAY:
                return Optional.of(desugarArray(n, v, iterable));
            default:
                return Optional.empty();
        }
    }

    /**
     * {@code (\binder int x : lo..hi; F…)  ≡  (\binder int x; lo <= x && x < hi; F…)}
     */
    private static JmlQuantifiedExpr desugarRange(JmlQuantifiedExpr n, JmlQuantifiedVariable v, BinaryExpr range) {
        String name = v.getNameAsString();
        Expression guard = new BinaryExpr(
                new BinaryExpr(range.getLeft().clone(), new NameExpr(name), BinaryExpr.Operator.LESS_EQUALS),
                new BinaryExpr(new NameExpr(name), range.getRight().clone(), BinaryExpr.Operator.LESS),
                BinaryExpr.Operator.AND);

        NodeList<VariableDeclarator> variables = new NodeList<>();
        variables.add(new VariableDeclarator(rangeBoundType(v), name));
        return new JmlQuantifiedExpr(
                n.getTokenRange().orElse(null),
                n.getBinder(),
                variables,
                prependHead(n, guard));
    }

    private static Type rangeBoundType(JmlQuantifiedVariable v) {
        // `var` in a range domain means `int` (design §3.6); keep any declared type otherwise.
        if (v.getType() instanceof VarType) {
            return PrimitiveType.intType();
        }
        return v.getType().clone();
    }

    /**
     * {@code (\binder T x : a; F…)  ≡  (\binder int _i; 0 <= _i && _i < a.length; F[x := a[_i]])}
     */
    private static JmlQuantifiedExpr desugarArray(JmlQuantifiedExpr n, JmlQuantifiedVariable v, Expression array) {
        String index = freshIndex(n);
        Expression guard = new BinaryExpr(
                new BinaryExpr(new IntegerLiteralExpr("0"), new NameExpr(index), BinaryExpr.Operator.LESS_EQUALS),
                new BinaryExpr(
                        new NameExpr(index),
                        new FieldAccessExpr(array.clone(), "length"),
                        BinaryExpr.Operator.LESS),
                BinaryExpr.Operator.AND);

        NodeList<VariableDeclarator> variables = new NodeList<>();
        variables.add(new VariableDeclarator(PrimitiveType.intType(), index));

        ArrayAccessExpr access = new ArrayAccessExpr(array.clone(), new NameExpr(index));
        NodeList<Expression> expressions = new NodeList<>();
        expressions.add(guard);
        for (Expression e : n.getExpressions()) {
            expressions.add(substitute(e.clone(), v.getNameAsString(), access.clone()));
        }
        return new JmlQuantifiedExpr(n.getTokenRange().orElse(null), n.getBinder(), variables, expressions);
    }

    private static String freshIndex(JmlQuantifiedExpr n) {
        Set<String> used = new HashSet<>();
        for (Expression e : n.getExpressions()) {
            for (NameExpr ne : e.findAll(NameExpr.class)) {
                used.add(ne.getNameAsString());
            }
        }
        String base = "_i";
        String candidate = base;
        while (used.contains(candidate)) {
            candidate = "_" + candidate;
        }
        return candidate;
    }

    private static Expression substitute(Expression body, String name, Expression replacement) {
        // Only NameExpr nodes present in the original expression are replaced; each
        // replacement introduces new nodes, so the pre-collected matches stay hits-free
        // (the domain may not reference the bound variable — validated by JmlProcessor).
        for (NameExpr match : body.findAll(NameExpr.class, ne -> ne.getNameAsString().equals(name))) {
            match.replace(replacement.clone());
        }
        return body;
    }

    private static NodeList<Expression> prependHead(JmlQuantifiedExpr n, Expression head) {
        NodeList<Expression> expressions = new NodeList<>();
        expressions.add(head);
        for (Expression e : n.getExpressions()) {
            expressions.add(e.clone());
        }
        return expressions;
    }

    private static final Function<Expression, Optional<ResolvedType>> DEFAULT_RESOLVER = expression -> {
        try {
            return Optional.ofNullable(expression.calculateResolvedType());
        } catch (RuntimeException ex) {
            // No symbol resolver configured (or unresolvable name) — the kind cannot be
            // decided; callers must fall back.
            return Optional.empty();
        }
    };
}
