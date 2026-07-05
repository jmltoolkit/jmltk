/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.utils;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.truth.Truth;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.javaparser.utils.CodeGenerationUtils.classLoaderRoot;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class SymbolSolverCollectionStrategyTest {

    private final Path root = classLoaderRoot(SymbolSolverCollectionStrategyTest.class)
            .resolve("../../../../../javaparser-core")
            .normalize();
    private final ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(root);

    @Test
    void resolveExpressions() throws IOException {
        SourceRoot sourceRoot =
                projectRoot.getSourceRoot(root.resolve("src/main/java")).get();
        AtomicInteger unresolved = new AtomicInteger();
        for (ParseResult<CompilationUnit> parseResult : sourceRoot.tryToParse()) {
            parseResult.ifSuccessful(compilationUnit -> {
                for (MethodDeclaration expr : compilationUnit.findAll(MethodDeclaration.class)) {
                    try {
                        expr.resolve().getQualifiedSignature();
                    } catch (UnsupportedOperationException e) {
                        // not supported operation, just skip
                    } catch (Exception e) {
                        unresolved.getAndIncrement();
                        Log.error(
                                e,
                                "Unable to resolve %s from %s",
                                () -> expr,
                                () -> compilationUnit.getStorage().get().getPath());
                    }
                }
            });
        }
        // not too many MethodDeclarations should be unresolved
        assertTrue(unresolved.get() < 10);
    }

    @Test
    @Disabled("weigl: There are runtime issues (heap space) with this approach.")
    void resolveMultiSourceRoots() {
        var relativeRootDir = List.of(
                "src/main/java-templates",
                "src/main/java",
                "src/main/javacc-support",
                "target/generated-sources/javacc",
                "target/generated-sources/java-templates",
                "src/main/java-templates");
        Path mainDirectory = classLoaderRoot(SymbolSolverCollectionStrategyTest.class)
                .resolve("../../../../../javaparser-core")
                .normalize();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(mainDirectory);
        List<com.github.javaparser.utils.SourceRoot> sourceRoots = projectRoot.getSourceRoots();
        // get all source root directories
        List<String> roots = projectRoot.getSourceRoots().stream()
                .map(s -> s.getRoot().toString())
                .collect(Collectors.toList());
        // verify each member of the list
        var relativeRootDirPaths = relativeRootDir.stream()
                .map(rrd -> Paths.get(mainDirectory.toString(), rrd))
                .map(Path::toString)
                .toList();
        Truth.assertThat(roots).containsAnyIn(relativeRootDirPaths);
    }
}
