/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import static com.github.javaparser.resolution.Navigator.demandParentNode;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import java.util.Optional;

/**
 * @author Federico Tomassetti
 */
public class JavaParserVariableDeclaration implements ResolvedValueDeclaration {

    private VariableDeclarator variableDeclarator;
    private VariableDeclarationExpr wrappedNode;
    private TypeSolver typeSolver;

    public JavaParserVariableDeclaration(VariableDeclarator variableDeclarator, TypeSolver typeSolver) {
        if (typeSolver == null) {
            throw new IllegalArgumentException("typeSolver should not be null");
        }
        this.variableDeclarator = variableDeclarator;
        this.typeSolver = typeSolver;
        if (!(demandParentNode(variableDeclarator) instanceof VariableDeclarationExpr)) {
            throw new IllegalStateException(
                    demandParentNode(variableDeclarator).getClass().getCanonicalName());
        }
        this.wrappedNode = (VariableDeclarationExpr) demandParentNode(variableDeclarator);
    }

    @Override
    public ResolvedType getType() {
        return JavaParserFacade.get(typeSolver).convert(variableDeclarator.getType(), wrappedNode);
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
    public VariableDeclarationExpr getWrappedNode() {
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
