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
 * Math-mode modifiers: JMLtk supports the JML <em>math mode</em> pragmas in both flavours that the
 * JML ecosystem uses — <em>code_*_math</em> (implementation math mode; used by OpenJML and KeY) and
 * <em>spec_*_math</em> (specification math mode; used by KeY).
 *
 * <p>All six keywords are lexed and wired into {@code jmlModifiersInner} (the grammar rule behind
 * {@code Modifiers()}), so they are accepted as Java/JML modifiers wherever {@code Modifiers()} is
 * used: as an inline class/method modifier ({@code public /*@ spec_java_math @*&#47; class X}), as a
 * standalone class-level pragma ({@code /*@ spec_bigint_math @*&#47;}), and as a clause inside a
 * class-level JML comment.</p>
 *
 * <p>Regression resources: {@code mathmods/TestClass.java}, {@code mathmods/Test.java} are the
 * KeY test sources {@code key.core/src/test/resources/testcase/speclang/testPackage/TestClass.java}
 * resp. {@code .../testcase/specMath/bigint/Test.java} (commit {@code 3b66e4e}). Both rely on a
 * math-mode pragma (class-level {@code spec_java_math} resp. {@code spec_bigint_math}) and, for
 * {@code Test.java}, one model-method and one method {@code spec_java_math} modifier. These tests
 * pin down that the un-stripped KeY sources parse and carry the expected math-mode modifiers.</p>
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

    private static void assertMathModifier(
            String reason, String code, Modifier.DefaultKeyword expected, int expectedCount) {
        ParseResult<CompilationUnit> r = parse(code);
        Assertions.assertTrue(r.isSuccessful(), reason + " must parse:\n" + problems(r));
        long count = r.getResult().orElseThrow().findAll(Modifier.class).stream()
                .filter(m -> m.getKeyword() == expected)
                .count();
        Assertions.assertEquals(
                expectedCount,
                count,
                reason + ": expected " + expectedCount + "x " + expected + " in:\n"
                        + r.getResult().orElseThrow());
    }

    /** The KeY speclang test class declares the math mode on the class: {@code public /*@ spec_java_math @*&#47; class}. */
    @Test
    void keYTestClassWithSpecJavaMathModifierParses() throws IOException {
        assertMathModifier(
                "KeY speclang TestClass.java", read("TestClass.java"), Modifier.DefaultKeyword.JML_SPEC_JAVA_MATH, 1);
    }

    /** The KeY specMath/bigint test: class pragma spec_bigint_math, model-method + method spec_java_math. */
    @Test
    void keYBigIntMathTestParses() throws IOException {
        String src = read("Test.java");
        ParseResult<CompilationUnit> r = parse(src);
        Assertions.assertTrue(r.isSuccessful(), "KeY specMath/bigint/Test.java must parse:\n" + problems(r));

        List<Modifier.Keyword> mathMods = r.getResult().orElseThrow().findAll(Modifier.class).stream()
                .map(Modifier::getKeyword)
                .filter(k -> k == Modifier.DefaultKeyword.JML_SPEC_BIGINT_MATH
                        || k == Modifier.DefaultKeyword.JML_SPEC_JAVA_MATH
                        || k == Modifier.DefaultKeyword.JML_SPEC_SAFE_MATH)
                .toList();
        Assertions.assertEquals(
                3,
                mathMods.size(),
                "expected 3 spec_*_math modifiers (class pragma + model method + method), got: " + mathMods);
    }

    /** Standalone class-level spec_*_math pragmas parse and attach the modifier to the class. */
    @Test
    void specMathPragmasParsed() {
        for (String mode : List.of("bigint", "java", "safe")) {
            Modifier.DefaultKeyword expected = specKeyword(mode);
            assertMathModifier(
                    "standalone spec_" + mode + "_math pragma",
                    "/*@ spec_" + mode + "_math @*/ class A {}",
                    expected,
                    1);
        }
    }

    /** spec_*_math used as a clause inside a class-level JML comment (model method declaration). */
    @Test
    void specMathAsClassLevelClauseParsed() {
        for (String mode : List.of("bigint", "java", "safe")) {
            Modifier.DefaultKeyword expected = specKeyword(mode);
            assertMathModifier(
                    "class-level spec_" + mode + "_math clause",
                    "class A { /*@ spec_" + mode + "_math\n  @ model public int m();\n  @*/ int i; }",
                    expected,
                    1);
        }
    }

    /**
     * The canonical OpenJML form sets both modes on one class: {@code /*@ code_java_math spec_java_math @*&#47; public class X}.
     * The inline modifier may also sit on the same line as the method it annotates.
     */
    @Test
    void combinedCodeAndSpecMathOnClassAndInlineMethodParsed() {
        ParseResult<CompilationUnit> r = parse("/*@ code_java_math spec_java_math @*/ public class TestJava {}");
        Assertions.assertTrue(r.isSuccessful(), "combined class pragma must parse:\n" + problems(r));
        Assertions.assertEquals(
                2,
                r.getResult().orElseThrow().findAll(Modifier.class).stream()
                        .filter(m -> m.getKeyword() == Modifier.DefaultKeyword.JML_CODE_JAVA_MATH
                                || m.getKeyword() == Modifier.DefaultKeyword.JML_SPEC_JAVA_MATH)
                        .count(),
                "expected code_java_math + spec_java_math on the class:\n"
                        + r.getResult().orElseThrow());

        assertMathModifier(
                "single-line inline method modifier",
                "class A { /*@ spec_java_math */public void m() {} }",
                Modifier.DefaultKeyword.JML_SPEC_JAVA_MATH,
                1);
    }

    /**
     * A method-level JML comment that carries a math modifier before a plain statement stays rejected:
     * statements cannot carry modifiers ("You passed a JML modifier but the following statement is not
     * able to carry modifiers"). This is a general JMLtk limitation of statement-level JML, not specific
     * to math modifiers.
     */
    @Test
    void specMathAsMethodLevelCommentBeforeStatementRejected() {
        for (String mode : List.of("bigint", "java", "safe")) {
            ParseResult<CompilationUnit> r = parse("class A { void m() { //@ spec_" + mode + "_math\n int x = 1; } }");
            Assertions.assertFalse(
                    r.isSuccessful(),
                    "method-level spec_" + mode + "_math before a statement must not parse:\n" + problems(r));
        }
    }

    /** code_*_math are JMLtk's other supported math-mode modifiers (as class-level clauses). */
    @Test
    void codeMathModifiersAccepted() {
        for (String mode : List.of("bigint", "java", "safe")) {
            Modifier.DefaultKeyword expected =
                    switch (mode) {
                        case "bigint" -> Modifier.DefaultKeyword.JML_CODE_BIGINT_MATH;
                        case "java" -> Modifier.DefaultKeyword.JML_CODE_JAVA_MATH;
                        default -> Modifier.DefaultKeyword.JML_CODE_SAFE_MATH;
                    };
            assertMathModifier(
                    "class-level code_" + mode + "_math clause",
                    "class A { /*@ code_" + mode + "_math\n  @ model public int m();\n  @*/ int i; }",
                    expected,
                    1);
        }
    }

    private static Modifier.DefaultKeyword specKeyword(String mode) {
        return switch (mode) {
            case "bigint" -> Modifier.DefaultKeyword.JML_SPEC_BIGINT_MATH;
            case "java" -> Modifier.DefaultKeyword.JML_SPEC_JAVA_MATH;
            default -> Modifier.DefaultKeyword.JML_SPEC_SAFE_MATH;
        };
    }
}
