/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchStatementContextTest {

    private final TypeSolver typeSolver = new ReflectionTypeSolver();
    private JavaParser javaParser;

    @BeforeEach
    void beforeEach() {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        javaParser = new JavaParser();
    }

    @Test
    void testSwitchWithoutPattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    switch (o) {\n"
                + "        case 12 -> someCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        SwitchEntry switchEntry = Navigator.demandNodeOfGivenClass(cu, SwitchEntry.class);
        SwitchEntryContext entryContext = new SwitchEntryContext(switchEntry, typeSolver);

        List<TypePatternExpr> typePatternsExposedToBody =
                entryContext.typePatternExprsExposedToChild(switchEntry.getStatement(0));

        assertEquals(0, typePatternsExposedToBody.size());
    }

    @Test
    void testSwitchWithTypePattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    switch (o) {\n"
                + "        case Foo f -> someCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        SwitchEntry switchEntry = Navigator.demandNodeOfGivenClass(cu, SwitchEntry.class);
        SwitchEntryContext entryContext = new SwitchEntryContext(switchEntry, typeSolver);

        List<TypePatternExpr> typePatternsExposedToBody =
                entryContext.typePatternExprsExposedToChild(switchEntry.getStatement(0));

        assertEquals(1, typePatternsExposedToBody.size());
        TypePatternExpr exposedPattern = typePatternsExposedToBody.get(0);
        assertEquals("Foo", exposedPattern.getTypeAsString());
        assertEquals("f", exposedPattern.getNameAsString());
    }

    @Test
    void testSwitchWithUnnamedTypePattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    switch (o) {\n"
                + "        case Foo _ -> someCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        SwitchEntry switchEntry = Navigator.demandNodeOfGivenClass(cu, SwitchEntry.class);
        SwitchEntryContext entryContext = new SwitchEntryContext(switchEntry, typeSolver);

        List<TypePatternExpr> typePatternsExposedToBody =
                entryContext.typePatternExprsExposedToChild(switchEntry.getStatement(0));

        assertEquals(0, typePatternsExposedToBody.size());
    }

    @Test
    void testSwitchWithRecordPattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    switch (o) {\n"
                + "        case Foo(Bar b, Qux(Quux q)) -> someCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        SwitchEntry switchEntry = Navigator.demandNodeOfGivenClass(cu, SwitchEntry.class);
        SwitchEntryContext entryContext = new SwitchEntryContext(switchEntry, typeSolver);

        List<TypePatternExpr> typePatternsExposedToBody =
                entryContext.typePatternExprsExposedToChild(switchEntry.getStatement(0));

        assertEquals(2, typePatternsExposedToBody.size());
        TypePatternExpr exposedPattern = typePatternsExposedToBody.get(0);
        assertEquals("Bar", exposedPattern.getTypeAsString());
        assertEquals("b", exposedPattern.getNameAsString());
        TypePatternExpr secondExposedPattern = typePatternsExposedToBody.get(1);
        assertEquals("Quux", secondExposedPattern.getTypeAsString());
        assertEquals("q", secondExposedPattern.getNameAsString());
    }

    @Test
    void testSwitchWithMatchAllPattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    switch (o) {\n"
                + "        case Foo(Bar b, Qux(Quux _)) -> someCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        SwitchEntry switchEntry = Navigator.demandNodeOfGivenClass(cu, SwitchEntry.class);
        SwitchEntryContext entryContext = new SwitchEntryContext(switchEntry, typeSolver);

        List<TypePatternExpr> typePatternsExposedToBody =
                entryContext.typePatternExprsExposedToChild(switchEntry.getStatement(0));

        assertEquals(1, typePatternsExposedToBody.size());
        TypePatternExpr exposedPattern = typePatternsExposedToBody.get(0);
        assertEquals("Bar", exposedPattern.getTypeAsString());
        assertEquals("b", exposedPattern.getNameAsString());
    }

    private CompilationUnit parse(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().orElseThrow(AssertionError::new);
    }
}
