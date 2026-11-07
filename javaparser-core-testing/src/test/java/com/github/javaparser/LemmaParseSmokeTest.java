/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
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

    private static final String SCRIPT_SOURCE =
            "class Foo {\n" + "    //@ public static no_state lemma nospace(\\seq s) \\by { oss; auto; };\n" + "}\n";

    @Test
    void scriptForm() {
        CompilationUnit cu = parse(SCRIPT_SOURCE);
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        Assertions.assertEquals("nospace", d.getName().asString());
        Assertions.assertEquals(2, d.getScript().size());
    }

    @Test
    void useLemmaInsideScript() {
        CompilationUnit cu =
                parse("class Foo {\n" + "    //@ lemma L(int x) \\by { use_lemma nospace(x); auto; };\n" + "}\n");
        JmlLemmaDeclaration d = cu.findAll(JmlLemmaDeclaration.class).getFirst();
        var script = d.getScript();
        Assertions.assertEquals(2, script.size());
        JmlProofCmdStmt use = (JmlProofCmdStmt) script.getFirst();
        Assertions.assertEquals("use_lemma", use.getName().asString());
    }

    @Test
    void definitionalFormIsRejected() {
        ParseResult<CompilationUnit> result = new JavaParser(new ParserConfiguration().setProcessJml(true))
                .parse("class Foo {\n" + "    //@ lemma Pred(int i) = i > 0;\n" + "}\n");
        Assertions.assertFalse(
                result.isSuccessful(), "the '= expr' definitional form is no longer part of the grammar");
    }

    @Test
    void assertedFormIsRejected() {
        ParseResult<CompilationUnit> result = new JavaParser(new ParserConfiguration().setProcessJml(true))
                .parse("class Foo {\n" + "    //@ lemma P(int i) assert i > 0 \\by { auto; };\n" + "}\n");
        Assertions.assertFalse(result.isSuccessful(), "the 'assert e' form is no longer part of the grammar");
    }

    @Test
    void bareFormIsRejected() {
        ParseResult<CompilationUnit> result = new JavaParser(new ParserConfiguration().setProcessJml(true))
                .parse("class Foo {\n" + "    //@ lemma helper();\n" + "}\n");
        Assertions.assertFalse(result.isSuccessful(), "a lemma requires a mandatory '\\by' proof script");
    }

    @Test
    void roundTrip() {
        parse(SCRIPT_SOURCE).toString();
    }
}
