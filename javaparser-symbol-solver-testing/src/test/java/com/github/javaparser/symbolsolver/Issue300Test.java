/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Issue300Test extends AbstractResolutionTest {

    @Test
    void fieldAccessIssue() throws IOException {
        Path pathToSourceFile = adaptPath("src/test/resources/issue300/Issue300.java");
        CompilationUnit cu = parse(pathToSourceFile);

        final FieldAccessExpr fieldAccess = Navigator.demandNodeOfGivenClass(cu, FieldAccessExpr.class);
        assertNotNull(fieldAccess);

        TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(adaptPath("src/test/resources/issue300"), new LeanParserConfiguration()));
        final JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        final SymbolReference<? extends ResolvedValueDeclaration> ref = javaParserFacade.solve(fieldAccess);
        assertEquals(
                ResolvedPrimitiveType.INT,
                ref.getCorrespondingDeclaration().getType().asPrimitive());
    }
}
