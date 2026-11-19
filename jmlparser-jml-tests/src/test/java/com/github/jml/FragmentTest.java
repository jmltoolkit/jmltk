/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Providers;
import com.github.javaparser.ast.jml.ArbitraryNodeContainer;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Weigl
 * @version 1 (12/9/21)
 */
class FragmentTest {

    /**
     * Fragments that fail at baseline for known-unsupported JML/KeY constructs. This is an explicit
     * manifest rather than a message-substring heuristic so that unrelated diagnostic changes do not
     * silently change which tests run.
     */
    private static final Set<String> KNOWN_UNSUPPORTED = loadKnownUnsupported();

    private static Set<String> loadKnownUnsupported() {
        try (InputStream in = FragmentTest.class.getResourceAsStream("/fragments/known-unsupported.txt")) {
            Objects.requireNonNull(in, "fragments/known-unsupported.txt not found");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertParsedOrKnownUnsupported(File file, ParseResult<ArbitraryNodeContainer> result) {
        if (KNOWN_UNSUPPORTED.contains(file.getName())) {
            if (result.isSuccessful()) {
                Assertions.fail(
                        "Fragment now parses; remove it from fragments/known-unsupported.txt: " + file.getName());
            }
            Assumptions.assumeTrue(false, "known unsupported: " + file.getName());
        }
        result.getProblems().forEach(it -> System.out.println(it.getMessage()));
        if (!result.isSuccessful()) {
            System.out.println(file.getAbsolutePath());
        }
        Assertions.assertTrue(result.isSuccessful());
    }

    @TestFactory
    Stream<DynamicTest> testStatementLevel() {
        return files().filter(it -> it.getName().startsWith("stmt"))
                .map(it -> DynamicTest.dynamicTest(it.getName(), () -> testStatementLevel(it)));
    }

    private void testStatementLevel(File f) throws FileNotFoundException {
        JavaParser jp = new JavaParser();
        ParseResult<ArbitraryNodeContainer> r = jp.parseJmlMethodLevel(Providers.provider(f));
        assertParsedOrKnownUnsupported(f, r);
    }

    @TestFactory
    Stream<DynamicTest> testClassLevel() {
        return files().filter(it -> it.getName().startsWith("decl"))
                .map(it -> DynamicTest.dynamicTest(it.getName(), () -> testClassLevel(it)));
    }

    private void testClassLevel(File f) throws FileNotFoundException {
        JavaParser jp = new JavaParser();
        ParseResult<ArbitraryNodeContainer> r = jp.parseJmlClassLevel(Providers.provider(f));
        assertParsedOrKnownUnsupported(f, r);
    }

    private Stream<File> files() {
        File[] files = new File("src/test/resources/fragments").listFiles();
        assert files != null;
        Arrays.sort(files);
        return Arrays.stream(files);
    }
}
