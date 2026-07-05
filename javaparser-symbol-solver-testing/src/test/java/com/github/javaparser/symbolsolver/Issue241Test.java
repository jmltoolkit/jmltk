/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue241Test extends AbstractResolutionTest {

    @Test
    void testSolveStaticallyImportedMemberType() {
        Path src = adaptPath("src/test/resources");
        TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(), new JavaParserTypeSolver(src, new LeanParserConfiguration()));

        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);

        CompilationUnit cu = parseSample("Issue241");
        ClassOrInterfaceDeclaration cls = Navigator.demandClassOrInterface(cu, "Main");
        VariableDeclarator v = Navigator.demandVariableDeclaration(cls, "foo").get();

        Type t = v.getType();
        ResolvedType t2 = javaParserFacade.convert(t, t);
        String typeName = t2.asReferenceType().getQualifiedName();

        assertEquals("issue241.TypeWithMemberType.MemberInterface", typeName);
    }
}
