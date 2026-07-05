/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javassistmodel;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavassistTypeDeclarationAdapterTest extends AbstractResolutionTest {

    @ParameterizedTest(name = "Given {0} is expected {1}")
    @ArgumentsSource(GetAncestorsProvider.class)
    void testGetAncestors(String ctClass, List<String> expectedAncestors) throws NotFoundException, IOException {
        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        CtClass clazz = new ClassPool(true).getCtClass(ctClass);

        ResolvedReferenceTypeDeclaration declaration = JavassistFactory.toTypeDeclaration(clazz, typeSolver);
        JavassistTypeDeclarationAdapter adapter = new JavassistTypeDeclarationAdapter(clazz, typeSolver, declaration);

        List<ResolvedReferenceType> resultAncestors = adapter.getAncestors(false);
        assertEquals(
                expectedAncestors,
                resultAncestors.stream()
                        .map(ResolvedReferenceType::getQualifiedName)
                        .collect(Collectors.toList()));
    }

    /**
     * Class which provider arguments to be tested in {@link JavassistTypeDeclarationAdapterTest#testGetAncestors}
     */
    static class GetAncestorsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // Node
                    Arguments.of(
                            "com.github.javaparser.ast.Node",
                            asList(
                                    "java.lang.Object",
                                    "java.lang.Cloneable",
                                    "com.github.javaparser.HasParentNode",
                                    "com.github.javaparser.ast.visitor.Visitable",
                                    "com.github.javaparser.ast.nodeTypes.NodeWithRange",
                                    "com.github.javaparser.ast.nodeTypes.NodeWithTokenRange")),
                    // Expression
                    Arguments.of(
                            "com.github.javaparser.ast.expr.Expression",
                            singletonList("com.github.javaparser.ast.Node")),
                    // Annotation.class
                    Arguments.of("com.github.javaparser.ParseStart", singletonList("java.lang.Object")),
                    // SlowTest Annotation
                    Arguments.of(
                            "com.github.javaparser.SlowTest",
                            asList("java.lang.Object", "java.lang.annotation.Annotation")));
        }
    }
}
