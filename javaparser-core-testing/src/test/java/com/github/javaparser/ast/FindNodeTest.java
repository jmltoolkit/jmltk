/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Some tests for finding descendant and ancestor nodes.
 */
class FindNodeTest {
    @Test
    void testFindFirst() {
        CompilationUnit cu = parse("class Foo {\n" + "    void foo() {\n"
                + "        try {\n"
                + "        } catch (Exception e) {\n"
                + "        } finally {\n"
                + "            try {\n"
                + "            } catch (Exception e) {\n"
                + "                foo();\n"
                + "            } finally {\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "    }\n"
                + "}\n");

        // find the method call expression foo()
        MethodCallExpr actual = cu.findFirst(MethodCallExpr.class).orElse(null);

        MethodCallExpr expected = cu.getType(0)
                .getMember(0)
                .asMethodDeclaration()
                .getBody()
                .get()
                .getStatement(0)
                .asTryStmt()
                .getFinallyBlock()
                .get()
                .getStatement(0)
                .asTryStmt()
                .getCatchClauses()
                .get(0)
                .getBody()
                .getStatement(0)
                .asExpressionStmt()
                .getExpression()
                .asMethodCallExpr();

        assertEquals(expected, actual);
    }

    @Test
    void testFindAncestralFinallyBlock() {
        CompilationUnit cu = parse("class Foo {\n" + "    void foo() {\n"
                + "        try {\n"
                + "        } catch (Exception e) {\n"
                + "        } finally {\n"
                + "            try {\n"
                + "            } catch (Exception e) {\n"
                + "                foo();\n"
                + "            } finally {\n"
                + "            }\n"
                + "        }\n"
                + "\n"
                + "    }\n"
                + "}\n");

        // find the method call expression foo()
        MethodCallExpr methodCallExpr = cu.findFirst(MethodCallExpr.class).orElse(null);

        // find the finally block that the method call expression foo() is in
        Predicate<BlockStmt> predicate = (bs) -> {
            if (bs.getParentNode().isPresent() && bs.getParentNode().get() instanceof TryStmt) {
                TryStmt ancestralTryStmt = (TryStmt) bs.getParentNode().get();
                return bs == ancestralTryStmt.getFinallyBlock().orElse(null);
            }
            return false;
        };
        BlockStmt actual =
                methodCallExpr.findAncestor(predicate, BlockStmt.class).orElse(null);

        BlockStmt expected = cu.getType(0)
                .getMember(0)
                .asMethodDeclaration()
                .getBody()
                .get()
                .getStatement(0)
                .asTryStmt()
                .getFinallyBlock()
                .get();

        assertEquals(expected, actual);
    }
}
