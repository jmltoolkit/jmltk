/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.version;

import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_10;
import static com.github.javaparser.Providers.provider;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.VarType;
import java.util.List;
import org.junit.jupiter.api.Test;

class Java10PostProcessorTest {
    public static final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(JAVA_10));

    @Test
    void varIsAType() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("var x=\"\";"));

        List<VarType> allVarTypes = result.getResult().get().findAll(VarType.class);

        assertEquals(1, allVarTypes.size());
    }

    @Test
    void expressionThatShouldNotBeInterpretedAsAVarType() {
        ParseResult<Statement> result = javaParser.parse(STATEMENT, provider("var.class.getName();"));

        List<VarType> allVarTypes = result.getResult().get().findAll(VarType.class);

        assertEquals(0, allVarTypes.size());
    }
}
