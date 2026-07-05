/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static com.github.javaparser.StaticJavaParser.parse;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.MethodContext;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Issue276Test extends AbstractResolutionTest {

    @Test
    void testSolveStaticallyImportedMemberType() throws IOException {
        CompilationUnit cu = parse(adaptPath("src/test/resources/issue276/foo/C.java"));
        ClassOrInterfaceDeclaration cls = Navigator.demandClassOrInterface(cu, "C");
        TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(adaptPath("src/test/resources/issue276"), new LeanParserConfiguration()));
        List<MethodDeclaration> methods = cls.findAll(MethodDeclaration.class);
        boolean isSolved = false;
        for (MethodDeclaration method : methods) {
            if (method.getNameAsString().equals("overrideMe")) {
                MethodContext context = new MethodContext(method, typeSolver);
                isSolved = context.solveType("FindMeIfYouCan").isSolved();
            }
        }
        Assertions.assertTrue(isSolved);
    }
}
