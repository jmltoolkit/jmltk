/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue1456Test extends AbstractResolutionTest {

    @Test
    void fieldAccessIssue() throws IOException {
        Path rootSourceDir = adaptPath("src/test/resources/issue1456");
        Path pathToSourceFile = adaptPath(rootSourceDir.toString() + "/bar/A.java");

        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(new JavaParserTypeSolver(rootSourceDir.toFile())));
        StaticJavaParser.setConfiguration(config);

        CompilationUnit cu = StaticJavaParser.parse(pathToSourceFile);

        ClassOrInterfaceDeclaration cid =
                cu.findFirst(ClassOrInterfaceDeclaration.class).get();
        ResolvedTypeDeclaration rtd = cid.resolve();
        List<ResolvedReferenceType> ancestors = rtd.asClass().getAncestors();
        assertEquals("foo.A", ancestors.get(0).describe());
    }
}
