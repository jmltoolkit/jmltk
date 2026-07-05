/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.resolution.TypeSolver;
import java.util.Collections;
import java.util.List;

/**
 * @author Federico Tomassetti
 */
public class MethodContext extends AbstractMethodLikeDeclarationContext<MethodDeclaration> {

    ///
    /// Constructors
    ///

    public MethodContext(MethodDeclaration wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<Parameter> parametersExposedToChild(Node child) {
        // TODO/FIXME: Presumably the parameters must be exposed to all children and their descendants, not just the
        // direct child?
        if (wrappedNode.getBody().isPresent() && child == wrappedNode.getBody().get()) {
            return wrappedNode.getParameters();
        }
        return Collections.emptyList();
    }
}
