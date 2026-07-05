/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3866Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "public interface MyActivity {\n"
                + "  class MyTimestamps {}\n"
                + "    MyTimestamps getTimestamps();\n"
                + "  }\n"
                + "\n"
                + "  public interface MyRichPresence extends MyActivity { }\n"
                + "\n"
                + "  class MyActivityImpl implements MyActivity {\n"
                + "    MyActivity.MyTimestamps timestamps;\n"
                + "    @Override\n"
                + "    public MyActivity.MyTimestamps getTimestamps() {\n"
                + "      return timestamps;\n"
                + "  }\n"
                + "}";

        final JavaSymbolSolver solver = new JavaSymbolSolver(new ReflectionTypeSolver(false));
        StaticJavaParser.getParserConfiguration().setSymbolResolver(solver);
        final CompilationUnit compilationUnit = StaticJavaParser.parse(code);

        final List<String> returnTypes = compilationUnit.findAll(MethodDeclaration.class).stream()
                .map(md -> md.resolve())
                .map(rmd -> rmd.getReturnType().describe())
                .collect(Collectors.toList());

        returnTypes.forEach(type -> assertEquals("MyActivity.MyTimestamps", type));
    }
}
