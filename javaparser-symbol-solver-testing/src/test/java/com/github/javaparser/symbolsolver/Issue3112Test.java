/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue3112Test {

    @Test
    public void test0() {
        ParserConfiguration config = new ParserConfiguration();
        CombinedTypeSolver cts = new CombinedTypeSolver();
        cts.add(new ReflectionTypeSolver(false));
        config.setSymbolResolver(new JavaSymbolSolver(cts));
        StaticJavaParser.setConfiguration(config);

        String str = "public class MyClass {\n" + "   class Inner1 {\n"
                + "       class Inner2 {\n"
                + "       }\n"
                + "   }\n"
                + "   {\n"
                + "       new Inner1(){}.new Inner2();\n"
                + "   }\n"
                + "}\n";
        CompilationUnit cu = StaticJavaParser.parse(str);
        List<ObjectCreationExpr> local = cu.findAll(ObjectCreationExpr.class);
        local.forEach(lcl -> assertFalse(lcl.getType()
                .resolve()
                .asReferenceType()
                .getTypeDeclaration()
                .get()
                .isInterface()));
    }

    @Test
    public void test1() {
        ParserConfiguration config = new ParserConfiguration();
        CombinedTypeSolver cts = new CombinedTypeSolver();
        cts.add(new ReflectionTypeSolver(false));
        config.setSymbolResolver(new JavaSymbolSolver(cts));
        StaticJavaParser.setConfiguration(config);

        String str = "public class MyClass {\n" + "   class Inner1 {\n"
                + "       class Inner2 {\n"
                + "           class Inner3 {\n"
                + "           }\n"
                + "       }\n"
                + "   }\n"
                + "   {\n"
                + "       new Inner1(){}.new Inner2(){}.new Inner3();\n"
                + "   }\n"
                + "}\n";
        CompilationUnit cu = StaticJavaParser.parse(str);
        List<ObjectCreationExpr> local = cu.findAll(ObjectCreationExpr.class);
        local.forEach(lcl -> assertFalse(lcl.getType()
                .resolve()
                .asReferenceType()
                .getTypeDeclaration()
                .get()
                .isInterface()));
    }
}
