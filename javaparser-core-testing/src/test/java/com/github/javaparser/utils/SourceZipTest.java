/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceZipTest {

    private final Path testDir = CodeGenerationUtils.mavenModuleRoot(SourceZipTest.class)
            .resolve(Paths.get(
                    "..",
                    "javaparser-core-testing",
                    "src",
                    "test",
                    "resources",
                    "com",
                    "github",
                    "javaparser",
                    "source_zip"))
            .normalize();

    @Test
    void parseTestDirectory() throws IOException {
        SourceZip sourceZip = new SourceZip(testDir.resolve("test.zip"));
        List<Pair<Path, ParseResult<CompilationUnit>>> results = sourceZip.parse();
        assertEquals(3, results.size());
        List<CompilationUnit> units = new ArrayList<>();
        for (Pair<Path, ParseResult<CompilationUnit>> pr : results) {
            units.add(pr.b.getResult().get());
        }
        assertTrue(units.stream().noneMatch(unit -> unit.getTypes().isEmpty()));
    }

    @Test
    void parseTestDirectoryWithCallback() throws IOException {
        SourceZip sourceZip = new SourceZip(testDir.resolve("test.zip"));
        List<Pair<Path, ParseResult<CompilationUnit>>> results = new ArrayList<>();

        sourceZip.parse((path, result) -> results.add(new Pair<>(path, result)));

        assertEquals(3, results.size());
        List<CompilationUnit> units = new ArrayList<>();
        for (Pair<Path, ParseResult<CompilationUnit>> pr : results) {
            units.add(pr.b.getResult().get());
        }
        assertTrue(units.stream().noneMatch(unit -> unit.getTypes().isEmpty()));
    }

    @Test
    void dirAsZipIsNotAllowed() {
        assertThrows(IOException.class, () -> new SourceZip(testDir.resolve("test")).parse());
    }

    @Test
    void fileAsZipIsNotAllowed() {
        assertThrows(IOException.class, () -> new SourceZip(testDir.resolve("test.txt")).parse());
    }
}
