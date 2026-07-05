/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class ProvidersTest {

    @Test
    void testResourceProvider() throws IOException {
        Provider provider = Providers.resourceProvider("com/github/javaparser/issue_samples/Issue290.java.txt");
        assertNotNull(provider);
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parse = parser.parse(ParseStart.COMPILATION_UNIT, provider);
        assertTrue(parse.isSuccessful());
    }

    @Test
    void testResourceProviderWithWrongEncoding() throws IOException {
        Provider provider = Providers.resourceProvider("com/github/javaparser/TestFileIso88591.java");
        assertNotNull(provider);
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parse = parser.parse(ParseStart.COMPILATION_UNIT, provider);
        assertFalse(parse.isSuccessful());
    }

    @Test
    void testResourceProviderWithEncoding() throws IOException {
        Provider provider = Providers.resourceProvider(
                "com/github/javaparser/TestFileIso88591.java", Charset.forName("ISO-8859-1"));
        assertNotNull(provider);
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parse = parser.parse(ParseStart.COMPILATION_UNIT, provider);
        if (!parse.isSuccessful()) {
            parse.getProblems().forEach(System.out::println);
        }
        assertTrue(parse.isSuccessful());
    }
}
