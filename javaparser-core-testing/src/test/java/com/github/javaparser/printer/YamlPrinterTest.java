/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.utils.TestUtils.readTextResource;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Test;

class YamlPrinterTest {

    private String read(String filename) {
        return readTextResource(YamlPrinterTest.class, filename);
    }

    @Test
    void testWithType() {
        YamlPrinter yamlPrinter = new YamlPrinter(true);
        Expression expression = parseExpression("x(1,1)");
        String output = yamlPrinter.output(expression);
        assertThat(output).isEqualToNormalizingWhitespace(read("yamlWithType.yaml"));
    }

    @Test
    void testWithoutType() {
        YamlPrinter yamlPrinter = new YamlPrinter(false);
        Expression expression = parseExpression("1+1");
        String output = yamlPrinter.output(expression);
        assertThat(output).isEqualToNormalizingWhitespace(read("yamlWithoutType.yaml"));
    }

    @Test
    void testWithColonFollowedBySpaceInValue() {
        YamlPrinter yamlPrinter = new YamlPrinter(true);
        Expression expression = parseExpression("\"a\\\\: b\"");
        String output = yamlPrinter.output(expression);
        assertThat(output).isEqualToNormalizingWhitespace(read("yamlWithColonFollowedBySpaceInValue.yaml"));
    }

    @Test
    void testWithColonFollowedByLineSeparatorInValue() {
        YamlPrinter yamlPrinter = new YamlPrinter(true);
        Expression expression = parseExpression("\"a\\\\:\\\\nb\"");
        String output = yamlPrinter.output(expression);
        assertThat(output).isEqualToNormalizingWhitespace(read("yamlWithColonFollowedByLineSeparatorInValue.yaml"));
    }

    @Test
    void testParsingJavadocWithQuoteAndNewline() {
        String code = "/**\n" + " * \" this comment contains a quote and newlines\n" + " */\n" + "public class Dog {}";

        YamlPrinter yamlPrinter = new YamlPrinter(true);
        CompilationUnit computationUnit = parse(code);
        String output = yamlPrinter.output(computationUnit);
        assertThat(output).isEqualToNormalizingWhitespace(read("yamlParsingJavadocWithQuoteAndNewline.yaml"));
    }
}
