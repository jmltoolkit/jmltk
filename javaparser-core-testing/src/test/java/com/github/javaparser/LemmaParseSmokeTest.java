/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
import com.github.javaparser.ast.jml.body.JmlLemmaKeyword;
import com.github.javaparser.ast.jml.proof.JmlProofCmdStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LemmaParseSmokeTest {

    private static CompilationUnit parse(String code) {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        ParseResult<CompilationUnit> result = new JavaParser(cfg).parse(code);
        Assertions.assertTrue(result.isSuccessful(), result.getProblems().toString());
        return result.getResult().get();
    }

    private static final String DEFINITION_SOURCE =
            "class Foo {\n" + "    //@ instance lemma Pred(int i) = i > 0;\n" + "}\n";

    private static final String ASSERTION_SOURCE =
            "class Foo {\n" + "    //@ lemma P(int i) assert i > 0 \\by { auto; };\n" + "}\n";

    private static final String SCRIPT_SOURCE =
            "class Foo {\n" + "    //@ public static no_state lemma nospace(\\seq s) \\by { oss; auto; };\n" + "}\n";

    private static final String BARE_SOURCE = "class Foo {\n" + "    //@ lemma helper();\n" + "}\n";

    @Test
    void definitionForm() {
        CompilationUnit cu = parse(DEFINITION_SOURCE);
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        Assertions.assertEquals("Pred", d.getName().asString());
        Assertions.assertEquals(
                JmlLemmaKeyword.Kind.DEFINITION, d.getKind().orElseThrow().getKeyword());
        Assertions.assertNotNull(d.getExpression().orElse(null));
        Assertions.assertTrue(d.getScript().isEmpty());
    }

    @Test
    void assertionForm() {
        CompilationUnit cu = parse(ASSERTION_SOURCE);
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        Assertions.assertEquals(
                JmlLemmaKeyword.Kind.ASSERTION, d.getKind().orElseThrow().getKeyword());
        Assertions.assertTrue(d.getScript().isPresent());
        Assertions.assertEquals(1, d.getScript().get().size());
    }

    @Test
    void scriptForm() {
        CompilationUnit cu = parse(SCRIPT_SOURCE);
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        Assertions.assertEquals("nospace", d.getName().asString());
        Assertions.assertEquals(
                JmlLemmaKeyword.Kind.SCRIPT, d.getKind().orElseThrow().getKeyword());
        Assertions.assertTrue(d.getExpression().isEmpty());
        Assertions.assertEquals(2, d.getScript().get().size());
    }

    @Test
    void bareForm() {
        CompilationUnit cu = parse(BARE_SOURCE);
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        Assertions.assertTrue(d.getKind().isEmpty());
        Assertions.assertTrue(d.getExpression().isEmpty());
        Assertions.assertTrue(d.getScript().isEmpty());
    }

    @Test
    void useLemmaInsideScript() {
        CompilationUnit cu =
                parse("class Foo {\n" + "    //@ lemma L(int x) \\by { use_lemma nospace(x); auto; };\n" + "}\n");
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        var script = d.getScript().orElseThrow(AssertionError::new);
        Assertions.assertEquals(2, script.size());
        JmlProofCmdStmt use = (JmlProofCmdStmt) script.getFirst();
        Assertions.assertEquals("use_lemma", use.getName().asString());
    }

    @Test
    void roundTrip() {
        parse(DEFINITION_SOURCE).toString();
        parse(ASSERTION_SOURCE).toString();
        parse(SCRIPT_SOURCE).toString();
        parse(BARE_SOURCE).toString();
    }
}
