/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserTypeParameter;
import java.util.List;

/**
 * Limited version of ClassOrInterfaceDeclarationContext that only resolves type parameters for use by
 * extends and implements part of declaration.
 */
public class ClassOrInterfaceDeclarationExtendsContext extends AbstractJavaParserContext<ClassOrInterfaceDeclaration> {
    public ClassOrInterfaceDeclarationExtendsContext(ClassOrInterfaceDeclaration wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<ResolvedTypeDeclaration> solveType(String name, List<ResolvedType> typeArguments) {
        for (TypeParameter typeParameter : wrappedNode.getTypeParameters()) {
            if (typeParameter.getName().getId().equals(name)) {
                return SymbolReference.solved(new JavaParserTypeParameter(typeParameter, typeSolver));
            }
        }

        return super.solveType(name, typeArguments);
    }
}
