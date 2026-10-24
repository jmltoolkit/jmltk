/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.jml;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Math modifiers: JMLtk accepts the <em>code_*_math</em> modifiers used by its own toolchain
 * (and KeY's SV-COMP translations), but deliberately rejects KeY's <em>spec_*_math</em> pragmas.
 *
 * <p>The {@code spec_*_math} tokens exist in the lexer but are not wired into any production
 * ({@code jmlModifiersInner} only knows {@code code_*_math}), so KeY style math modes
 * ({@code /*@ spec_java_math @*&#47;}, {@code /*@ spec_bigint_math @*&#47;}, {@code /*@ spec_safe_math @*&#47;})
 * are parse errors in every position: as a standalone class/modifier pragma, as a class-level
 * clause, and as an inline JML modifier.</p>
 *
 * <p>Regression resources: {@code mathmods/TestClass.java}, {@code mathmods/Test.java} are the
 * KeY test sources {@code key.core/src/test/resources/testcase/speclang/testPackage/TestClass.java}
 * resp. {@code .../testcase/specMath/bigint/Test.java} (commit {@code 3b66e4e}). Both rely on a
 * math-mode pragma (class-level {@code spec_java_math} resp. {@code spec_bigint_math}) and, for
 * {@code Test.java}, one model-method and one method {@code spec_java_math} modifier; KeY stripped
 * them only in a later handoff (commit {@code 248b69f4}) explicitly because JMLtk does not accept
 * standalone spec-math comments. These tests pin down that the un-stripped sources must not parse.</p>
 */
public class JmlMathModifierTest {

    private static final Path MATHMODS = Paths.get("src/test/resources/com/github/javaparser/jml/mathmods");

    private static ParseResult<CompilationUnit> parse(String code) {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setProcessJml(true);
        return new JavaParser(cfg).parse(code);
    }

    private static String problems(ParseResult<CompilationUnit> r) {
        return r.getProblems().stream().map(p -> p.getMessage()).collect(Collectors.joining("\n"));
    }

    private static String read(String name) throws IOException {
        return Files.readString(MATHMODS.resolve(name));
    }

    private static void assertRejected(String reason, String code, String mathToken) {
        ParseResult<CompilationUnit> r = parse(code);
        Assertions.assertFalse(r.isSuccessful(), reason + " must not parse:\n" + problems(r));
        Assertions.assertTrue(
                problems(r).contains(mathToken),
                reason + ": expected a problem mentioning " + mathToken + " but got:\n" + problems(r));
    }

    private static void assertAccepted(String reason, String code) {
        ParseResult<CompilationUnit> r = parse(code);
        Assertions.assertTrue(r.isSuccessful(), reason + " must parse:\n" + problems(r));
    }

    /** The KeY speclang test class declares the math mode on the class: {@code public /*@ spec_java_math @*&#47; class}. */
    @Test
    void keiTestClassWithSpecJavaMathModifierRejected() throws IOException {
        assertRejected("KeY speclang TestClass.java with class-level spec_java_math", read("TestClass.java"), "spec_java_math");
    }

    /** The KeY specMath/bigint test starts with a class-level spec_bigint_math pragma and uses spec_java_math twice more. */
    @Test
    void keiBigIntMathTestRejected() throws IOException {
        String src = read("Test.java");
        ParseResult<CompilationUnit> r = parse(src);
        Assertions.assertFalse(r.isSuccessful(), "KeY specMath/bigint/Test.java must not parse:\n" + problems(r));
        Assertions.assertTrue(
                problems(r).contains("spec_bigint_math") && problems(r).contains("spec_java_math"),
                "expected problems mentioning both spec_bigint_math and spec_java_math, got:\n" + problems(r));
    }

    /** Control: without the math modifiers the very same sources parse (that is what KeY's stripped variants rely on). */
    @Test
    void keiFilesStrippedOfMathModifiersParse() throws IOException {
        String tc = read("TestClass.java").replace("public /*@ spec_java_math @*/ class TestClass", "public class TestClass");
        assertAccepted("stripped TestClass.java", tc);

        String big = read("Test.java")
                .replace("/*@ spec_bigint_math @*/", "")
                .replace("static spec_java_math model boolean testModel", "static model boolean testModel")
                .replace("public /*@ spec_java_math @*/ void testJavaMath()", "public void testJavaMath()");
        assertAccepted("stripped Test.java", big);
    }

    /** Standalone class-level spec_*_math pragmas are rejected. */
    @Test
    void specMathPragmasRejected() {
        for (String mode : List.of("bigint", "java", "safe")) {
            String token = "spec_" + mode + "_math";
            assertRejected("standalone " + token + " pragma",
                    "/*@ " + token + " @*/ class A {}",
                    token);
        }
    }

    /**
     * spec_*_math used as a clause inside a class-level JML comment is rejected. The failure
     * surfaces as a cascade ("unexpected token public ... Was expecting: (") because the clause
     * opener is parsed as an unsupported modifier position, so only the parse failure itself is
     * asserted here (the token-level messages are asserted by the standalone and method-level tests).
     */
    @Test
    void specMathAsClassLevelClauseRejected() {
        for (String mode : List.of("bigint", "java", "safe")) {
            String token = "spec_" + mode + "_math";
            ParseResult<CompilationUnit> r = parse(
                    "class A { /*@ " + token + "\n  @ model public int m();\n  @*/ int i; }");
            Assertions.assertFalse(r.isSuccessful(), "class-level " + token + " clause must not parse:\n" + problems(r));
        }
    }

    /** spec_*_math as a method-level JML comment (before a statement) is rejected. */
    @Test
    void specMathAsMethodLevelModifierRejected() {
        for (String mode : List.of("bigint", "java", "safe")) {
            String token = "spec_" + mode + "_math";
            assertRejected("method-level " + token + " comment",
                    "class A { void m() { //@ " + token + "\n int x = 1; } }",
                    token);
        }
    }

    /** code_*_math are JMLtk's supported math-mode modifiers (as class-level clauses). */
    @Test
    void codeMathModifiersAccepted() {
        for (String mode : List.of("bigint", "java", "safe")) {
            String token = "code_" + mode + "_math";
            ParseResult<CompilationUnit> r = parse(
                    "class A { /*@ " + token + "\n  @ model public int m();\n  @*/ int i; }");
            Assertions.assertTrue(r.isSuccessful(), "class-level " + token + " clause must parse:\n" + problems(r));
            Modifier.DefaultKeyword expected = switch (mode) {
                case "bigint" -> Modifier.DefaultKeyword.JML_CODE_BIGINT_MATH;
                case "java" -> Modifier.DefaultKeyword.JML_CODE_JAVA_MATH;
                default -> Modifier.DefaultKeyword.JML_CODE_SAFE_MATH;
            };
            Assertions.assertTrue(
                    r.getResult().orElseThrow().findAll(Modifier.class).stream()
                            .anyMatch(m -> m.getKeyword() == expected),
                    "expected a " + expected + " modifier in:\n" + r.getResult().orElseThrow());
        }
    }
}
