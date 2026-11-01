/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedVariable;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import java.util.List;
import java.util.Optional;

import static com.github.javaparser.resolution.Navigator.demandParentNode;
import static com.github.javaparser.resolution.TypeSolver.JAVA_LANG_OBJECT;

/**
 * @author Federico Tomassetti
 */
public class JmlJavaParserQuantifiedVariableDeclaration implements ResolvedValueDeclaration {

    private VariableDeclarator variableDeclarator;
    private JmlQuantifiedExpr wrappedNode;
    private TypeSolver typeSolver;

    public JmlJavaParserQuantifiedVariableDeclaration(VariableDeclarator variableDeclarator, TypeSolver typeSolver) {
        if (typeSolver == null) {
            throw new IllegalArgumentException("typeSolver should not be null");
        }
        this.variableDeclarator = variableDeclarator;
        this.typeSolver = typeSolver;
        if (!(demandParentNode(variableDeclarator) instanceof JmlQuantifiedExpr e)) {
            throw new IllegalStateException(
                    demandParentNode(variableDeclarator).getClass().getCanonicalName());
        }
        this.wrappedNode = e;
    }

    @Override
    public ResolvedType getType() {
        if (variableDeclarator instanceof JmlQuantifiedVariable jqv) {
            Optional<Expression> iterable = jqv.getIterable();
            if (iterable.isPresent()) {
                return inferElementType(jqv, iterable.get());
            }
        }
        return JavaParserFacade.get(typeSolver).convert(variableDeclarator.getType(), wrappedNode);
    }

    /**
     * The type of a quantified binding with an iterable domain is the domain's element
     * type — the same rule {@code VarType.convertToUsage} applies to enhanced-for
     * variables: a range {@code 0..n} yields {@code int}, an array the component type
     * (the element of {@code T[][]} is {@code T[]}), and an {@code Iterable} (or any
     * generic reference type) its first type argument, with raw domains degrading to
     * {@code Object}. This is what makes {@code (\forall var e : a; ...)} and
     * {@code (\forall T e : c; ...)} resolvable. If the domain cannot be resolved in
     * the current context, the declared type is used as a fallback.
     */
    private ResolvedType inferElementType(JmlQuantifiedVariable binding, Expression domain) {
        if (domain instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) domain;
            if (binary.getOperator() == BinaryExpr.Operator.RANGE) {
                return ResolvedPrimitiveType.INT;
            }
        }
        try {
            ResolvedType domainType = JavaParserFacade.get(typeSolver).getType(domain);
            if (domainType.isArray()) {
                return domainType.asArrayType().getComponentType();
            }
            if (domainType.isReferenceType()) {
                List<ResolvedType> typeArguments =
                        domainType.asReferenceType().typeParametersMap().getTypes();
                if (!typeArguments.isEmpty()) {
                    return typeArguments.get(0);
                }
                return ReferenceTypeImpl.undeterminedParameters(typeSolver.solveType(JAVA_LANG_OBJECT));
            }
        } catch (UnsolvedSymbolException e) {
            // The domain cannot be resolved in the current context — fall back to the declared type.
        }
        return JavaParserFacade.get(typeSolver).convert(binding.getType(), wrappedNode);
    }

    @Override
    public String getName() {
        return variableDeclarator.getName().getId();
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    /**
     * Returns the JavaParser node associated with this JavaParserFieldDeclaration.
     *
     * @return A visitable JavaParser node wrapped by this object.
     */
    public JmlQuantifiedExpr getWrappedNode() {
        return wrappedNode;
    }

    public VariableDeclarator getVariableDeclarator() {
        return variableDeclarator;
    }

    @Override
    public String toString() {
        return "JavaParserVariableDeclaration{" + getName() + "}";
    }

    @Override
    public Optional<Node> toAst() {
        return Optional.of(wrappedNode);
    }
}
