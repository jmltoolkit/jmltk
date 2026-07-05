/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class Issue3100Test extends AbstractResolutionTest {

    @Test
    public void test() {
        String code = "public class Test {\r\n"
                + "    public static void main(String[] args) {\r\n"
                + "        ConvolutionLayer.Builder b = new Convolution2D.Builder();\r\n"
                + "    }\r\n"
                + "}";
        Path pathToSourceFile = adaptPath("src/test/resources/issue3100");
        TypeSolver typeSolver =
                new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(pathToSourceFile));
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);
        CompilationUnit cu = StaticJavaParser.parse(code);
        (new VoidVisitorAdapter<Void>() {

                    @Override
                    public void visit(ClassOrInterfaceType n, Void arg) {
                        ResolvedType rt = n.resolve();
                    }
                })
                .visit(cu, null);
    }
}
