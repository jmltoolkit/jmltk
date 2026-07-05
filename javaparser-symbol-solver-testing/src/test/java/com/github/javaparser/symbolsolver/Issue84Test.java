/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

class Issue84Test extends AbstractResolutionTest {

    @Test
    void variadicIssue() {
        CompilationUnit cu = parseSample("Issue84");
        final MethodCallExpr methodCall =
                Navigator.findMethodCall(cu, "variadicMethod").get();

        final JavaParserFacade javaParserFacade = JavaParserFacade.get(new ReflectionTypeSolver());
        final ResolvedType type = javaParserFacade.getType(methodCall);
        assertEquals(String.class.getCanonicalName(), type.asReferenceType().getQualifiedName());
    }
}
