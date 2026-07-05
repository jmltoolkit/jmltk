/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IfStatementContextTest {

    private final TypeSolver typeSolver = new ReflectionTypeSolver();
    private JavaParser javaParser;

    @BeforeEach
    void beforeEach() {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        javaParser = new JavaParser();
    }

    @Test
    void testInstanceOfWithoutPattern() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof String) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(0, typePatternsExposedToThen.size());
        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithTypePatternVariableIntroducedToThen() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof Foo f) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(1, typePatternsExposedToThen.size());
        TypePatternExpr introducedTypePattern = typePatternsExposedToThen.get(0);
        assertEquals("Foo", introducedTypePattern.getTypeAsString());
        assertEquals("f", introducedTypePattern.getNameAsString());

        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithTypePatternVariableIntroducedToElse() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (!(o instanceof Foo f)) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(0, typePatternsExposedToThen.size());

        assertEquals(1, typePatternsExposedToElse.size());
        TypePatternExpr introducedTypePattern = typePatternsExposedToElse.get(0);
        assertEquals("Foo", introducedTypePattern.getTypeAsString());
        assertEquals("f", introducedTypePattern.getNameAsString());

        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithTypePatternVariableIntroducedByIf() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (!(o instanceof Foo f)) {\n"
                + "        return;\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(0, typePatternsExposedToThen.size());

        assertEquals(1, typePatternsExposedToElse.size());
        TypePatternExpr typePatternIntroducedToElse = typePatternsExposedToElse.get(0);
        assertEquals("Foo", typePatternIntroducedToElse.getTypeAsString());
        assertEquals("f", typePatternIntroducedToElse.getNameAsString());

        assertEquals(1, introducedTypePatterns.size());
        TypePatternExpr introducedTypePattern = typePatternsExposedToElse.get(0);
        assertEquals("Foo", introducedTypePattern.getTypeAsString());
        assertEquals("f", introducedTypePattern.getNameAsString());
    }

    @Test
    void testInstanceOfWithRecordPatternVariablesIntroducedToThen() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof Foo (Bar b, Baz (Qux q))) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(2, typePatternsExposedToThen.size());
        TypePatternExpr barTypePattern = typePatternsExposedToThen.get(0);
        assertEquals("Bar", barTypePattern.getTypeAsString());
        assertEquals("b", barTypePattern.getNameAsString());
        TypePatternExpr quxTypePattern = typePatternsExposedToThen.get(1);
        assertEquals("Qux", quxTypePattern.getTypeAsString());
        assertEquals("q", quxTypePattern.getNameAsString());

        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithUnnamedTypePatternVariableIntroducedToThen() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof Foo _) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(0, typePatternsExposedToThen.size());
        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithMatchAllPatternVariableIntroducedToThen() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof Foo(_)) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(0, typePatternsExposedToThen.size());
        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    @Test
    void testInstanceOfWithNestedUnnamedTypePatternVariableIntroducedToThen() {
        CompilationUnit cu = parse("class Foo {\n" + "  public void foo(Object o) {\n"
                + "    if (o instanceof Foo(Bar _, Baz(Qux q, _))) {\n"
                + "        thenCall();\n"
                + "    } else {\n"
                + "        elseCall();\n"
                + "    }\n"
                + "  }\n"
                + "}");

        IfStmt ifStmt = Navigator.demandNodeOfGivenClass(cu, IfStmt.class);
        IfStatementContext ifContext = new IfStatementContext(ifStmt, typeSolver);

        List<TypePatternExpr> typePatternsExposedToThen =
                ifContext.typePatternExprsExposedToChild(ifStmt.getThenStmt());
        List<TypePatternExpr> typePatternsExposedToElse =
                ifContext.typePatternExprsExposedToChild(ifStmt.getElseStmt().get());
        List<TypePatternExpr> introducedTypePatterns = ifContext.getIntroducedTypePatterns();

        assertEquals(1, typePatternsExposedToThen.size());
        TypePatternExpr barTypePattern = typePatternsExposedToThen.get(0);
        assertEquals("Qux", barTypePattern.getTypeAsString());
        assertEquals("q", barTypePattern.getNameAsString());
        assertEquals(0, typePatternsExposedToElse.size());
        assertEquals(0, introducedTypePatterns.size());
    }

    private CompilationUnit parse(String sourceCode) {
        return javaParser.parse(sourceCode).getResult().orElseThrow(AssertionError::new);
    }
}
