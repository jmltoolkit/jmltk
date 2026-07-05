/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CompilationUnitContext.solveType(String name, TypeSolver typeSolver) checks package and imports in wrong order.
 * @see <a href="https://github.com/javaparser/javaparser/issues/1526">https://github.com/javaparser/javaparser/issues/1526</a>
 */
public class Issue1526Test extends AbstractSymbolResolutionTest {

    private final Path testRoot = adaptPath("src/test/resources/issue1526");
    private final Path rootCompiles = testRoot.resolve("compiles");
    private final Path rootErrors = testRoot.resolve("errors");

    @Test
    public void givenImport_whenCompiles_expectPass() throws IOException {
        Path root = rootCompiles;
        Path file = rootCompiles.resolve("a/b/c/ExampleClass.java");

        assertDoesNotThrow(() -> {
            doTest(root, file);
        });
    }

    @Test
    public void givenImportCommentOut_whenCompiles_expectFail() throws IOException {
        Path root = rootErrors;
        Path file = rootErrors.resolve("a/b/c/ExampleClass.java");

        assertThrows(UnsolvedSymbolException.class, () -> {
            doTest(root, file);
        });
    }

    private void doTest(Path root, Path file) throws IOException {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(root, new LeanParserConfiguration()));

        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));

        ParseResult<CompilationUnit> cu = javaParser.parse(file);
        assumeTrue(cu.isSuccessful(), "the file should compile -- errors are expected when attempting to resolve.");

        cu.getResult().get().findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
            methodCallExpr.resolve();
            methodCallExpr.calculateResolvedType();
        });
    }
}
