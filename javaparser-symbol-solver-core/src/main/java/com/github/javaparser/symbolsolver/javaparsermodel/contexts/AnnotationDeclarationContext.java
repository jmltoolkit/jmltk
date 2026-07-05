/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnnotationDeclaration;
import java.util.List;

/**
 * @author Takeshi D. Itoh
 */
public class AnnotationDeclarationContext extends AbstractJavaParserContext<AnnotationDeclaration> {

    private JavaParserTypeDeclarationAdapter javaParserTypeDeclarationAdapter;

    public AnnotationDeclarationContext(AnnotationDeclaration wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
        this.javaParserTypeDeclarationAdapter =
                new JavaParserTypeDeclarationAdapter(wrappedNode, typeSolver, getDeclaration(), this);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        if (typeSolver == null) throw new IllegalArgumentException();

        if (this.getDeclaration().hasField(name)) {
            return SymbolReference.solved(this.getDeclaration().getField(name));
        }

        // then to parent
        return solveSymbolInParentContext(name);
    }

    @Override
    public SymbolReference<ResolvedTypeDeclaration> solveType(String name, List<ResolvedType> resolvedTypes) {
        return javaParserTypeDeclarationAdapter.solveType(name, resolvedTypes);
    }

    @Override
    public SymbolReference<ResolvedMethodDeclaration> solveMethod(
            String name,
            List<ResolvedType> argumentsTypes,
            boolean staticOnly,
            ResolvedReferenceTypeDeclaration invocationContext) {
        return javaParserTypeDeclarationAdapter.solveMethod(name, argumentsTypes, staticOnly, invocationContext);
    }

    ///
    /// Private methods
    ///

    private ResolvedReferenceTypeDeclaration getDeclaration() {
        return new JavaParserAnnotationDeclaration(this.wrappedNode, typeSolver);
    }
}
