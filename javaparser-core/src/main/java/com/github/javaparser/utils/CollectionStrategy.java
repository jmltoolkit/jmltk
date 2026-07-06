/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;

/**
 * A strategy for discovering the structure of a project.
 * Implementations could read a pom.xml, a Gradle build file, a makefile...
 */
public interface CollectionStrategy {

    ParserConfiguration getParserConfiguration();

    ProjectRoot collect(Path path);

    default Optional<Path> getRoot(Path file) {
        try {
            final JavaParser javaParser = new JavaParser(getParserConfiguration());
            final ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
            if (parseResult.isSuccessful()) {
                if (parseResult.getResult().isPresent()) {
                    final Optional<CompilationUnit.Storage> storage = parseResult.getResult().flatMap(CompilationUnit::getStorage);
                    if (storage.isPresent()) {
                        if ("module-info.java".equals(storage.get().getFileName())) {
                            // module-info.java is useless for finding the source root, since it can be placed in any
                            // directory.
                            return Optional.empty();
                        }
                        return storage.map(CompilationUnit.Storage::getSourceRoot);
                    }
                    Log.info("Storage information not present -- an issue with providing a string rather than file reference?");
                }
                Log.info("Parse result not present");
            }
            Log.info("Parsing was not successful.");
            Log.info("There were (%d) problems parsing file: %s", () -> parseResult.getProblems().size(), parseResult::getProblems);
        } catch (ParseProblemException e) {
            Log.info("Problem parsing file %s : %s", () -> file, () -> e.getLocalizedMessage());
        } catch (RuntimeException e) {
            Log.info("Could not parse file %s : %s", () -> file, () -> e.getLocalizedMessage());
        } catch (IOException e) {
            Log.info("Could not read file %s : %s", () -> file, () -> e.getLocalizedMessage());
        }
        return Optional.empty();
    }

    default PathMatcher getPathMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher(pattern);
    }
}
