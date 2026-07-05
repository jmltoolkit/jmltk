/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Issue3099Test extends AbstractResolutionTest {

    @Test
    void illegalArgumentExceptionWhenSolvingName() throws IOException {

        // Setup symbol solver
        JavaParserTypeSolver javaParserTypeSolver =
                new JavaParserTypeSolver(adaptPath("src/test/resources/issue3099/"));
        StaticJavaParser.getConfiguration()
                .setSymbolResolver(
                        new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver(), javaParserTypeSolver)));

        // Parse the File
        Path filePath = adaptPath("src/test/resources/issue3099/com/example/Beta.java");
        CompilationUnit cu = StaticJavaParser.parse(filePath);

        // Get the expected inner class
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        assertEquals(2, classes.size());
        ResolvedReferenceTypeDeclaration innerInterface = classes.get(1).resolve();
        assertTrue(innerInterface.isInterface());

        // Check if the value is present
        Optional<ResolvedReferenceType> resolvedType = cu.findFirst(VariableDeclarator.class)
                .map(VariableDeclarator::getType)
                .map(Type::resolve)
                .filter(ResolvedType::isReferenceType)
                .map(ResolvedType::asReferenceType);
        assertTrue(resolvedType.isPresent());
        assertEquals(innerInterface, resolvedType.get().getTypeDeclaration().orElse(null));
    }
}
