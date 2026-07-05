/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedTypePatternDeclarationTest;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeAll;

import java.util.Optional;

class JavaParserTypePatternDeclarationTest implements ResolvedTypePatternDeclarationTest {

    @BeforeAll
    public static void setup() {
        StaticJavaParser.getConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14_PREVIEW);
    }

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(safeCast(associableToAST, JavaParserTypePatternDeclaration.class)
                .getWrappedNode());
    }

    @Override
    public JavaParserTypePatternDeclaration createValue() {
        TypePatternExpr wrappedNode = StaticJavaParser.parse("class A {a() {if (object instanceof String d) return;}}")
                .findFirst(TypePatternExpr.class)
                .get();
        ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();
        return new JavaParserTypePatternDeclaration(wrappedNode, typeSolver);
    }

    @Override
    public String getCanonicalNameOfExpectedType(ResolvedValueDeclaration resolvedDeclaration) {
        return String.class.getCanonicalName();
    }
}
