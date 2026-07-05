/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4568Test extends AbstractResolutionTest {

    @Test
    void test() {

        String code = "public class Board {\n" + "\n"
                + "    class Field {\n"
                + "        private final Board board;\n"
                + "        private final int x;\n"
                + "        private final int y;\n"
                + "\n"
                + "        public Field(Board board, int x, int y) {\n"
                + "            this.board = board;\n"
                + "            this.x = x;\n"
                + "            this.y = y;\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    public static final int SIZE = 9;\n"
                + "    private final Field[] board;\n"
                + "\n"
                + "    public Board() {\n"
                + "        for (int x = 0; x < SIZE; x++)\n"
                + "            for (int y = 0; y < SIZE; y++)\n"
                + "                board[SIZE * y + x] = new Field(this, x, y);\n"
                + "    }\n"
                + "}";
        ParserConfiguration parserConfiguration =
                new ParserConfiguration().setSymbolResolver(symbolResolver(defaultTypeSolver()));

        CompilationUnit cu =
                JavaParserAdapter.of(new JavaParser(parserConfiguration)).parse(code);

        ObjectCreationExpr oce = cu.findFirst(ObjectCreationExpr.class).get();

        for (Expression e : oce.getArguments()) {
            if (e.isNameExpr()) {
                NameExpr ne = e.asNameExpr();
                final ResolvedValueDeclaration rvd = ne.resolve();
                assertEquals("int", rvd.getType().describe());
            }
        }
        final ResolvedConstructorDeclaration rcd = oce.resolve();
        assertEquals("Board.Field.Field(Board, int, int)", rcd.getQualifiedSignature());
    }
}
