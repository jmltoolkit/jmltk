/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts.jml;

import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.AbstractJavaParserContext;

/**
 * Context for JmlSimpleExprClause (ensures, requires, etc.)
 * This context delegates symbol resolution to the parent contract context,
 * which handles forall binder variables and old clause declarations.
 *
 * @author Alexander Weigl
 * @version 1 (08.07.26)
 */
public class JmlSimpleExprClauseContext extends AbstractJavaParserContext<JmlSimpleExprClause> {
    public JmlSimpleExprClauseContext(JmlSimpleExprClause wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        // Delegate to parent (contract) context to resolve class members and forall variables
        return super.solveSymbol(name);
    }
}
