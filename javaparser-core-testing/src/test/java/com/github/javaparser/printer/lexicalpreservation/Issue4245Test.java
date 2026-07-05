/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;

class Issue4245Test extends AbstractLexicalPreservingTest {

    @Test
    public void test() {

        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setLanguageLevel(LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(parserConfiguration);
        considerCode("public sealed interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}");

        ClassOrInterfaceDeclaration classOrInterface =
                cu.findFirst(ClassOrInterfaceDeclaration.class).get();
        classOrInterface.setModifiers();

        String expected = "interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}";

        assertEqualsStringIgnoringEol(expected, LexicalPreservingPrinter.print(cu));
    }
}
