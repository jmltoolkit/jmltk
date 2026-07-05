/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.TypeSolver;
import java.util.Collections;
import java.util.List;

/**
 * @author Federico Tomassetti
 */
public class VariableDeclaratorContext extends AbstractJavaParserContext<VariableDeclarator> {

    public VariableDeclaratorContext(VariableDeclarator wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<VariableDeclarator> localVariablesExposedToChild(Node child) {
        if (wrappedNode.getInitializer().isPresent()
                && wrappedNode.getInitializer().get() == child) {
            return Collections.singletonList(wrappedNode);
        }

        return Collections.emptyList();
    }
}
