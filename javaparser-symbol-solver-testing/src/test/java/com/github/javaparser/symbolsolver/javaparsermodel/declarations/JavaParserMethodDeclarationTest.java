/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclarationTest;
import com.github.javaparser.symbolsolver.core.resolution.TypeVariableResolutionCapabilityTest;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JavaParserMethodDeclarationTest extends AbstractResolutionTest
        implements ResolvedMethodDeclarationTest, TypeVariableResolutionCapabilityTest {

    @Override
    public Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST) {
        return Optional.of(
                safeCast(associableToAST, JavaParserMethodDeclaration.class).getWrappedNode());
    }

    @Override
    public JavaParserMethodDeclaration createValue() {
        MethodDeclaration methodDeclaration = StaticJavaParser.parse("class A {void a() {}}")
                .findFirst(MethodDeclaration.class)
                .get();
        TypeSolver typeSolver = new ReflectionTypeSolver();
        return new JavaParserMethodDeclaration(methodDeclaration, typeSolver);
    }

    @Test
    void issue2484() {
        String code = "public class MyClass {\n"
                + "    private Ibaz m_something;\n"
                + "\n"
                + "    public interface Ibaz {\n"
                + "    }\n"
                + "    \n"
                + "    public void foo(Class<? extends Ibaz> clazz) {\n"
                + "    }\n"
                + "    \n"
                + "    protected void bar() {\n"
                + "        foo(null); // this works\n"
                + "        foo(m_something.getClass()); // this doesn't work\n"
                + "    }\n"
                + "}";

        JavaParserAdapter parser = JavaParserAdapter.of(createParserWithResolver(defaultTypeSolver()));
        CompilationUnit cu = parser.parse(code);

        List<MethodCallExpr> mces = cu.findAll(MethodCallExpr.class);
        assertEquals(
                "MyClass.foo(java.lang.Class<? extends MyClass.Ibaz>)",
                mces.get(0).resolve().getQualifiedSignature());
        assertEquals(
                "MyClass.foo(java.lang.Class<? extends MyClass.Ibaz>)",
                mces.get(1).resolve().getQualifiedSignature());
    }
}
