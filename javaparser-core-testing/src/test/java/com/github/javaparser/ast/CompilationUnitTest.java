/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.utils.CodeGenerationUtils.mavenModuleRoot;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class CompilationUnitTest {
    @Test
    void issue578TheFirstCommentIsWithinTheCompilationUnit() {
        CompilationUnit compilationUnit =
                parse("// This is my class, with my comment\n" + "class A {\n" + "    static int a;\n" + "}");

        assertEquals(1, compilationUnit.getAllContainedComments().size());
    }

    @Test
    void testGetSourceRoot() throws IOException {
        Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                .resolve(Paths.get("src", "test", "resources"))
                .normalize();
        Path testFile = sourceRoot.resolve(Paths.get("com", "github", "javaparser", "storage", "Z.java"));

        CompilationUnit cu = parse(testFile);
        Path sourceRoot1 = cu.getStorage().get().getSourceRoot();
        assertEquals(sourceRoot, sourceRoot1);
    }

    @Test
    void testGetSourceRootWithBadPackageDeclaration() {
        assertThrows(RuntimeException.class, () -> {
            Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                    .resolve(Paths.get("src", "test", "resources"))
                    .normalize();
            Path testFile = sourceRoot.resolve(Paths.get("com", "github", "javaparser", "storage", "A.java"));
            CompilationUnit cu = parse(testFile);
            cu.getStorage().get().getSourceRoot();
        });
    }

    @Test
    void testGetSourceRootInDefaultPackage() throws IOException {
        Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                .resolve(Paths.get("src", "test", "resources", "com", "github", "javaparser", "storage"))
                .normalize();
        Path testFile = sourceRoot.resolve(Paths.get("B.java"));

        CompilationUnit cu = parse(testFile);
        Path sourceRoot1 = cu.getStorage().get().getSourceRoot();
        assertEquals(sourceRoot, sourceRoot1);
    }

    @Test
    void testGetPrimaryTypeName() throws IOException {
        Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                .resolve(Paths.get("src", "test", "resources"))
                .normalize();
        Path testFile = sourceRoot.resolve(Paths.get("com", "github", "javaparser", "storage", "PrimaryType.java"));
        CompilationUnit cu = parse(testFile);

        assertEquals("PrimaryType", cu.getPrimaryTypeName().get());
    }

    @Test
    void testNoPrimaryTypeName() {
        CompilationUnit cu = parse("class PrimaryType{}");

        assertFalse(cu.getPrimaryTypeName().isPresent());
    }

    @Test
    void testGetPrimaryType() throws IOException {
        Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                .resolve(Paths.get("src", "test", "resources"))
                .normalize();
        Path testFile = sourceRoot.resolve(Paths.get("com", "github", "javaparser", "storage", "PrimaryType.java"));
        CompilationUnit cu = parse(testFile);

        assertEquals("PrimaryType", cu.getPrimaryType().get().getNameAsString());
    }

    @Test
    void testNoPrimaryType() throws IOException {
        Path sourceRoot = mavenModuleRoot(CompilationUnitTest.class)
                .resolve(Paths.get("src", "test", "resources"))
                .normalize();
        Path testFile = sourceRoot.resolve(Paths.get("com", "github", "javaparser", "storage", "PrimaryType2.java"));
        CompilationUnit cu = parse(testFile);

        assertFalse(cu.getPrimaryType().isPresent());
    }
}
