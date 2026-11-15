# `jmltk-pp` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `jmltk-pp` CLI (and `jmltk pp` subcommand) that reads Java or JML from files/stdin, optionally pretty-prints it, and renders plain text, 256-color ANSI, Pygments-class HTML, or LaTeX.

**Architecture:** A pure pipeline in `:tools:prettyprinting`: parse/detect snippets with JavaParser, optionally format (`JmlFmt` for compilation units, the JavaParser printer for snippets), classify the JavaParser token stream into Pygments style classes (with a small JML sub-lexer for `/*@ ... @*/`), then hand the resulting `Span` list to one renderer. Clikt wraps the facade for both the standalone launcher and the aggregate CLI.

**Tech Stack:** Kotlin 2.4.21, JDK 21, JavaParser/JMLtk core, google-java-format 1.35.0, Clikt 5.1.0, JUnit 5 + AssertJ.

**Spec:** `doc/design/prettyprinting-cli.md`

## Global Constraints

- JDK 21 toolchain; Kotlin 2.4.21; use the Gradle wrapper only (`./gradlew`).
- All new Kotlin files go under `io.github.jmltoolkit.prettyprinting` (except `PpCommand` consumers in `tools/cli`), 4-space indent, and start with the license header from `gradle/header`.
- Default output is plain text; default pretty-print width is `100`; ANSI is 256-color; HTML carries Pygments class names only (no bundled CSS).
- LaTeX `--minted` emits `\PYG{<class>}{...}` body content; `--latex-raw` emits `\jml<cls>{...}` macros with digit renaming `c1 -> cOne`, `s1 -> sOne`, `s2 -> sTwo`.
- Never hand-edit generated sources.
- Every user-visible change needs a `jmltk-changelog.md` entry under `Unreleased`/`Added`.
- Formatting gate: `./gradlew spotlessCheck`; relevant tests: `./gradlew :tools:prettyprinting:test :tools:cli:compileKotlin`.

## Review Focus

These are the inputs most likely to break a user's expectations; each must be pinned by a test in the task that owns the code.

1. **JML line comments (`//@ ...`)** and block comments (`/*@ ... @*/`) must survive highlighting exactly, including when they start a snippet and are not part of the node's own token range. (Task 5)
2. **Unicode and HTML-significant characters** (`<`, `>`, `&`, non-ASCII identifiers/strings) must not be mangled or double-escaped. (Task 6)
3. **LaTeX special characters** (`{`, `}`, `\`, `_`, `%`, `&`, `$`, `#`, `^`, `~`) must not break compilation of either LaTeX mode. (Task 8)
4. **Unparseable input without `--pretty-print`** must still render (plain fallback) and exit successfully, rather than crash. (Task 9)
5. **Input boundaries**: missing trailing newline, CRLF, multiple files, and `-`/stdin must not lose or duplicate content. (Tasks 6 and 10)

---

### Task 1: Module wiring and `JmlFmt` fix

**Files:**
- Move: `tools/prettyprinting/src/main/kotlin/JmlFmt.kt` -> `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/JmlFmt.kt`
- Create: `tools/prettyprinting/src/main/java/com/google/googlejavaformat/java/JmltkFormatterBridge.java`
- Modify: `tools/prettyprinting/build.gradle.kts`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/JmlFmtTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces:
  - `class JmlFmt { fun pp(x: String, width: Int = 100): String }` in package `io.github.jmltoolkit.prettyprinting`.
  - `public static String JmltkFormatterBridge.format(String text, CommentsHelper helper, int maxWidth) throws FormatterException` in package `com.google.googlejavaformat.java` (returns the formatted text with one trailing newline).

- [ ] **Step 1: Move the file and add the package declaration**

```bash
mkdir -p tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting
git mv tools/prettyprinting/src/main/kotlin/JmlFmt.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/JmlFmt.kt
```

Add `package io.github.jmltoolkit.prettyprinting` after the license header in the moved file.

- [ ] **Step 2: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JmlFmtTest {
    @Test
    fun `formats a whole compilation unit`() {
        val out = JmlFmt().pp("public class A { int f(){return 1;} }", 100)
        val expected = """
            public class A {
              int f() {
                return 1;
              }
            }
        """.trimIndent() + "\n"
        assertThat(out).isEqualTo(expected)
    }

    @Test
    fun `preserves jml comments`() {
        val out = JmlFmt().pp("public class A { /*@ requires x > 0; @*/ int f(){return 1;} }", 100)
        assertThat(out).contains("/*@ requires x > 0; @*/")
        assertThat(out).contains("int f()")
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.JmlFmtTest"`
Expected: FAIL on the first assertion, because `pp` currently returns only `output.getLine(0)`.

- [ ] **Step 4: Add the formatter bridge**

The existing manual pipeline is missing the AST visitor (`OpsBuilder.drain()` without `JavaInputAstVisitor` just echoes the input). `Trees.parse` is package-private, so add a small Java bridge in google-java-format's package:

`tools/prettyprinting/src/main/java/com/google/googlejavaformat/java/JmltkFormatterBridge.java`:

```java
/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.google.googlejavaformat.java;

import com.google.googlejavaformat.CommentsHelper;
import com.google.googlejavaformat.Doc;
import com.google.googlejavaformat.DocBuilder;
import com.google.googlejavaformat.OpsBuilder;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Calls the package-private google-java-format pieces needed to run the formatter with a custom
 * {@link CommentsHelper}. This shim lives in google-java-format's package only because {@code
 * Trees.parse} is package-private; it is not a fork of the formatter.
 */
public final class JmltkFormatterBridge {
    private JmltkFormatterBridge() {}

    public static String format(String text, CommentsHelper helper, int maxWidth) throws FormatterException {
        JavaInput input = new JavaInput(text);
        Context context = new Context();
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        JCCompilationUnit unit = Trees.parse(context, errors, false, input.getText());
        input.setCompilationUnit(unit);
        if (!errors.isEmpty()) {
            throw FormatterException.fromJavacDiagnostics(errors);
        }

        JavaOutput output = new JavaOutput("\n", input, helper);
        OpsBuilder builder = new OpsBuilder(input, output);
        JavaInputAstVisitor visitor =
                new JavaInputAstVisitor(builder, JavaFormatterOptions.defaultOptions().indentationMultiplier());
        visitor.scan(unit, null);
        builder.sync(input.getText().length());
        builder.drain();

        Doc doc = new DocBuilder().withOps(builder.build()).build();
        doc.computeBreaks(output.getCommentsHelper(), maxWidth, new Doc.State(+0, 0));
        doc.write(output);
        output.flush();

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < output.getLineCount(); i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(output.getLine(i));
        }
        return result.append('\n').toString();
    }
}
```

Then reduce `JmlFmt` to:

```kotlin
fun pp(x: String, width: Int = 100): String =
    JmltkFormatterBridge.format(
        x,
        JmlCommentsHelper("\n", JavaFormatterOptions.defaultOptions()),
        width,
    )
```

Add the imports `com.google.googlejavaformat.java.JavaFormatterOptions` and `com.google.googlejavaformat.java.JmltkFormatterBridge`, and remove the now-unused `JavaInput`/`JavaOutput`/`OpsBuilder`/`DocBuilder` imports. `JmlCommentsHelper` stays as-is; its `rewrite` returns `/*@` blocks unchanged, which preserves their internal indentation.

- [ ] **Step 5: Add the google-java-format JVM exports to compile and test**

In `tools/prettyprinting/build.gradle.kts`:

```kotlin
val googleJavaFormatExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
)

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(googleJavaFormatExports)
}

tasks.test {
    jvmArgs(googleJavaFormatExports)
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.JmlFmtTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add tools/prettyprinting/build.gradle.kts tools/prettyprinting/src/main/java tools/prettyprinting/src/main/kotlin tools/prettyprinting/src/test/kotlin
git commit -m "fix(prettyprinting): move JmlFmt into its package and run the formatter AST visitor"
```

---

### Task 2: Style classes, Pygments palette, and spans

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/StyleClass.kt`
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Span.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/StyleClassTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum class StyleClass(val cssClass: String)` with the values used by later tasks (see below).
  - `val StyleClass.latexMacro: String`.
  - `data class StyleAttributes(val rgb: Int?, val bold: Boolean = false, val italic: Boolean = false)`.
  - `internal val pygmentsDefault: Map<StyleClass, StyleAttributes>`.
  - `internal fun xterm256(rgb: Int): Int`.
  - `data class Span(val text: String, val style: StyleClass = StyleClass.TEXT)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StyleClassTest {
    @Test
    fun `exposes pygments css names`() {
        assertThat(StyleClass.KEYWORD_DECLARATION.cssClass).isEqualTo("kd")
        assertThat(StyleClass.KEYWORD_TYPE.cssClass).isEqualTo("kt")
        assertThat(StyleClass.NAME_FUNCTION.cssClass).isEqualTo("nf")
        assertThat(StyleClass.COMMENT_SINGLE.cssClass).isEqualTo("c1")
        assertThat(StyleClass.TEXT.cssClass).isEmpty()
    }

    @Test
    fun `renames digit classes for latex`() {
        assertThat(StyleClass.COMMENT_SINGLE.latexMacro).isEqualTo("cOne")
        assertThat(StyleClass.KEYWORD_DECLARATION.latexMacro).isEqualTo("kd")
        assertThat(StyleClass.STRING.latexMacro).isEqualTo("s")
    }

    @Test
    fun `maps pygments default colors to xterm 256`() {
        assertThat(xterm256(0x008000)).isEqualTo(2)
        assertThat(xterm256(0x666666)).isEqualTo(241)
        assertThat(xterm256(0xB00040)).isEqualTo(125)
        assertThat(xterm256(0x3D7B7B)).isEqualTo(66)
    }

    @Test
    fun `has colors and attributes for java classes`() {
        assertThat(pygmentsDefault[StyleClass.KEYWORD]).isEqualTo(StyleAttributes(0x008000, bold = true))
        assertThat(pygmentsDefault[StyleClass.KEYWORD_TYPE]).isEqualTo(StyleAttributes(0xB00040))
        assertThat(pygmentsDefault[StyleClass.COMMENT_MULTILINE]).isEqualTo(
            StyleAttributes(0x3D7B7B, italic = true)
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.StyleClassTest"`
Expected: FAIL to compile (`StyleClass`, `xterm256`, `pygmentsDefault` unresolved).

- [ ] **Step 3: Implement `StyleClass.kt` and `Span.kt`**

`StyleClass` values (keep this order; the preamble iterates it):

```kotlin
enum class StyleClass(val cssClass: String) {
    TEXT(""),
    KEYWORD("k"),
    KEYWORD_DECLARATION("kd"),
    KEYWORD_TYPE("kt"),
    KEYWORD_CONSTANT("kc"),
    KEYWORD_NAMESPACE("kn"),
    NAME("n"),
    NAME_ATTRIBUTE("na"),
    NAME_BUILTIN("nb"),
    NAME_CLASS("nc"),
    NAME_DECORATOR("nd"),
    NAME_FUNCTION("nf"),
    NAME_LABEL("nl"),
    NAME_NAMESPACE("nn"),
    NUMBER("m"),
    NUMBER_BINARY("mb"),
    NUMBER_FLOAT("mf"),
    NUMBER_HEX("mh"),
    NUMBER_INTEGER("mi"),
    NUMBER_OCT("mo"),
    STRING("s"),
    STRING_CHAR("sc"),
    STRING_ESCAPE("se"),
    OPERATOR("o"),
    PUNCTUATION("p"),
    COMMENT("c"),
    COMMENT_SINGLE("c1"),
    COMMENT_MULTILINE("cm"),
    COMMENT_SPECIAL("cs"),
    ERROR("err");

    val latexMacro: String
        get() = when (this) {
            COMMENT_SINGLE -> "cOne"
            else -> cssClass
        }
}
```

Palette (Pygments `default`), exact entries needed:

```kotlin
internal val pygmentsDefault: Map<StyleClass, StyleAttributes> = mapOf(
    StyleClass.KEYWORD to StyleAttributes(0x008000, bold = true),
    StyleClass.KEYWORD_DECLARATION to StyleAttributes(0x008000, bold = true),
    StyleClass.KEYWORD_CONSTANT to StyleAttributes(0x008000, bold = true),
    StyleClass.KEYWORD_NAMESPACE to StyleAttributes(0x008000, bold = true),
    StyleClass.KEYWORD_TYPE to StyleAttributes(0xB00040),
    StyleClass.NAME_ATTRIBUTE to StyleAttributes(0x687822),
    StyleClass.NAME_BUILTIN to StyleAttributes(0x008000),
    StyleClass.NAME_CLASS to StyleAttributes(0x0000FF, bold = true),
    StyleClass.NAME_DECORATOR to StyleAttributes(0xAA22FF),
    StyleClass.NAME_FUNCTION to StyleAttributes(0x0000FF),
    StyleClass.NAME_LABEL to StyleAttributes(0x767600),
    StyleClass.NAME_NAMESPACE to StyleAttributes(0x0000FF, bold = true),
    StyleClass.NUMBER to StyleAttributes(0x666666),
    StyleClass.NUMBER_BINARY to StyleAttributes(0x666666),
    StyleClass.NUMBER_FLOAT to StyleAttributes(0x666666),
    StyleClass.NUMBER_HEX to StyleAttributes(0x666666),
    StyleClass.NUMBER_INTEGER to StyleAttributes(0x666666),
    StyleClass.NUMBER_OCT to StyleAttributes(0x666666),
    StyleClass.STRING to StyleAttributes(0xBA2121),
    StyleClass.STRING_CHAR to StyleAttributes(0xBA2121),
    StyleClass.STRING_ESCAPE to StyleAttributes(0xAA5D1F, bold = true),
    StyleClass.OPERATOR to StyleAttributes(0x666666),
    StyleClass.COMMENT to StyleAttributes(0x3D7B7B, italic = true),
    StyleClass.COMMENT_SINGLE to StyleAttributes(0x3D7B7B, italic = true),
    StyleClass.COMMENT_MULTILINE to StyleAttributes(0x3D7B7B, italic = true),
    StyleClass.COMMENT_SPECIAL to StyleAttributes(0x3D7B7B, italic = true),
    StyleClass.ERROR to StyleAttributes(0xFF0000),
)
```

`xterm256` builds the 256-entry palette once (16 system colors, the 6x6x6 cube `[0,95,135,175,215,255]`, then 24 grayscale steps `8 + 10*i`) and returns the index with the smallest squared RGB distance.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.StyleClassTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/StyleClass.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Span.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/StyleClassTest.kt
git commit -m "feat(prettyprinting): add Pygments style classes and spans"
```

---

### Task 3: JML sub-lexer

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/JmlLexer.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/JmlLexerTest.kt`

**Interfaces:**
- Consumes: `Span`, `StyleClass`.
- Produces: `class JmlLexer { fun lex(text: String): List<Span> }`. The concatenation of `Span.text` must equal the input exactly.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JmlLexerTest {
    @Test
    fun `lexes clause keywords operators and backslash names`() {
        val input = "requires x > 0 && \\old(a) != null;"
        val spans = JmlLexer().lex(input)

        assertThat(spans.joinToString("") { it.text }).isEqualTo(input)
        assertThat(styled(spans)).containsExactly(
            "requires" to StyleClass.KEYWORD_DECLARATION,
            "x" to StyleClass.NAME,
            ">" to StyleClass.OPERATOR,
            "0" to StyleClass.NUMBER_INTEGER,
            "&&" to StyleClass.OPERATOR,
            "\\old" to StyleClass.KEYWORD,
            "(" to StyleClass.PUNCTUATION,
            "a" to StyleClass.NAME,
            ")" to StyleClass.PUNCTUATION,
            "!=" to StyleClass.OPERATOR,
            "null" to StyleClass.KEYWORD_CONSTANT,
            ";" to StyleClass.PUNCTUATION,
        )
    }

    @Test
    fun `lexes jml specific operators and numbers`() {
        val spans = JmlLexer().lex("\\forall int i; i < n ==> a[i] == 0x10;")
        assertThat(styled(spans)).contains(
            "\\forall" to StyleClass.KEYWORD,
            "int" to StyleClass.KEYWORD_TYPE,
            "==>" to StyleClass.OPERATOR,
            "0x10" to StyleClass.NUMBER_HEX,
            "[" to StyleClass.OPERATOR,
            "]" to StyleClass.OPERATOR,
        )
    }

    @Test
    fun `splits string escapes`() {
        val input = "\"a\\nb\""
        val spans = JmlLexer().lex(input)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(input)
        assertThat(spans.filter { it.style == StyleClass.STRING_ESCAPE }.map { it.text })
            .containsExactly("\\n")
    }

    @Test
    fun `lexes comments`() {
        val spans = JmlLexer().lex("// note\nrequires true; /* done */")
        assertThat(spans.filter { it.style == StyleClass.COMMENT_SINGLE }.map { it.text })
            .containsExactly("// note")
        assertThat(spans.filter { it.style == StyleClass.COMMENT_MULTILINE }.map { it.text })
            .containsExactly("/* done */")
    }

    private fun styled(spans: List<Span>) = spans.filter { it.style != StyleClass.TEXT }.map { it.text to it.style }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.JmlLexerTest"`
Expected: FAIL to compile (`JmlLexer` unresolved).

- [ ] **Step 3: Implement the lexer**

A character scanner with these token rules, in order:

- whitespace run -> `Span(text, TEXT)`, or skip (either is fine as long as the concatenation invariant holds; prefer the span).
- `//` to end of line -> `COMMENT_SINGLE`.
- `/*` to `*/` -> `COMMENT_MULTILINE`.
- `"` or `'` with backslash escapes: emit normal runs as `STRING` / `STRING_CHAR`, each backslash plus its following character (or `\uXXXX`) as `STRING_ESCAPE`.
- numbers: `0x`/`0X` -> `NUMBER_HEX`; `0b`/`0B` -> `NUMBER_BINARY`; leading `0` with more digits -> `NUMBER_OCT`; contains `.`/`e`/`E` plus optional `f`/`d` -> `NUMBER_FLOAT`; otherwise `NUMBER_INTEGER`.
- identifiers: `[A-Za-z_$]` followed by `[A-Za-z0-9_$]*`, or `\` followed by letters/underscore. `\` names -> `KEYWORD`. `true`/`false`/`null` -> `KEYWORD_CONSTANT`. Clause keywords -> `KEYWORD_DECLARATION`. Logical words -> `KEYWORD`. Otherwise `NAME`.
- clause keyword set: `requires`, `ensures`, `assignable`, `modifies`, `accessible`, `measured_by`, `captures`, `drains`, `duration`, `working_space`, `when`, `signals`, `diverges`, `decreases`, `continues`, `breaks`, `returns`, `also`, `behavior`, `normal_behavior`, `exceptional_behavior`, `private_behavior`, `public_behavior`, `invariant`, `constraint`, `represents`, `initially`, `loop_invariant`, `maintaining`, `loop_decreases`, `loop_when`, `assert`, `assume`, `set`, `model`, `ghost`, `helper`, `pure`, `spec_public`, `spec_protected`.
- logical keyword set: `forall`, `exists`, `old`, `result`, `fresh`, `nothing`, `everything`, `sum`, `product`, `max`, `min`, `num_of`, `such_that`, `reach`, `typeof`, `type`, `elemtype`, `nonnullelements`, `strictly_nothing`.
- operators, longest match first: `==>`, `<==>`, `<:`, `->`, `..`, `==`, `!=`, `<=`, `>=`, `&&`, `||`, `+=`, `-=`, `*=`, `/=`, `%=`, `+`, `-`, `*`, `/`, `%`, `!`, `?`, `:`, `=`, `<`, `>`, `.`, `|`, `&`, `^`, `~` -> `OPERATOR`.
- punctuation: `{}();,@` -> `PUNCTUATION`; `[` and `]` -> `OPERATOR` (match the Java mapping).
- anything else -> `Span(char, TEXT)`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.JmlLexerTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/JmlLexer.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/JmlLexerTest.kt
git commit -m "feat(prettyprinting): add JML lexer"
```

---

### Task 4: Snippet detection and parsing

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/SnippetParser.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/SnippetParserTest.kt`

**Interfaces:**
- Consumes: `com.github.javaparser.JavaParser`, `ParserConfiguration`, `ParseResult`, `com.github.javaparser.ast.Node`, `TokenRange`.
- Produces:
  ```kotlin
  enum class Language { JAVA, JML }
  enum class SnippetKind { AUTO, CU, MEMBER, STMT, EXPR }
  enum class ParsedKind { COMPILATION_UNIT, MEMBER, STATEMENT, EXPRESSION, JML_EXPRESSION, JML_CLAUSE, PLAIN }

  data class ParsedSource(
      val source: String,
      val kind: ParsedKind,
      val nodes: List<Node>,
      val tokenRanges: List<TokenRange>,
      val stripWrapperNewlines: Boolean = false,
  )

  class SnippetParser {
      fun parse(
          text: String,
          language: Language = Language.JAVA,
          kind: SnippetKind = SnippetKind.AUTO,
      ): ParsedSource
  }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.Statement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SnippetParserTest {
    private val parser = SnippetParser()

    @Test
    fun `detects a compilation unit`() {
        val result = parser.parse("class A { int f() { return 1; } }")
        assertThat(result.kind).isEqualTo(ParsedKind.COMPILATION_UNIT)
        assertThat(result.nodes.single()).isInstanceOf(CompilationUnit::class.java)
    }

    @Test
    fun `detects a member`() {
        val result = parser.parse("int f() { return 1; }")
        assertThat(result.kind).isEqualTo(ParsedKind.MEMBER)
        assertThat(result.nodes.single().toString()).contains("int f()")
    }

    @Test
    fun `field wins over local variable`() {
        assertThat(parser.parse("int x = 1;").kind).isEqualTo(ParsedKind.MEMBER)
    }

    @Test
    fun `detects a statement`() {
        val result = parser.parse("return 1;")
        assertThat(result.kind).isEqualTo(ParsedKind.STATEMENT)
        assertThat(result.nodes.single()).isInstanceOf(Statement::class.java)
    }

    @Test
    fun `detects an expression`() {
        val result = parser.parse("1 + 2")
        assertThat(result.kind).isEqualTo(ParsedKind.EXPRESSION)
        assertThat(result.nodes.single()).isInstanceOf(Expression::class.java)
    }

    @Test
    fun `respects an explicit snippet kind`() {
        assertThat(parser.parse("1 + 2", kind = SnippetKind.CU).kind).isEqualTo(ParsedKind.PLAIN)
        assertThat(parser.parse("1 + 2", kind = SnippetKind.EXPR).kind).isEqualTo(ParsedKind.EXPRESSION)
    }

    @Test
    fun `parses jml expressions`() {
        val result = parser.parse("\\old(x) > 0", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_EXPRESSION)
        assertThat(result.nodes.single().toString()).isEqualTo("\\old(x) > 0")
    }

    @Test
    fun `parses jml clauses`() {
        val result = parser.parse("requires x > 0;", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_CLAUSE)
        assertThat(result.nodes.single().toString()).contains("requires x > 0;")
    }

    @Test
    fun `parses several jml clauses`() {
        val result = parser.parse("requires x > 0;\nensures x < 10;", language = Language.JML)
        assertThat(result.kind).isEqualTo(ParsedKind.JML_CLAUSE)
        assertThat(result.nodes).hasSize(2)
    }

    @Test
    fun `falls back to plain for invalid input`() {
        val result = parser.parse("class {")
        assertThat(result.kind).isEqualTo(ParsedKind.PLAIN)
        assertThat(result.nodes).isEmpty()
        assertThat(result.tokenRanges).isEmpty()
    }

    @Test
    fun `works out the exact snippet token range`() {
        val text = "/*@ requires x > 0; @*/\nint f() { return 1; }"
        val result = parser.parse(text)
        assertThat(result.kind).isEqualTo(ParsedKind.MEMBER)
        assertThat(result.stripWrapperNewlines).isTrue()
        val tokens = result.tokenRanges.single().iterator().asSequence().joinToString("") { it.text }
        assertThat(tokens.trim()).isEqualTo(text.trim())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.SnippetParserTest"`
Expected: FAIL to compile (`SnippetParser`, `ParsedKind`, ... unresolved).

- [ ] **Step 3: Implement the parser**

Use one `ParserConfiguration` with `setProcessJml(true)` and a fresh `JavaParser` per call.

Java parsing:

- CU: `JavaParser.parse(text)`; success when `ParseResult.isSuccessful` (result present and no problems).
- MEMBER: parse `class __JmltkSnippet {\n$text\n}`; take `getType(0).members.firstOrNull()`. Token range: from the token after the wrapper type's first `{` to the token before its last `}`; `stripWrapperNewlines = true`.
- STMT: parse `class __JmltkSnippet { void __m() {\n$text\n} }`; take the method body's first statement. Token range: inside the method body's `{ ... }`; `stripWrapperNewlines = true`.
- EXPR: parse `class __JmltkSnippet { Object __f = (\n$text\n); }`; take the `__f` initializer. Token range: inside the initializer's outer `( ... )`; `stripWrapperNewlines = true`.

Helper for token ranges:

```kotlin
private fun innerRange(range: TokenRange, open: String, close: String): TokenRange? {
    val tokens = range.iterator().asSequence().filter { it.text.isNotEmpty() }.toList()
    val first = tokens.indexOfFirst { it.text == open }
    val last = tokens.indexOfLast { it.text == close }
    if (first < 0 || last <= first) return null
    return TokenRange(tokens[first + 1], tokens[last - 1])
}
```

JML parsing (`Language.JML`), in order:

1. `parseJmlExpression(text)` -> one node, `JML_EXPRESSION`, token range from the node.
2. Split `text` on top-level `;` (track `()[]{}` depth and string/char/comment state), include the semicolon in each part. If more than one part results, parse every part with `parseJmlClause`; all must succeed, producing `JML_CLAUSE` with one node and token range per clause.
3. Otherwise `parseJmlClause(text)` -> one node, `JML_CLAUSE`, token range from the node.

For each node, `node.tokenRange` supplies the range (empty optional -> ignore that node). If no node survives, return `ParsedSource(text, ParsedKind.PLAIN, emptyList(), emptyList())`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.SnippetParserTest"`
Expected: PASS (11 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/SnippetParser.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/SnippetParserTest.kt
git commit -m "feat(prettyprinting): detect and parse Java and JML snippets"
```

---

### Task 5: Java/JML highlighter

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Highlighter.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/HighlighterTest.kt`

**Interfaces:**
- Consumes: `SnippetParser`, `ParsedSource`, `ParsedKind`, `JmlLexer`, `StyleClass`, `Span`, `JavaToken`, `JavaToken.Category`, `TokenRange`.
- Produces: `class Highlighter(private val jmlLexer: JmlLexer = JmlLexer()) { fun highlight(parsed: ParsedSource): List<Span> }`. The concatenation of `Span.text` equals the rendered source text (with exactly one wrapper newline removed for snippets).

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HighlighterTest {
    private val parser = SnippetParser()
    private val highlighter = Highlighter()

    private fun spans(src: String): List<Span> = highlighter.highlight(parser.parse(src))

    private fun styleOf(src: String, text: String): StyleClass =
        spans(src).first { it.text == text }.style

    @Test
    fun `concatenation preserves the source exactly`() {
        val src = "public class A { int f() { return 1; } }"
        assertThat(spans(src).joinToString("") { it.text }).isEqualTo(src)
    }

    @Test
    fun `classifies java tokens`() {
        val src = "public class A { int f() { return 1; } }"
        assertThat(styleOf(src, "public")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, "class")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, "A")).isEqualTo(StyleClass.NAME_CLASS)
        assertThat(styleOf(src, "int")).isEqualTo(StyleClass.KEYWORD_TYPE)
        assertThat(styleOf(src, "f")).isEqualTo(StyleClass.NAME_FUNCTION)
        assertThat(styleOf(src, "return")).isEqualTo(StyleClass.KEYWORD)
        assertThat(styleOf(src, "1")).isEqualTo(StyleClass.NUMBER_INTEGER)
    }

    @Test
    fun `classifies names after dots and annotations`() {
        val src = "class A { @Override public String toString() { return this.x.y; } }"
        assertThat(styleOf(src, "@")).isEqualTo(StyleClass.NAME_DECORATOR)
        assertThat(styleOf(src, "Override")).isEqualTo(StyleClass.NAME_DECORATOR)
        assertThat(styleOf(src, "y")).isEqualTo(StyleClass.NAME_ATTRIBUTE)
    }

    @Test
    fun `classifies number bases`() {
        val src = "class A { int a = 0xFF; int b = 0b101; int c = 0755; float d = 1.5f; }"
        assertThat(styleOf(src, "0xFF")).isEqualTo(StyleClass.NUMBER_HEX)
        assertThat(styleOf(src, "0b101")).isEqualTo(StyleClass.NUMBER_BINARY)
        assertThat(styleOf(src, "0755")).isEqualTo(StyleClass.NUMBER_OCT)
        assertThat(styleOf(src, "1.5f")).isEqualTo(StyleClass.NUMBER_FLOAT)
    }

    @Test
    fun `splits string escapes`() {
        val src = "class A { String s = \"a\\nb\"; }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.filter { it.style == StyleClass.STRING_ESCAPE }.map { it.text })
            .containsExactly("\\n")
    }

    @Test
    fun `classifies comments`() {
        val src = "// one\nclass A { /** doc */ /* block */ int f() { return 1; } }"
        assertThat(styleOf(src, "// one")).isEqualTo(StyleClass.COMMENT_SINGLE)
        assertThat(styleOf(src, "/** doc */")).isEqualTo(StyleClass.COMMENT_SPECIAL)
        assertThat(styleOf(src, "/* block */")).isEqualTo(StyleClass.COMMENT_MULTILINE)
    }

    @Test
    fun `lexes jml inside java comments`() {
        val src = "class A { /*@ requires x > 0; @*/ int f() { return 1; } }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(styleOf(src, "/*@")).isEqualTo(StyleClass.COMMENT_MULTILINE)
        assertThat(styleOf(src, "@*/")).isEqualTo(StyleClass.COMMENT_MULTILINE)
        assertThat(styleOf(src, "requires")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(styleOf(src, ">")).isEqualTo(StyleClass.OPERATOR)
        assertThat(styleOf(src, "0")).isEqualTo(StyleClass.NUMBER_INTEGER)
    }

    @Test
    fun `keeps a leading jml line comment in a member snippet`() {
        val src = "//@ requires x > 0;\nint f() { return 1; }"
        val spans = spans(src)
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.first().style).isEqualTo(StyleClass.COMMENT_SINGLE)
        assertThat(styleOf(src, "requires")).isEqualTo(StyleClass.KEYWORD_DECLARATION)
    }

    @Test
    fun `lexes jml snippets with jml keywords`() {
        val src = "requires \\old(x) > 0;"
        val spans = highlighter.highlight(parser.parse(src, language = Language.JML))
        assertThat(spans.joinToString("") { it.text }).isEqualTo(src)
        assertThat(spans.first { it.text == "requires" }.style).isEqualTo(StyleClass.KEYWORD_DECLARATION)
        assertThat(spans.first { it.text == "\\old" }.style).isEqualTo(StyleClass.KEYWORD)
    }

    @Test
    fun `falls back to one plain span`() {
        val src = "class {"
        val spans = spans(src)
        assertThat(spans).containsExactly(Span(src, StyleClass.TEXT))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.HighlighterTest"`
Expected: FAIL to compile (`Highlighter` unresolved).

- [ ] **Step 3: Implement `Highlighter`**

- `ParsedKind.PLAIN` or empty ranges -> `listOf(Span(parsed.source))`.
- `ParsedKind.JML_EXPRESSION` or `ParsedKind.JML_CLAUSE` -> `jmlLexer.lex(parsed.source)` (the Java token stream loses JML-specific names like `\old`).
- For each `TokenRange`, iterate `JavaToken`s. Skip empty texts.
- If a token text starts with `/*@` or `//@`, emit JML spans:
  - prefix `/*@` (or `//@`) as `COMMENT_MULTILINE` (or `COMMENT_SINGLE`);
  - `JmlLexer().lex(inner)` where `inner` is between the prefix and the suffix `@*/` / `*/` (JML line comments have no suffix);
  - suffix as the same comment class.
- Otherwise emit spans per token:
  - whitespace/EOL -> `TEXT`;
  - comments: `/**` -> `COMMENT_SPECIAL`, `//` -> `COMMENT_SINGLE`, else `COMMENT_MULTILINE`;
  - keywords: control set -> `KEYWORD`; declaration set -> `KEYWORD_DECLARATION`; primitives -> `KEYWORD_TYPE`; `true`/`false`/`null` -> `KEYWORD_CONSTANT`; `package`/`import` -> `KEYWORD_NAMESPACE`;
  - identifiers: previous `class`/`interface`/`enum`/`record`/`new` -> `NAME_CLASS`; previous `@` -> `NAME_DECORATOR`; previous `.` -> `NAME_ATTRIBUTE`; next `(` -> `NAME_FUNCTION`; next `:` -> `NAME_LABEL`; else `NAME`;
  - literals: as in Task 3's number rules; `"` -> `STRING` (split escapes into `STRING_ESCAPE` runs), `'` -> `STRING_CHAR`;
  - separators: `[`/`]` -> `OPERATOR`, `@` -> `NAME_DECORATOR`, else `PUNCTUATION`;
  - operators: `@` -> `NAME_DECORATOR`, else `OPERATOR`.
- Keyword sets: reuse the Pygments Java lexer lists. Control: `assert`, `break`, `case`, `catch`, `continue`, `default`, `do`, `else`, `finally`, `for`, `goto`, `if`, `instanceof`, `new`, `return`, `switch`, `this`, `throw`, `try`, `while`, `yield`. Declaration: `abstract`, `const`, `enum`, `exports`, `extends`, `final`, `implements`, `native`, `non-sealed`, `open`, `opens`, `permits`, `private`, `protected`, `provides`, `public`, `requires`, `sealed`, `static`, `strictfp`, `super`, `synchronized`, `throws`, `to`, `transient`, `transitive`, `uses`, `volatile`, `with`, `module`, `class`, `interface`, `record`, `var`. Types: `boolean`, `byte`, `char`, `double`, `float`, `int`, `long`, `short`, `void`.
- If `parsed.stripWrapperNewlines` is true, remove exactly one leading `\n` from the first `TEXT` span and one trailing `\n` from the last `TEXT` span, then drop empty spans.
- Between token ranges, insert `Span("\n")`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.HighlighterTest"`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Highlighter.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/HighlighterTest.kt
git commit -m "feat(prettyprinting): highlight Java and JML token streams"
```

---

### Task 6: Text and HTML renderers

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/TextRenderer.kt`
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/HtmlRenderer.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/TextHtmlRendererTest.kt`

**Interfaces:**
- Consumes: `Span`, `StyleClass`.
- Produces:
  - `class TextRenderer { fun render(text: String): String }` (exactly one trailing newline).
  - `class HtmlRenderer { fun renderFragment(spans: List<Span>): String; fun renderDocument(fragment: String, css: String?): String }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TextHtmlRendererTest {
    @Test
    fun `text adds exactly one trailing newline`() {
        assertThat(TextRenderer().render("a\nb")).isEqualTo("a\nb\n")
        assertThat(TextRenderer().render("a\n")).isEqualTo("a\n")
        assertThat(TextRenderer().render("a\r\nb")).isEqualTo("a\r\nb\n")
    }

    @Test
    fun `html fragment uses pygments classes and escapes`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("<A>", StyleClass.NAME),
            Span("&", StyleClass.OPERATOR),
        )
        assertThat(HtmlRenderer().renderFragment(spans)).isEqualTo(
            "<pre class=\"highlight\"><code><span class=\"kd\">public</span> &lt;A&gt;<span class=\"o\">&amp;</span></code></pre>\n"
        )
    }

    @Test
    fun `html keeps unicode intact`() {
        val spans = listOf(Span("Grüße", StyleClass.STRING), Span(" = "), Span("λ", StyleClass.NAME))
        val html = HtmlRenderer().renderFragment(spans)
        assertThat(html).contains("Grüße")
        assertThat(html).contains("λ")
    }

    @Test
    fun `html full document inlines user css`() {
        val html = HtmlRenderer().renderDocument("<pre/>", "code { color: red }")
        assertThat(html).startsWith("<!DOCTYPE html>")
        assertThat(html).contains("<style>\ncode { color: red }\n</style>")
        assertThat(html).contains("<pre/>")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.TextHtmlRendererTest"`
Expected: FAIL to compile (`TextRenderer`, `HtmlRenderer` unresolved).

- [ ] **Step 3: Implement both renderers**

- `TextRenderer.render`: if the text ends with `\n`, return it unchanged; otherwise append `\n`. Do not normalize CRLF.
- `HtmlRenderer.renderFragment`: `<pre class="highlight"><code>` + for each span, escape `&`/`<`/`>` and wrap non-`TEXT` spans in `<span class="<cssClass>">` + `</code></pre>\n`.
- `HtmlRenderer.renderDocument`: emit `<!DOCTYPE html>`, `<html>`, `<head>`, `<meta charset="utf-8">`, an optional `<style>` containing the CSS verbatim, `</head>`, `<body>`, the fragment, `</body>`, `</html>`, each on its own line.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.TextHtmlRendererTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/TextRenderer.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/HtmlRenderer.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/TextHtmlRendererTest.kt
git commit -m "feat(prettyprinting): add text and HTML renderers"
```

---

### Task 7: ANSI renderer and color detection

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/AnsiRenderer.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/AnsiRendererTest.kt`

**Interfaces:**
- Consumes: `Span`, `StyleClass`, `pygmentsDefault`, `xterm256`.
- Produces:
  - `object ColorSupport { fun enabled(noColorFlag: Boolean, env: (String) -> String? = System::getenv): Boolean }`.
  - `class AnsiRenderer(private val color: Boolean = true) { fun render(spans: List<Span>): String }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnsiRendererTest {
    @Test
    fun `emits bold 256-color escapes`() {
        val out = AnsiRenderer().render(listOf(Span("public", StyleClass.KEYWORD_DECLARATION)))
        assertThat(out).isEqualTo("\u001B[1;38;5;2mpublic\u001B[0m")
    }

    @Test
    fun `emits non-bold and italic escapes`() {
        val type = AnsiRenderer().render(listOf(Span("int", StyleClass.KEYWORD_TYPE)))
        assertThat(type).isEqualTo("\u001B[38;5;125mint\u001B[0m")

        val comment = AnsiRenderer().render(listOf(Span("// x", StyleClass.COMMENT_SINGLE)))
        assertThat(comment).isEqualTo("\u001B[3;38;5;66m// x\u001B[0m")
    }

    @Test
    fun `leaves whitespace and text unescaped`() {
        assertThat(AnsiRenderer().render(listOf(Span(" "), Span("plain")))).isEqualTo(" plain")
    }

    @Test
    fun `disabling color returns plain text`() {
        assertThat(AnsiRenderer(color = false).render(listOf(Span("public", StyleClass.KEYWORD))))
            .isEqualTo("public")
    }

    @Test
    fun `honours no color flag and environment`() {
        assertThat(ColorSupport.enabled(noColorFlag = false) { null }).isTrue()
        assertThat(ColorSupport.enabled(noColorFlag = false) { "1" }).isFalse()
        assertThat(ColorSupport.enabled(noColorFlag = true) { null }).isFalse()
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.AnsiRendererTest"`
Expected: FAIL to compile (`AnsiRenderer`, `ColorSupport` unresolved).

- [ ] **Step 3: Implement the renderer**

- `ColorSupport.enabled` = `!noColorFlag && env("NO_COLOR").isNullOrEmpty()`.
- `AnsiRenderer.render`: for each span, if `!color` or the style is `TEXT` or the text is blank, append the text unchanged. Otherwise build SGR codes: `1` when bold, `3` when italic, then `38;5;<xterm256(rgb)>` when `pygmentsDefault[style]?.rgb != null`; wrap as `\u001B[<codes>m<text>\u001B[0m`.
- `PUNCTUATION` has no palette entry, so it stays plain.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.AnsiRendererTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/AnsiRenderer.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/AnsiRendererTest.kt
git commit -m "feat(prettyprinting): add 256-color ANSI renderer"
```

---

### Task 8: LaTeX renderer

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/LatexRenderer.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/LatexRendererTest.kt`

**Interfaces:**
- Consumes: `Span`, `StyleClass`, `pygmentsDefault`, `StyleClass.latexMacro`.
- Produces: `class LatexRenderer(private val raw: Boolean = false) { fun render(spans: List<Span>): String; fun preamble(): String }`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LatexRendererTest {
    @Test
    fun `minted mode emits PYG macros and groups equal styles`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("class", StyleClass.KEYWORD_DECLARATION),
        )
        assertThat(LatexRenderer(raw = false).render(spans))
            .isEqualTo("\\PYG{kd}{public} \\PYG{kd}{class}")
    }

    @Test
    fun `minted mode escapes latex specials`() {
        val spans = listOf(
            Span("{", StyleClass.PUNCTUATION),
            Span("a_b", StyleClass.NAME),
            Span("\\", StyleClass.STRING),
            Span("<", StyleClass.OPERATOR),
        )
        assertThat(LatexRenderer(raw = false).render(spans))
            .isEqualTo("\\PYG{p}{\\PYGZob{}}\\PYG{n}{a\\PYGZus{}b}\\PYG{s}{\\PYGZbs{}}\\PYG{o}{\\PYGZlt{}}")
    }

    @Test
    fun `raw mode emits semantic macros`() {
        val spans = listOf(
            Span("public", StyleClass.KEYWORD_DECLARATION),
            Span(" "),
            Span("// x", StyleClass.COMMENT_SINGLE),
        )
        assertThat(LatexRenderer(raw = true).render(spans))
            .isEqualTo("\\jmlkd{public} \\jmlcOne{// x}")
    }

    @Test
    fun `raw mode escapes latex specials`() {
        val spans = listOf(Span("{a_b}", StyleClass.NAME))
        assertThat(LatexRenderer(raw = true).render(spans))
            .isEqualTo("\\jmln{\\{a\\_b\\}}")
    }

    @Test
    fun `raw preamble defines colors and macros`() {
        val preamble = LatexRenderer(raw = true).preamble()
        assertThat(preamble).contains("\\definecolor{jmltkKd}{HTML}{008000}")
        assertThat(preamble).contains("\\newcommand{\\jmlkd}[1]{\\textcolor{jmltkKd}{\\textbf{#1}}}")
        assertThat(preamble).contains("\\definecolor{jmltkKt}{HTML}{B00040}")
        assertThat(preamble).contains("\\newcommand{\\jmlcOne}[1]{\\textcolor{jmltkCOne}{\\textit{#1}}}")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.LatexRendererTest"`
Expected: FAIL to compile (`LatexRenderer` unresolved).

- [ ] **Step 3: Implement the renderer**

- Group adjacent spans with the same style before emitting.
- `TEXT` spans that are whitespace-only are appended verbatim. Any other `TEXT` span is escaped with the active escaping map and emitted without a macro (no style).
- Minted mode: `\PYG{<cssClass>}{<pygmentsEscape(text)>}`.
- Pygments escaping map: `\ -> \PYGZbs{}`, `{ -> \PYGZob{}`, `} -> \PYGZcb{}`, `^ -> \PYGZca{}`, `_ -> \PYGZus{}`, `& -> \PYGZam{}`, `< -> \PYGZlt{}`, `> -> \PYGZgt{}`, `# -> \PYGZsh{}`, `% -> \PYGZpc{}`, `$ -> \PYGZdl{}`, `- -> \PYGZhy{}`, `' -> \PYGZsq{}`, `" -> \PYGZdq{}`, `~ -> \PYGZti{}`.
- Raw mode: `\jml<latexMacro>{<latexEscape(text)>}`.
- LaTeX escaping map: `\ -> \textbackslash{}`, `{ -> \{`, `} -> \}`, `# -> \#`, `% -> \%`, `& -> \&`, `_ -> \_`, `$ -> \$`, `^ -> \textasciicircum{}`, `~ -> \textasciitilde{}`, `< -> \textless{}`, `> -> \textgreater{}`, `- -> {-}`, `' -> \textquotesingle{}`, `" -> \textquotedbl{}`.
- `preamble()`: start with `\usepackage{xcolor}`, then for every non-`TEXT` enum value in order: if it has a palette entry, `\definecolor{jmltk<Macro>}{HTML}{RRGGBB}`; then `\newcommand{\jml<latexMacro>}[1]{...}` where `...` is `\textcolor{jmltk<Macro>}{<textbf/textit>#1}` or `\textcolor{...}{#1}`, or plain `#1` when there is no color. Color name = `jmltk` + `latexMacro.replaceFirstChar { it.uppercase() }`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.LatexRendererTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/LatexRenderer.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/LatexRendererTest.kt
git commit -m "feat(prettyprinting): add minted and semantic LaTeX renderers"
```

---

### Task 9: Facade and render options

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/RenderOptions.kt`
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/PrettyPrintingFacade.kt`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/PrettyPrintingFacadeTest.kt`

**Interfaces:**
- Consumes: every previous task.
- Produces:
  ```kotlin
  enum class OutputFormat { TEXT, ANSI, HTML, LATEX }

  data class RenderOptions(
      val prettyPrint: Boolean = false,
      val format: OutputFormat = OutputFormat.TEXT,
      val language: Language = Language.JAVA,
      val snippet: SnippetKind = SnippetKind.AUTO,
      val width: Int = 100,
      val style: String? = null,
      val latexRaw: Boolean = false,
      val htmlFull: Boolean = false,
      val css: String? = null,
      val color: Boolean = true,
  )

  class PrettyPrintingException(message: String) : RuntimeException(message)

  class PrettyPrintingFacade(
      private val parser: SnippetParser = SnippetParser(),
      private val highlighter: Highlighter = Highlighter(),
      private val formatter: JmlFmt = JmlFmt(),
  ) {
      fun render(input: String, options: RenderOptions = RenderOptions()): String
  }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PrettyPrintingFacadeTest {
    private val facade = PrettyPrintingFacade()

    @Test
    fun `pretty prints a compilation unit`() {
        val out = facade.render(
            "public class A { int f(){return 1;} }",
            RenderOptions(prettyPrint = true),
        )
        val expected = """
            public class A {
              int f() {
                return 1;
              }
            }
        """.trimIndent() + "\n"
        assertThat(out).isEqualTo(expected)
    }

    @Test
    fun `renders html with pygments classes`() {
        val out = facade.render("class A {}", RenderOptions(format = OutputFormat.HTML))
        assertThat(out).contains("<span class=\"kd\">class</span>")
        assertThat(out).contains("<span class=\"nc\">A</span>")
    }

    @Test
    fun `renders ansi`() {
        val out = facade.render("public class A {}", RenderOptions(format = OutputFormat.ANSI))
        assertThat(out).contains("\u001B[1;38;5;2mpublic\u001B[0m")
    }

    @Test
    fun `renders raw latex`() {
        val out = facade.render(
            "public class A {}",
            RenderOptions(format = OutputFormat.LATEX, latexRaw = true),
        )
        assertThat(out).contains("\\jmlkd{public}")
        assertThat(out).contains("\\jmlkd{class}")
    }

    @Test
    fun `highlights jml snippets`() {
        val out = facade.render(
            "requires x > 0;",
            RenderOptions(language = Language.JML, format = OutputFormat.HTML),
        )
        assertThat(out).contains("<span class=\"kd\">requires</span>")
        assertThat(out).contains("<span class=\"o\">&gt;</span>")
        assertThat(out).contains("<span class=\"mi\">0</span>")
    }

    @Test
    fun `falls back to plain text for invalid input`() {
        val out = facade.render("class {", RenderOptions(format = OutputFormat.HTML))
        assertThat(out).contains("class {")
    }

    @Test
    fun `pretty printing invalid input throws`() {
        assertThatThrownBy { facade.render("class {", RenderOptions(prettyPrint = true)) }
            .isInstanceOf(PrettyPrintingException::class.java)
    }

    @Test
    fun `style emits usemintedstyle in minted mode`() {
        val out = facade.render(
            "class A {}",
            RenderOptions(format = OutputFormat.LATEX, style = "monokai"),
        )
        assertThat(out).startsWith("\\usemintedstyle{monokai}\n")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.PrettyPrintingFacadeTest"`
Expected: FAIL to compile (`PrettyPrintingFacade`, `RenderOptions`, `OutputFormat` unresolved).

- [ ] **Step 3: Implement the facade**

- Parse with `parser.parse(input, options.language, options.snippet)`.
- If `prettyPrint`:
  - `ParsedKind.PLAIN` -> throw `PrettyPrintingException("Cannot pretty-print: the input does not parse")`.
  - `ParsedKind.COMPILATION_UNIT` -> `formatter.pp(input, options.width)`.
  - otherwise -> `parsed.nodes.joinToString("\n") { it.toString().trimEnd() }`.
  - Wrap the formatting call in `try/catch (Exception)` and rethrow as `PrettyPrintingException("Cannot pretty-print: ${e.message}")`.
  - Re-parse the formatted text with `SnippetKind.AUTO` for highlighting.
- If not `prettyPrint`, use the original parse for highlighting.
- `TEXT` -> `TextRenderer().render(sourceUnderRender)` where the source is the formatted text when pretty-printing, otherwise the input. Do not tokenize for text.
- `ANSI` -> `AnsiRenderer(options.color).render(highlighter.highlight(parsedForHighlight))`.
- `HTML` -> `HtmlRenderer().renderFragment(...)`; if `htmlFull`, pass it and `options.css` to `renderDocument`.
- `LATEX` -> if `latexRaw`, `LatexRenderer(raw = true).render(...)`; otherwise `LatexRenderer(raw = false).render(...)` prefixed with `\usemintedstyle{<style>}\n` when `options.style != null`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.PrettyPrintingFacadeTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/RenderOptions.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/PrettyPrintingFacade.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/PrettyPrintingFacadeTest.kt
git commit -m "feat(prettyprinting): add render facade and options"
```

---

### Task 10: Standalone `jmltk-pp` CLI

**Files:**
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/InputReader.kt`
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/PpCommand.kt`
- Create: `tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Main.kt`
- Modify: `tools/prettyprinting/build.gradle.kts`
- Test: `tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/InputReaderTest.kt`

**Interfaces:**
- Consumes: `PrettyPrintingFacade`, `RenderOptions`, `OutputFormat`, `Language`, `SnippetKind`, `LatexRenderer`, `ColorSupport`.
- Produces:
  - `class InputReader(private val stdin: () -> String = { System.`in`.reader(Charsets.UTF_8).readText() }, private val terminal: () -> Boolean = { System.console() != null }) { fun read(files: List<File>): List<String> }`; throws `InputException`.
  - `class InputException(message: String) : RuntimeException(message)`.
  - `open class PpCommand(name: String = "pp") : CliktCommand(name)`.
  - `fun main(args: Array<String>)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package io.github.jmltoolkit.prettyprinting

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class InputReaderTest {
    @Test
    fun `reads stdin when no files are given`() {
        val reader = InputReader(stdin = { "from stdin" }, terminal = { false })
        assertThat(reader.read(emptyList())).containsExactly("from stdin")
    }

    @Test
    fun `rejects empty input from a terminal`() {
        val reader = InputReader(stdin = { "unused" }, terminal = { true })
        assertThatThrownBy { reader.read(emptyList()) }.isInstanceOf(InputException::class.java)
    }

    @Test
    fun `reads files in order and dash once`(@TempDir dir: Path) {
        val a = dir.resolve("A.java").toFile().apply { writeText("class A {}") }
        val b = dir.resolve("B.java").toFile().apply { writeText("class B {}") }
        val reader = InputReader(stdin = { "class S {}" }, terminal = { false })
        assertThat(reader.read(listOf(a, b))).containsExactly("class A {}", "class B {}")
        assertThat(reader.read(listOf(a, java.io.File("-"), b)))
            .containsExactly("class A {}", "class S {}", "class B {}")
    }

    @Test
    fun `rejects repeated stdin`() {
        val reader = InputReader(stdin = { "x" }, terminal = { false })
        assertThatThrownBy { reader.read(listOf(java.io.File("-"), java.io.File("-"))) }
            .isInstanceOf(InputException::class.java)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.InputReaderTest"`
Expected: FAIL to compile (`InputReader` unresolved).

- [ ] **Step 3: Implement `InputReader`**

- Empty file list: if `terminal()` throw `InputException("No input files and stdin is a terminal")`, else return `listOf(stdin())`.
- Otherwise map each file: `-` -> `stdin()` (throw on a second `-`), else `readText(Charsets.UTF_8)`; wrap `IOException` in `InputException`.
- Read stdin at most once per call.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :tools:prettyprinting:test --tests "io.github.jmltoolkit.prettyprinting.InputReaderTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Implement `PpCommand` and `Main`**

Options (Clikt delegates):

```kotlin
open class PpCommand(name: String = "pp") : CliktCommand(name = name) {
    private val prettyPrint by option("-p", "--pretty-print").flag()
    private val format by option("-f", "--format").enum<OutputFormat>().default(OutputFormat.TEXT)
    private val language by option("--language").enum<Language>().default(Language.JAVA)
    private val snippet by option("--snippet").enum<SnippetKind>().default(SnippetKind.AUTO)
    private val width by option("-w", "--width").int().default(100)
    private val style by option("--style")
    private val minted by option("--minted").flag()
    private val latexRaw by option("--latex-raw").flag()
    private val latexPreamble by option("--latex-preamble").flag()
    private val htmlFull by option("--html-full").flag()
    private val css by option("--css").file(mustExist = true, canBeDir = false)
    private val noColor by option("--no-color").flag()
    private val files by argument("FILES").file().multiple()

    override fun help(context: Context): String = "..."
    override fun run() { ... }
}
```

`run()`:

- `versionOption(JmlTkBuild.PROJECT_VERSION)` in `init`.
- Reject `minted && latexRaw` with `UsageError`; reject `latexPreamble && (format != LATEX || !latexRaw)`.
- If `latexPreamble`: print `LatexRenderer(raw = true).preamble()` and return.
- Read inputs with `InputReader(files = ...)`; catch `InputException` -> `CliktError(message, statusCode = 1)`.
- Build `RenderOptions(prettyPrint, format, language, snippet, width, style, latexRaw, htmlFull, css?.readText(), ColorSupport.enabled(noColor))`; wrap CSS read failures in `CliktError(..., statusCode = 1)`.
- Catch `PrettyPrintingException` -> `CliktError(message, statusCode = 3)`.
- Render every input and join with `"\n"`; `print(...)`, then `println()` if the result does not end with `\n`.

`Main.kt`:

```kotlin
package io.github.jmltoolkit.prettyprinting

import com.github.ajalt.clikt.core.main

fun main(args: Array<String>) = PpCommand().main(args)
```

- [ ] **Step 6: Wire the application plugin**

`tools/prettyprinting/build.gradle.kts`:

```kotlin
plugins {
    id("standard-kotlin")
    application
}

application {
    mainClass = "io.github.jmltoolkit.prettyprinting.MainKt"
    applicationName = "jmltk-pp"
    applicationDefaultJvmArgs = googleJavaFormatExports
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
```

Add `implementation(libs.clickt)` to the dependencies.

- [ ] **Step 7: Verify the CLI manually**

```bash
./gradlew :tools:prettyprinting:run --args="--help"
printf 'public class A { int f(){return 1;} }' | ./gradlew -q :tools:prettyprinting:run --args="--pretty-print"
printf 'requires x > 0;' | ./gradlew -q :tools:prettyprinting:run --args="--language jml -f html"
printf 'class A {}' | ./gradlew -q :tools:prettyprinting:run --args="-f latex --latex-raw"
printf 'class A {}' | ./gradlew -q :tools:prettyprinting:run --args="-f latex --latex-preamble"
```

Expected: help text lists all options; the first render is formatted Java; the JML HTML contains `class="kd"`; the raw LaTeX contains `\jmlkd{class}`; the preamble contains `\definecolor`.

- [ ] **Step 8: Commit**

```bash
git add tools/prettyprinting/build.gradle.kts tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/InputReader.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/PpCommand.kt tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/Main.kt tools/prettyprinting/src/test/kotlin/io/github/jmltoolkit/prettyprinting/InputReaderTest.kt
git commit -m "feat(prettyprinting): add jmltk-pp CLI"
```

---

### Task 11: Aggregate `jmltk pp` integration

**Files:**
- Modify: `tools/cli/src/main/kotlin/io/github/jmltoolkit/cli/Main.kt`
- Modify: `tools/cli/src/main/kotlin/io/github/jmltoolkit/cli/PrettyPrintCommand.kt`
- Modify: `tools/cli/build.gradle.kts`

**Interfaces:**
- Consumes: `PpCommand`.
- Produces: `jmltk pp` and `jmltk format` both run the new pipeline.

- [ ] **Step 1: Replace the empty stub**

`PrettyPrintCommand.kt` becomes:

```kotlin
package io.github.jmltoolkit.cli

import io.github.jmltoolkit.prettyprinting.PpCommand

class PrettyPrintCommand : PpCommand(name = "format")
```

Remove the now-unused imports.

- [ ] **Step 2: Register the `pp` command**

In `Main.kt`, import `io.github.jmltoolkit.prettyprinting.PpCommand` and add it to the subcommands:

```kotlin
val cmd = Main().subcommands(
    J2JCommand(), LintCommand(), PrettyPrintCommand(), PpCommand(), XPathCommand(),
    StatCommand(), WdCommand(), JmlStubCommand(), VcgCommand()
)
```

- [ ] **Step 3: Add the google-java-format JVM exports to the CLI launchers**

In `tools/cli/build.gradle.kts`, add the same `googleJavaFormatExports` list as Task 1 and append it to:

- `application { applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED") + googleJavaFormatExports }`
- `startScripts { defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED") + googleJavaFormatExports }`
- `createLauncher { defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED") + googleJavaFormatExports }`

- [ ] **Step 4: Verify both entry points**

```bash
./gradlew :tools:cli:compileKotlin
./gradlew :tools:cli:installDist
build/install/jmltk/bin/jmltk pp --help
printf 'public class A { int f(){return 1;} }' > /tmp/PpSmoke.java
build/install/jmltk/bin/jmltk pp -p /tmp/PpSmoke.java
build/install/jmltk/bin/jmltk format -f html /tmp/PpSmoke.java
```

Expected: `pp --help` works, `-p` formats, and `format -f html` prints `<span class="kd">public</span>`.

- [ ] **Step 5: Commit**

```bash
git add tools/cli/build.gradle.kts tools/cli/src/main/kotlin/io/github/jmltoolkit/cli/Main.kt tools/cli/src/main/kotlin/io/github/jmltoolkit/cli/PrettyPrintCommand.kt
git commit -m "feat(cli): expose jmltk pp and format commands"
```

---

### Task 12: Documentation, changelog, and final verification

**Files:**
- Create: `tools/prettyprinting/README.md`
- Modify: `jmltk-changelog.md`
- Modify: `/home/weigl/work/jmltoolkit.github.io/content/tools/prettyprinting/_index.md`

**Interfaces:**
- Consumes: all previous tasks.
- Produces: user-facing documentation and the final green build.

- [ ] **Step 1: Write the module README**

Include:

- a one-paragraph description and the `jmltk-pp`/`jmltk pp` invocation;
- a table of the options from the spec;
- one example per output mode:
  ```bash
  jmltk-pp -p Example.java
  jmltk-pp -f ansi Example.java
  jmltk-pp -f html --html-full --css pygments.css Example.java
  jmltk-pp -f latex Example.java
  jmltk-pp -f latex --latex-raw --latex-preamble Example.java
  printf 'requires x > 0;' | jmltk-pp --language jml -f html
  ```
- a note that `--minted` outputs `\PYG` body content for use where minted's macros exist, and that `--latex-raw` macros are defined by `--latex-preamble`;
- a note that JML comments are preserved, not reflowed.

- [ ] **Step 2: Add the changelog entry**

Under `## Unreleased — 1.2.0-SNAPSHOT` -> `**Added**`, add one bullet:

```markdown
- `tools/prettyprinting`: new `jmltk-pp` CLI (also `jmltk pp`, `jmltk format`) that pretty-prints Java/JML from files or stdin and highlights it for terminals (256-color ANSI), HTML (Pygments class names), and LaTeX (minted `\PYG` body or semantic `\jml` macros); snippets are auto-detected and JML comments are highlighted.
```

Then append the 9-character hash of the commit produced by Task 11 in parentheses, marking it as a feature-branch hash (`branch weigl/prettyprint-cli`) because this work is not merged yet.

- [ ] **Step 3: Format and run the gates**

```bash
./gradlew :tools:prettyprinting:spotlessApply :tools:cli:spotlessApply
./gradlew :tools:prettyprinting:test :tools:cli:compileKotlin
./gradlew spotlessCheck checkstyleMain
```

Expected: all green.

- [ ] **Step 4: Install and smoke-test the distribution**

```bash
./gradlew :tools:prettyprinting:installDist :tools:cli:installDist
tools/prettyprinting/build/install/jmltk-pp/bin/jmltk-pp -p /tmp/PpSmoke.java
printf 'class A {}\n' | tools/prettyprinting/build/install/jmltk-pp/bin/jmltk-pp -f ansi
build/install/jmltk/bin/jmltk pp -f latex /tmp/PpSmoke.java
```

Expected: no `IllegalAccessError` from google-java-format, formatted/ANSI/LaTeX output as documented.

- [ ] **Step 5: Update the docs site**

In `/home/weigl/work/jmltoolkit.github.io/content/tools/prettyprinting/_index.md`, replace the "wiring is still in progress" warning callout with the working `jmltk-pp`/`jmltk pp` usage and one example per output format. Update the Kotlin snippet to import `io.github.jmltoolkit.prettyprinting.JmlFmt` and note that `pp` returns `String`. Commit that repository separately.

- [ ] **Step 6: Commit**

```bash
git add tools/prettyprinting/README.md jmltk-changelog.md
git commit -m "docs(prettyprinting): document jmltk-pp"
```

---

## Self-Review Notes

- Spec coverage: command surface (Task 10/11), snippet detection (Task 4), pretty printing (Task 1/9), Java/JML highlighting (Tasks 3/5), text/ANSI/HTML/LaTeX (Tasks 6/7/8), build exports (Tasks 1/10/11), tests/docs/changelog (all tasks + Task 12).
- The finalized command surface has no `--inline` or `--minted-options`; no task implements them, and the README documents the final surface.
- Type names used consistently: `ParsedKind`, `ParsedSource.tokenRanges`, `RenderOptions`, `StyleClass.latexMacro`, `ColorSupport.enabled`.
- Review Focus items are pinned in HighlighterTest (JML comments), TextHtmlRendererTest (Unicode/HTML), LatexRendererTest (special characters), PrettyPrintingFacadeTest (invalid input), and InputReaderTest (stdin/files).
