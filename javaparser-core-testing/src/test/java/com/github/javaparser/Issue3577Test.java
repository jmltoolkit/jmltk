/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ParserConfiguration.LanguageLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Issue3577Test {

    @Test
    public void test() {
        String str = "public class MyClass {\n"
                + "    public static void main(String args[]) {\n"
                + "      System.out.println(\"Hello\\sWorld\");\n"
                + "    }\n"
                + "}";

        ParserConfiguration config = new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_15);
        StaticJavaParser.setConfiguration(config);

        assertDoesNotThrow(() -> StaticJavaParser.parse(str));
        //        unitOpt.getProblems().stream().forEach(p -> System.err.println(p.toString()));
    }
}
