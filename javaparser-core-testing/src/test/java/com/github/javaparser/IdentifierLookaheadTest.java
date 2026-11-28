/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierLookaheadTest {

    private static final Pattern KIND = Pattern.compile("<([A-Z_0-9]+)>");
    private static final Pattern IMAGE = Pattern.compile("\"([A-Za-z_$][A-Za-z0-9_$]*)\"");

    /** Parses with validators disabled so the result depends on the grammar only. */
    private static boolean parsesAsIdentifier(String text) {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.getProcessors().clear();
        return new JavaParser(configuration)
                .parse("class X { Object " + text + "; }")
                .isSuccessful();
    }

    private static int kindForImage(String image) {
        String quoted = "\"" + image + "\"";
        for (int kind = 0; kind < GeneratedJavaParserConstants.tokenImage.length; kind++) {
            if (GeneratedJavaParserConstants.tokenImage[kind].equals(quoted)) {
                return kind;
            }
        }
        throw new AssertionError("No token kind has image " + quoted);
    }

    /**
     * The guard is a hard gate in front of the alternatives: every token kind listed in
     * {@code Identifier()} must be accepted by the predicate, otherwise valid source stops parsing.
     */
    @Test
    void predicateCoversEveryIdentifierAlternative() throws IOException, ReflectiveOperationException {
        String grammar = Files.readString(Paths.get("../javaparser-core/src/main/javacc/java.jj"));
        int start = grammar.indexOf("String Identifier():");
        int end = grammar.indexOf("Expression Expression():", start);
        assertTrue(start >= 0 && end > start, "Identifier() production not found in java.jj");
        String block = grammar.substring(start, end).replaceAll("//.*", "");

        Set<String> names = new LinkedHashSet<>();
        Matcher kinds = KIND.matcher(block);
        while (kinds.find()) {
            names.add(kinds.group(1));
        }
        Matcher images = IMAGE.matcher(block);
        while (images.find()) {
            names.add(String.valueOf(kindForImage(images.group(1))));
        }
        assertFalse(names.isEmpty(), "no Identifier() alternatives found");

        for (String name : names) {
            int kind = name.chars().allMatch(Character::isDigit)
                    ? Integer.parseInt(name)
                    : GeneratedJavaParserConstants.class.getField(name).getInt(null);
            assertTrue(TokenTypes.isIdentifierKind(kind), "predicate rejects Identifier() kind " + name);
        }
    }

    @Test
    void predicateRejectsReservedJavaKeywords() {
        int[] reserved = {
            GeneratedJavaParserConstants.CLASS,
            GeneratedJavaParserConstants.IF,
            GeneratedJavaParserConstants.RETURN,
            GeneratedJavaParserConstants.NEW,
            GeneratedJavaParserConstants.PUBLIC,
            GeneratedJavaParserConstants.INT,
            GeneratedJavaParserConstants.VOID,
            GeneratedJavaParserConstants.TRUE,
            GeneratedJavaParserConstants.NULL,
        };
        for (int kind : reserved) {
            assertFalse(TokenTypes.isIdentifierKind(kind), "predicate accepts reserved keyword kind " + kind);
        }
    }

    @Test
    void keywordIdentifiersStillParseInIdentifierPosition() {
        String[] keywords = {
            "invariant",
            "requires",
            "ensures",
            "assert",
            "enum",
            "record",
            "yield",
            "_",
            "module",
            "open",
            "when",
            "sealed",
            "source",
            "exec",
            "ccatch",
            "ccat"
        };
        for (String keyword : keywords) {
            assertTrue(parsesAsIdentifier(keyword), "keyword is no longer accepted as an identifier: " + keyword);
        }
    }

    @Test
    void truncatedInputDoesNotThrow() {
        assertDoesNotThrow(() -> new JavaParser().parse("class X { Object "));
        assertDoesNotThrow(
                () -> new JavaParser(new ParserConfiguration().setProcessJml(true)).parse("class X { void m() { a."));
    }
}
