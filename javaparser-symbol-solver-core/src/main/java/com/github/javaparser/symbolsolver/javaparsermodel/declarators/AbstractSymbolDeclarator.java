/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.SymbolDeclarator;
import com.github.javaparser.resolution.TypeSolver;

/**
 * @author Federico Tomassetti
 */
public abstract class AbstractSymbolDeclarator<N extends Node> implements SymbolDeclarator {

    protected N wrappedNode;
    protected TypeSolver typeSolver;

    public AbstractSymbolDeclarator(N wrappedNode, TypeSolver typeSolver) {
        this.wrappedNode = wrappedNode;
        this.typeSolver = typeSolver;
    }
}
