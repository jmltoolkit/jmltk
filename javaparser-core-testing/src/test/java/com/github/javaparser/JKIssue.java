/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JKIssue {
    @Test
    void test() throws IOException {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        JavaParser parser = new JavaParser(cfg);
        CompilationUnit cu = parser.parse(Paths.get("src/test/test_sourcecode/JKIssueDoubleContract.java"))
                .getResult()
                .get();

        var methods = cu.getPrimaryType().get().getMethods();
        for (var method : methods) {
            Assertions.assertEquals(1, method.getContracts().size());
        }
    }

    @Test
    void test2() throws IOException {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        JavaParser parser = new JavaParser(cfg);
        CompilationUnit cu = parser.parse(Paths.get("src/test/test_sourcecode/MissingParentSimpleExprClause.java"))
                .getResult()
                .get();

        var clause = cu.getType(0)
                .getMethods()
                .get(0)
                .getContracts()
                .get(0)
                .getClauses()
                .get(0)
                .asJmlSimpleExprClause();
        Assertions.assertEquals(1, clause.getChildNodes().size());
    }

    @Test
    void test3() throws IOException {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        JavaParser parser = new JavaParser(cfg);
        final var result = parser.parse(Paths.get("src/test/test_sourcecode/JKTmpTest.java"));
        result.getProblems().forEach(System.err::println);
        Assertions.assertTrue(result.isSuccessful());
    }
}
