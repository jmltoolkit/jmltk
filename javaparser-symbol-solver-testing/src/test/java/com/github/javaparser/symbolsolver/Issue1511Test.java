/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static com.github.javaparser.symbolsolver.AbstractSymbolResolutionTest.adaptPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * IndexOutOfBoundsException when attempting to resolve super() #1511
 *
 * @see <a href="https://github.com/javaparser/javaparser/issues/1511">https://github.com/javaparser/javaparser/issues/1511</a>
 */
public class Issue1511Test {

    @Test
    public void test() throws FileNotFoundException {

        Path dir = adaptPath("src/test/resources/issue1511");
        Path file = adaptPath("src/test/resources/issue1511/A.java");

        // configure symbol solver
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(dir.toFile()));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        // get compilation unit & extract explicit constructor invocation statement
        CompilationUnit cu = StaticJavaParser.parse(file.toFile());
        ExplicitConstructorInvocationStmt ecis = cu.getPrimaryType()
                .orElseThrow(IllegalStateException::new)
                .asClassOrInterfaceDeclaration()
                .getMember(0)
                .asConstructorDeclaration()
                .getBody()
                .get()
                .getStatement(0)
                .asExplicitConstructorInvocationStmt();

        // attempt to resolve explicit constructor invocation statement
        ResolvedConstructorDeclaration rcd = ecis.resolve(); // .resolveInvokedConstructor(); // <-- exception occurs
    }

    @Test
    public void exploratory_resolveAndGetSuperClass() {

        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        JavaParser javaParser = new JavaParser(configuration);

        CompilationUnit foo = javaParser.parse("class A {}").getResult().orElseThrow(IllegalStateException::new);
        ResolvedReferenceType a = foo.getClassByName("A")
                .orElseThrow(IllegalStateException::new)
                .resolve()
                .asClass()
                .getSuperClass()
                .get();

        assertEquals("java.lang.Object", a.getQualifiedName());
    }
}
