/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.Providers.provider;
import static com.github.javaparser.ast.Node.Parsedness.UNPARSABLE;
import static org.junit.jupiter.api.Assertions.*;

class ParseErrorRecoveryTest {
    private final JavaParser parser = new JavaParser();

    @Test
    void compilationUnitRecovery() {
        CompilationUnit cu = parser.parse(ParseStart.COMPILATION_UNIT, provider("XXX"))
                .getResult()
                .get();
        assertEquals(UNPARSABLE, cu.getParsed());
    }

    @Test
    void bodystatementSemicolonRecovery() {
        MethodDeclaration cu = parser.parse(ParseStart.CLASS_BODY, provider("int x(){X X X;}"))
                .getResult()
                .get()
                .asMethodDeclaration();
        Statement xxx = cu.getBody().get().getStatements().get(0);
        assertEquals(UNPARSABLE, xxx.getParsed());
    }

    @Test
    void bodystatementClosingBraceRecovery() {
        MethodDeclaration cu = parser.parse(ParseStart.CLASS_BODY, provider("int x(){X X X}"))
                .getResult()
                .get()
                .asMethodDeclaration();
        Statement xxx = cu.getBody().get();
        assertEquals(1, xxx.getChildNodes().size());
        assertTrue(xxx.getChildNodes().get(0) instanceof UnparsableStmt);
    }

    @Test
    void labeledStatementSemicolonRecovery() {
        CompilationUnit cu = parser.parse(ParseStart.COMPILATION_UNIT, provider("class X{int x(){aaa:X X X;}}"))
                .getResult()
                .get();
        LabeledStmt xxx = cu.getClassByName("X")
                .get()
                .getMethods()
                .get(0)
                .getBody()
                .get()
                .getStatements()
                .get(0)
                .asLabeledStmt();
        assertEquals(UNPARSABLE, xxx.getStatement().getParsed());
    }

    @Test
    void testIncompleteClassParse() {
        CompilationUnit compilationUnit = parser.parse(getClass().getResourceAsStream("Sample.java"))
                .getResult()
                .get();
        assertFalse(compilationUnit.getTypes().isEmpty());
        assertFalse(compilationUnit.getTypes().get(0).getMembers().isEmpty());
    }

    @Test
    void testBodyRecoverIf() {
        CompilationUnit compilationUnit = parser.parse(
                        ParseStart.COMPILATION_UNIT, provider("class A { int a() { if() }}"))
                .getResult()
                .get();
        assertFalse(compilationUnit.getTypes().isEmpty());
        assertEquals(1, compilationUnit.getTypes().get(0).getMembers().size());
    }

    @Test
    void testBodyRecoverLevel() {
        CompilationUnit compilationUnit = parser.parse(
                        ParseStart.COMPILATION_UNIT, provider("class A { int a() { int b = if (true) {int c = 5;} }}"))
                .getResult()
                .get();
        assertFalse(compilationUnit.getTypes().isEmpty());
        assertEquals(1, compilationUnit.getTypes().get(0).getMembers().size());
    }
}
