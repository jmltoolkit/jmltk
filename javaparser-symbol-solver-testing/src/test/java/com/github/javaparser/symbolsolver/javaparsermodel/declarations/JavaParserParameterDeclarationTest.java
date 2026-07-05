/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclarationTest;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.Optional;

class JavaParserParameterDeclarationTest implements ResolvedParameterDeclarationTest {

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(
                safeCast(associableToAST, JavaParserParameterDeclaration.class).getWrappedNode());
    }

    @Override
    public JavaParserParameterDeclaration createValue() {
        Parameter parameter = StaticJavaParser.parseMethodDeclaration("<T> void a(T a) {}")
                .findFirst(Parameter.class)
                .get();
        ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();
        return new JavaParserParameterDeclaration(parameter, typeSolver);
    }

    @Override
    public String getCanonicalNameOfExpectedType(ResolvedValueDeclaration resolvedDeclaration) {
        return "T";
    }
}
