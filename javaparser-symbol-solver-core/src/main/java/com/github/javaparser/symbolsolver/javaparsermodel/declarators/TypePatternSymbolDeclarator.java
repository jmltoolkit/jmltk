/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarators;

import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Roger Howell
 */
public class TypePatternSymbolDeclarator extends AbstractSymbolDeclarator<TypePatternExpr> {

    public TypePatternSymbolDeclarator(TypePatternExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<ResolvedValueDeclaration> getSymbolDeclarations() {
        List<ResolvedValueDeclaration> symbols = new LinkedList<>();
        symbols.add(JavaParserSymbolDeclaration.patternVar(wrappedNode, typeSolver));
        return symbols;
    }
}
