/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

public class Issue1950Test extends AbstractResolutionTest {

    @Test
    public void test() {

        TypeSolver typeSolver = new ReflectionTypeSolver(false);
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);

        String s = "import java.util.concurrent.Callable;\n" + "class Foo { \n"
                + "  void foo() {\n"
                + "     method(()->{});\n"
                + "  }\n"
                + "  public void method(Runnable lambda) {\n"
                + "  }\n"
                + "  public <T> void method(Callable<T> lambda) {\n"
                + "  }\n"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(s);
        MethodCallExpr mce = cu.findFirst(MethodCallExpr.class).get();

        ResolvedMethodDeclaration resolved = mce.resolve();

        // 15.12.2.5. Choosing the Most Specific Method
        // One applicable method m1 is more specific than another applicable method m2, for an invocation with argument
        // expressions e1, ..., ek, if any of the following are true:
        // m2 is generic, and m1 is inferred to be more specific than m2 for argument expressions e1, ..., ek by
        // §18.5.4.
        assertEquals("java.lang.Runnable", resolved.getParam(0).getType().describe());
        assertTrue(!resolved.isGeneric());
    }
}
