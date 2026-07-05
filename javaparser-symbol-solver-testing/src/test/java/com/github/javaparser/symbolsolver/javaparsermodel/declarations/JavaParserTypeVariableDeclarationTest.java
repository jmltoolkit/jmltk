/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.logic.AbstractTypeDeclaration;
import com.github.javaparser.symbolsolver.logic.AbstractTypeDeclarationTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JavaParserTypeVariableDeclarationTest extends AbstractTypeDeclarationTest {

    @Override
    public JavaParserTypeVariableDeclaration createValue() {
        CompilationUnit cu = StaticJavaParser.parse("class A<T>{}");
        TypeParameter typeParameter = cu.findFirst(TypeParameter.class).get();
        ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();
        return new JavaParserTypeVariableDeclaration(typeParameter, typeSolver);
    }

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(safeCast(associableToAST, JavaParserTypeVariableDeclaration.class)
                .getWrappedNode());
    }

    @Override
    public boolean isFunctionalInterface(AbstractTypeDeclaration typeDeclaration) {
        return false;
    }

    @Test
    void getWrappedNodeShouldNotBeNull() {
        assertNotNull(createValue().getWrappedNode());
    }

    @Nested
    class TestGetAncestorAncestorsMethod {

        private final JavaParser parser = new JavaParser();
        private final ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();

        private void testGetAncestorWith(Iterable<String> expectedTypes, String sourceCode) {
            CompilationUnit cu = parser.parse(sourceCode).getResult().orElseThrow(AssertionError::new);
            TypeParameter typeParameter = Navigator.demandNodeOfGivenClass(cu, TypeParameter.class);
            JavaParserTypeVariableDeclaration parserTypeParameter =
                    new JavaParserTypeVariableDeclaration(typeParameter, typeSolver);
            assertEquals(
                    expectedTypes,
                    parserTypeParameter.getAncestors().stream()
                            .map(ResolvedReferenceType::getQualifiedName)
                            .sorted()
                            .collect(Collectors.toList()));
        }

        @Test
        void withoutBound() {
            String sourceCode = "class A<T> {}";
            testGetAncestorWith(Collections.singletonList("java.lang.Object"), sourceCode);
        }

        @Test
        void withObjectBound() {
            String sourceCode = "class A<T extends Object> {}";
            testGetAncestorWith(Collections.singletonList("java.lang.Object"), sourceCode);
        }

        @Test
        void withMultipleBounds() {
            String sourceCode = "class A {} interface B {} class C<T extends A & B> {}";
            testGetAncestorWith(Arrays.asList("A", "B"), sourceCode);
        }
    }
}
