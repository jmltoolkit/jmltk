/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.StreamProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflectionTypeSolverTest extends ClassLoaderTypeSolverTest<ReflectionTypeSolver> {

    public ReflectionTypeSolverTest() {
        super(ReflectionTypeSolver::new);
    }

    @Test
    void testHasType() {
        ReflectionTypeSolver ts = new ReflectionTypeSolver();
        assertEquals(true, ts.hasType(String.class.getCanonicalName()));
        assertEquals(true, ts.hasType(Object.class.getCanonicalName()));
        assertEquals(false, ts.hasType("foo.zum.unexisting"));
    }

    @Test()
    void testInvalidArgumentNumber() throws IOException {
        Path file = adaptPath("src/test/resources/issue2366/Test.java");

        CombinedTypeSolver combinedSolver = new CombinedTypeSolver(new ReflectionTypeSolver());

        ParserConfiguration pc = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedSolver))
                .setLanguageLevel(LanguageLevel.JAVA_8);

        JavaParser javaParser = new JavaParser(pc);

        CompilationUnit unit = javaParser
                .parse(
                        ParseStart.COMPILATION_UNIT,
                        new StreamProvider(Files.newInputStream(file), StandardCharsets.UTF_8.name()))
                .getResult()
                .get();

        Assertions.assertThrows(
                UnsolvedSymbolException.class,
                () -> unit.accept(
                        new VoidVisitorAdapter<Object>() {
                            @Override
                            public void visit(ObjectCreationExpr exp, Object arg) {
                                super.visit(exp, arg);
                                exp.resolve().getSignature();
                            }
                        },
                        null));
    }

    @Test
    void testFilteringAll() {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(false);
        assertEquals(true, reflectionTypeSolver.hasType("java.lang.Object"));
        assertEquals(true, reflectionTypeSolver.hasType("org.xml.sax.Parser"));
        assertEquals(true, reflectionTypeSolver.hasType(this.getClass().getCanonicalName()));
    }

    @Test
    void testFilteringJRE() {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(true);
        assertEquals(true, reflectionTypeSolver.hasType("java.lang.Object"));
        assertEquals(false, reflectionTypeSolver.hasType("org.xml.sax.Parser"));
        assertEquals(false, reflectionTypeSolver.hasType(this.getClass().getCanonicalName()));
    }

    @Test
    void testFilteringJCL() {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(ReflectionTypeSolver.JCL_ONLY);
        assertEquals(true, reflectionTypeSolver.hasType("java.lang.Object"));
        assertEquals(true, reflectionTypeSolver.hasType("org.xml.sax.Parser"));
        assertEquals(false, reflectionTypeSolver.hasType(this.getClass().getCanonicalName()));
    }
}
