/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.RAW;
import static com.github.javaparser.Providers.provider;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

class ParserConfigurationTest {
    @Test
    void storeNoTokens() {
        ParseResult<CompilationUnit> result = new JavaParser(new ParserConfiguration().setStoreTokens(false))
                .parse(ParseStart.COMPILATION_UNIT, provider("class X{}"));

        assertFalse(result.getResult().get().getTokenRange().isPresent());
        assertTrue(result.getResult().get().findAll(Node.class).stream()
                .noneMatch(node -> node.getTokenRange().isPresent()));
    }

    @Test
    void noProblemsHere() {
        ParseResult<Statement> result =
                new JavaParser(new ParserConfiguration().setLanguageLevel(RAW)).parse(STATEMENT, provider("try{}"));
        assertTrue(result.isSuccessful());
    }
}
