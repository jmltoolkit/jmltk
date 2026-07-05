/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclarationTest;
import com.github.javaparser.symbolsolver.logic.AbstractTypeDeclaration;
import com.github.javaparser.symbolsolver.logic.AbstractTypeDeclarationTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.Optional;

class JavaParserTypeParameterTest extends AbstractTypeDeclarationTest implements ResolvedTypeParameterDeclarationTest {

    @Override
    public JavaParserTypeParameter createValue() {
        TypeParameter typeParameter = StaticJavaParser.parse("class A<T> {}")
                .findFirst(TypeParameter.class)
                .get();
        TypeSolver typeSolver = new ReflectionTypeSolver();
        return new JavaParserTypeParameter(typeParameter, typeSolver);
    }

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(
                safeCast(associableToAST, JavaParserTypeParameter.class).getWrappedNode());
    }

    @Override
    public boolean isFunctionalInterface(AbstractTypeDeclaration typeDeclaration) {
        return false;
    }
}
