/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast.body;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.*;

import static com.github.javaparser.StaticJavaParser.parseStatement;
import static com.github.javaparser.utils.TestUtils.assertEqualsStringIgnoringEol;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Transforming Statement and verifying the LexicalPreservation works as expected.
 */
class StatementTransformationsTest extends AbstractLexicalPreservingTest {

    private static final ParserConfiguration.LanguageLevel storedLanguageLevel =
            StaticJavaParser.getParserConfiguration().getLanguageLevel();

    @BeforeEach
    public void setLanguageLevel() {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    }

    @AfterEach
    public void resetLanguageLevel() {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(storedLanguageLevel);
    }

    Statement consider(String code) {
        Statement statement = parseStatement(code);
        LexicalPreservingPrinter.setup(statement);
        return statement;
    }

    @Test
    void ifStmtTransformation() {
        Statement stmt = consider("if (a) {} else {}");
        stmt.asIfStmt().setCondition(new NameExpr("b"));
        assertTransformedToString("if (b) {} else {}", stmt);
    }

    @Test
    void switchEntryCsmHasTrailingUnindent() {
        Statement stmt = consider("switch (a) { case 1: a; a; }");
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(1, statements.get(1).clone()); // clone() to force replacement
        assertTransformedToString("switch (a) { case 1: a; a; }", stmt);
    }

    @Test
    void newSwitchEntryPreserved() {
        String code = "switch (a) { case 2 -> System.out.println(-1); }";
        Statement stmt = consider(code);
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code, stmt);
    }

    @Test
    void newDefaultSwitchEntryPreserved() {
        String code = "switch (a) { default -> System.out.println(-1); }";
        Statement stmt = consider(code);
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code, stmt);
    }

    @Test
    void nullDefaultSwitchEntryPreserved() {
        String code = "switch (a) { case null, default -> System.out.println(-1); }";
        Statement stmt = consider(code);
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code, stmt);
    }

    @Test
    void switchPatternPreserved() {
        String code = "switch (a) { case String s -> System.out.println(s); }";
        Statement stmt = consider(code);
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code, stmt);
    }

    @Test
    void switchPatternWithGuardPreserved() {
        String code = "switch (a) { case String s when s.length() > 5 -> System.out.println(s); }";
        Statement stmt = consider(code);
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code, stmt);
    }

    @Test
    void switchWithRecordPatternPreserved() {
        String code = "switch (a) { case OldBox (TwoBox(String s, Box (Integer i))) -> System.out.println(i); }";
        Statement stmt = consider(code);
        NodeList<SwitchEntry> entries = stmt.asSwitchStmt().getEntries();
        entries.get(0).getLabels().get(0).asRecordPatternExpr().setType("NewBox");
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code.replaceAll("OldBox", "NewBox"), stmt);
    }

    @Test
    void switchWithMatchAllPatternPreserved() {
        String code = "switch (a) { case OldBox (TwoBox(String s, Box (_))) -> System.out.println(i); }";
        Statement stmt = consider(code);
        NodeList<SwitchEntry> entries = stmt.asSwitchStmt().getEntries();
        entries.get(0).getLabels().get(0).asRecordPatternExpr().setType("NewBox");
        NodeList<Statement> statements = stmt.asSwitchStmt().getEntry(0).getStatements();
        statements.set(0, statements.get(0).clone());
        assertTransformedToString(code.replaceAll("OldBox", "NewBox"), stmt);
    }

    @Test
    void matchAllPatternAdded() {
        String originalCode = "class A {\n"
                + "		void m(int year) { \n"
                + "			return switch (year) {\n"
                + "				case Box(String s) -> new Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			};\n"
                + "		}\n"
                + "	}";
        String expectedCode = "switch (year) {\n"
                + "				case Box(_) -> new Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			}";
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        StaticJavaParser.setConfiguration(config);
        CompilationUnit cu = LexicalPreservingPrinter.setup(StaticJavaParser.parse(originalCode));
        SwitchExpr switchExpr = cu.findFirst(SwitchExpr.class).get();
        NodeList<SwitchEntry> entries = switchExpr.getEntries();

        RecordPatternExpr label = entries.get(0).getLabels().get(0).asRecordPatternExpr();
        NodeList<ComponentPatternExpr> newPatternList = NodeList.nodeList(new MatchAllPatternExpr(new NodeList<>()));
        label.setPatternList(newPatternList);

        String output = LexicalPreservingPrinter.print(switchExpr);

        assertEqualsStringIgnoringEol(expectedCode, output);
    }

    @Test
    void unnamedTypePatternAdded() {
        String originalCode = "class A {\n"
                + "		void m(int year) { \n"
                + "			return switch (year) {\n"
                + "				case Box(String s) -> new Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			};\n"
                + "		}\n"
                + "	}";
        String expectedCode = "switch (year) {\n"
                + "				case Box(String _) -> new Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			}";
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        StaticJavaParser.setConfiguration(config);
        CompilationUnit cu = LexicalPreservingPrinter.setup(StaticJavaParser.parse(originalCode));
        SwitchExpr switchExpr = cu.findFirst(SwitchExpr.class).get();
        NodeList<SwitchEntry> entries = switchExpr.getEntries();

        RecordPatternExpr label = entries.get(0).getLabels().get(0).asRecordPatternExpr();
        TypePatternExpr typePatternExpr = label.getPatternList().get(0).asTypePatternExpr();
        typePatternExpr.setName("_");

        String output = LexicalPreservingPrinter.print(switchExpr);

        assertEqualsStringIgnoringEol(expectedCode, output);
    }

    @Test
    void issue4646() {
        String originalCode = "class A {\n"
                + "		void m(int year) { \n"
                + "			return switch (year) {\n"
                + "				case 2023 -> new Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			};\n"
                + "		}\n"
                + "	}";
        String expectedCode = "switch (year) {\n"
                + "				case 2023 -> new Object();\n"
                + "				case 2024 -> new java.lang.Object();\n"
                + "				default -> throw new IllegalStateException(\"Cant create for year\");\n"
                + "			}";
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        StaticJavaParser.setConfiguration(config);
        CompilationUnit cu = LexicalPreservingPrinter.setup(StaticJavaParser.parse(originalCode));
        SwitchExpr switchExpr = cu.findFirst(SwitchExpr.class).get();
        NodeList<SwitchEntry> entries = switchExpr.getEntries();
        NodeList<Expression> labels = NodeList.nodeList(new IntegerLiteralExpr("2024"));

        Range entryRange = entries.get(0).getRange().get();
        assertEquals(4, entryRange.begin.line);
        assertEquals(4, entryRange.end.line);
        assertEquals(5, entryRange.begin.column);
        assertEquals(30, entryRange.end.column);

        Statement stmt = new ExpressionStmt(new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType("java.lang.Object"),
                entries.get(entries.size() - 2)
                        .getStatements()
                        .get(0)
                        .asExpressionStmt()
                        .getExpression()
                        .asObjectCreationExpr()
                        .getArguments()));

        NodeList<Statement> expressions = NodeList.nodeList(stmt);

        SwitchEntry newEntry = new SwitchEntry(labels, SwitchEntry.Type.EXPRESSION, expressions);

        entries.add(entries.size() - 1, newEntry);

        String output = LexicalPreservingPrinter.print(switchExpr);

        assertEqualsStringIgnoringEol(expectedCode, output);
    }
}
