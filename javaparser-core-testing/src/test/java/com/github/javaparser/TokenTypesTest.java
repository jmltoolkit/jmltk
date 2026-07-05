/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.SwitchEntry;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.utils.CodeGenerationUtils.mavenModuleRoot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TokenTypesTest {

    @Test
    void everyTokenHasACategory() throws IOException {
        final int tokenCount = GeneratedJavaParserConstants.tokenImage.length - 1;
        Path tokenTypesPath = mavenModuleRoot(JavaParserTest.class)
                .resolve("../javaparser-core/src/main/java/com/github/javaparser/TokenTypes.java");
        CompilationUnit tokenTypesCu = parse(tokenTypesPath);

        // -1 to take off the default: case.
        int switchEntries = tokenTypesCu.findAll(SwitchEntry.class).size() - 1;

        // The amount of "case XXX:" in TokenTypes.java should be equal to the amount of tokens JavaCC knows about:
        assertEquals(tokenCount + 1, switchEntries);
    }

    @TestFactory
    Stream<DynamicTest> everyTokenHasACategory0() {
        final int tokenCount = GeneratedJavaParserConstants.tokenImage.length;
        return IntStream.range(0, tokenCount)
                .mapToObj(it -> DynamicTest.dynamicTest("TokenType: " + it, () -> {
                    try {
                        TokenTypes.getCategory(it);
                    } catch (IllegalArgumentException ignored) {
                    }
                }));
    }

    @Test
    void throwOnUnrecognisedTokenType() {
        assertThrows(AssertionError.class, () -> {
            TokenTypes.getCategory(-1);
        });
    }
}
