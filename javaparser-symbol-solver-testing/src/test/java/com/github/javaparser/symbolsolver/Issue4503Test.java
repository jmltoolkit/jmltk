/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import org.junit.jupiter.api.Test;

public class Issue4503Test extends AbstractResolutionTest {

    @Test
    void test() {
        String code = "import java.util.function.Function;\n"
                + "import java.math.BigDecimal;\n"
                + "public class Test {\n"
                + "    public static <T, R> String logAndCatch(String moduleName, Object filter1, Object filter2, T req, Function<T, R> func) {\n"
                + "        return null;\n"
                + "    }\n"
                + "    public void test(String testModule, String filter1, String filter2) {\n"
                + "        Test.logAndCatch(testModule, filter1, filter2, \"1.2\", this::getAmount);\n"
                + "    }\n"
                + "    public BigDecimal getAmount(String amount) {\n"
                + "        return new BigDecimal(amount);\n"
                + "    }\n"
                + "}";

        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_18);
        config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
        StaticJavaParser.setConfiguration(config);

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<MethodReferenceExpr> exprs = cu.findAll(MethodReferenceExpr.class);
        for (MethodReferenceExpr expr : exprs) {
            if (expr.getIdentifier().contentEquals("getAmount")) {
                assertEquals("Test.getAmount(java.lang.String)", expr.resolve().getQualifiedSignature());
            }
        }
    }
}
