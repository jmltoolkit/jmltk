/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.proof.*;
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Parsing and round-tripping of KeY-style proof scripts in {@code //@ assert ... \by { ... };}.
 */
public class JmlProofScriptTest {

    private static final String SOURCE = "class Foo {\n"
            + "    void m(int[] a, int k, int mc, int mx) {\n"
            + "        //@ assert (\\forall int x; x != mx; 2 * count(a, k+1, x) <= k+1 - mc) \\by { oss; obtain int x \\from_goal; expand on: \"self.count(a, k_0 + 1, x)\"; macro \"nosplit-prop\"; instantiate var:\"x\" with: x; auto classAxioms:false; };\n"
            + "    }\n"
            + "}\n";

    private static CompilationUnit parse(String code) {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        ParseResult<CompilationUnit> result = new JavaParser(cfg).parse(code);
        Assertions.assertTrue(result.isSuccessful(), result.getProblems().toString());
        return result.getResult().get();
    }

    @Test
    void parseProofScript() {
        CompilationUnit cu = parse(SOURCE);
        JmlExpressionStmt stmt = cu.findAll(JmlExpressionStmt.class).getFirst();
        var script = stmt.getScript().orElseThrow(AssertionError::new);
        Assertions.assertEquals(6, script.size());

        var obtain = (JmlProofObtainStmt) script.get(1);
        Assertions.assertEquals("x", obtain.getVar().asString());
        Assertions.assertEquals(JmlProofObtainStmt.ObtainKind.FROM_GOAL, obtain.getKind());

        var instantiate = (JmlProofCmdStmt) script.get(4);
        Assertions.assertEquals("instantiate", instantiate.getName().asString());
        Assertions.assertEquals(2, instantiate.getArgs().size());
        JmlProofArg var = instantiate.getArgs().getFirst();
        Assertions.assertEquals(
                "var", var.getLabel().orElseThrow(AssertionError::new).asString());
    }

    @Test
    void roundTrip() {
        CompilationUnit cu1 = parse(SOURCE);
        String printed = cu1.toString();
        CompilationUnit cu2 = parse(printed);
        Assertions.assertEquals(cu1, cu2);
    }

    @Test
    void proofCmdCases() {
        CompilationUnit cu = parse("""
            class Foo {
                void m() {
                    //@ assert true \\by {
                    //@     case "a":
                    //@         expand on: "x";
                    //@     default: oss;
                    //@     case: auto;
                    //@ };
                }
            }""");
        var script = cu.findAll(JmlExpressionStmt.class).getFirst().getScript().orElseThrow(AssertionError::new);
        Assertions.assertFalse(script.isEmpty());
        var cases = (JmlProofCaseStmt) script.getFirst();

        JmlProofCaseStmt a = (JmlProofCaseStmt) script.getFirst();
        Assertions.assertEquals(
                "a", a.getLabel().orElseThrow(AssertionError::new).getValue());

        JmlProofCaseStmt def = (JmlProofCaseStmt) script.get(1);
        Assertions.assertTrue(def.getLabel().isEmpty());
        Assertions.assertEquals(1, def.getCommands().size());
        Assertions.assertEquals(
                "oss",
                ((JmlProofCmdStmt) def.getCommands().getFirst()).getName().asString());

        String printed = cu.toString();
        System.out.println(printed);
        CompilationUnit cu2 = parse(printed);
        Assertions.assertEquals(cu, cu2);
    }

    @Test
    void obtainSuchThat() {
        CompilationUnit cu = parse("class Foo {\n" + "    void m() {\n"
                + "        //@ assert true \\by { obtain int x \\such_that x > 0; };\n"
                + "    }\n"
                + "}\n");
        var obtain = (JmlProofObtainStmt) cu.findAll(JmlExpressionStmt.class)
                .getFirst()
                .getScript()
                .orElseThrow(AssertionError::new)
                .getFirst();
        Assertions.assertEquals(JmlProofObtainStmt.ObtainKind.SUCH_THAT, obtain.getKind());
        CompilationUnit cu2 = parse(cu.toString());
        Assertions.assertEquals(cu, cu2);
    }
}
