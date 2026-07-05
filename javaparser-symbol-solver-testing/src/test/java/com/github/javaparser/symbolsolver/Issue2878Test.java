/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class Issue2878Test extends AbstractResolutionTest {

    @Test()
    void test() throws IOException {
        Path rootSourceDir = adaptPath("src/test/resources/issue2878");

        String src = "import java.util.Optional;\n" + "\n"
                + "public class U10 {\n"
                + "    private final String file;\n"
                + "\n"
                + "    public U10(String file) {\n"
                + "        this.file = file;\n"
                + "    }\n"
                + "    public void main(String[] args) {\n"
                + "        U9 u1 = new U9(Optional.empty(), Optional.empty(), 1); // failed\n"
                + "        U9 u2 = new U9(Optional.of(file), Optional.empty(), 1); // success\n"
                + "        U9 u3 = new U9(Optional.empty(), Optional.empty(), \"/\"); // success\n"
                + "        U9 u4 = new U9(Optional.empty(), Optional.empty(), true); // success\n"
                + "    }\n"
                + "}";

        ParserConfiguration config = new ParserConfiguration();
        CombinedTypeSolver cts = new CombinedTypeSolver(
                new ReflectionTypeSolver(false), new JavaParserTypeSolver(rootSourceDir.toFile()));
        config.setSymbolResolver(new JavaSymbolSolver(cts));
        StaticJavaParser.setConfiguration(config);

        CompilationUnit cu = StaticJavaParser.parse(src);

        List<ObjectCreationExpr> oces = cu.findAll(ObjectCreationExpr.class);

        assertEquals(
                "U9.U9(java.util.Optional<java.lang.String>, java.util.Optional<U9>, int)",
                oces.get(0).resolve().getQualifiedSignature());
        assertEquals(
                "U9.U9(java.util.Optional<java.lang.String>, java.util.Optional<U9>, int)",
                oces.get(1).resolve().getQualifiedSignature());
        assertEquals(
                "U9.U9(java.util.Optional<java.lang.String>, java.util.Optional<java.lang.String>, java.lang.String)",
                oces.get(2).resolve().getQualifiedSignature());
        assertEquals(
                "U9.U9(java.util.Optional<U9>, java.util.Optional<U9>, boolean)",
                oces.get(3).resolve().getQualifiedSignature());
    }
}
