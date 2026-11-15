# `jmltk-pp` — pretty-printing and syntax-highlighting CLI

Date: 2026-11-15
Status: approved design, implementation pending
Module: `:tools:prettyprinting`

## Goal

Add a CLI that reads Java or JML source from files or stdin, optionally pretty-prints it, and optionally renders it with syntax highlighting for terminals (ANSI), HTML, or LaTeX.

The tool must be usable both as a standalone launcher (`jmltk-pp`) and as a subcommand of the aggregate CLI (`jmltk pp`, with `jmltk format` kept as an alias).

## Non-goals

- Reflowing/rewriting the contents of JML comments. The formatter preserves JML comment layout.
- Bundling CSS themes. HTML output carries Pygments class names; users supply CSS.
- Rendering line numbers, except through future minted options.
- Highlighting syntactically invalid Java beyond a plain-text fallback.

## Command surface

```
jmltk-pp [OPTIONS] [FILES...]

  -p, --pretty-print              format before rendering
  -f, --format text|ansi|html|latex   (default: text)
      --language java|jml             (default: java)
      --snippet auto|cu|member|stmt|expr (default: auto; java only)
  -w, --width N                   pretty-print width (default 100)
      --style NAME                Pygments/minted style name (LaTeX only)
      --minted                    LaTeX: emit \PYG{...} body content (default)
      --latex-raw                 LaTeX: emit \jml<cls>{...} semantic macros (mutually exclusive with --minted)
      --latex-preamble            LaTeX raw: print macro/color definitions
      --html-full                 full HTML document
      --css FILE                  inline user CSS (with --html-full)
      --no-color                  force plain text (also honors NO_COLOR)
      --version
```

Inputs:

- `FILES...` are read in order. `-` means stdin.
- With no file arguments, stdin is read when it is not a TTY; otherwise the command prints usage and exits with an error.
- Multiple inputs are rendered independently and concatenated with a blank line. In HTML each input gets its own `<pre class="highlight"><code>` block; in LaTeX each gets its own block.

Defaults: plain text, no pretty-printing, Java language, auto snippet detection.

`jmltk pp` uses the same command class. `jmltk format` becomes an alias (the current `format` subcommand is an empty stub).

## Pipeline

```
read input -> detect/parse (CU | member | stmt | expr | JML)
           -> optional pretty-print
           -> highlighter -> List<Span(text, StyleClass)>
           -> renderer (text | ansi | html | latex)
```

### Snippet detection (`--language java`)

Try in order, stopping at the first successful parse:

1. `cu`: parse as a compilation unit.
2. `member`: wrap in `class __JmltkSnippet { <text> }`, take the first member.
3. `stmt`: wrap in `class __JmltkSnippet { void __m() { <text> } }`, take the first statement.
4. `expr`: wrap in `class __JmltkSnippet { Object __f = (<text>); }`, take the initializer.

Explicit `--snippet` skips the earlier alternatives. A parse result counts as successful only when it has a result and no problems. The wrapper is used only for parsing; output is always rendered from the unwrapped node.

### JML input (`--language jml`)

Parse with the fork's JML entry points, trying expression, then clause, then a list of clauses split on top-level semicolons:

- `JavaParser.parseJmlExpression`
- `JavaParser.parseJmlClause`
- one `parseJmlClause` call per top-level clause

If all fail, the input is rendered as plain text.

### Pretty-printing

- Full compilation units: `JmlFmt`, the existing google-java-format wrapper with a JML-preserving `CommentsHelper`.
- Snippets: the parsed node's `toString()` (the fork's `DefaultPrettyPrinter` / `ConcreteSyntaxModel`).
- JML snippets: the parsed JML node's `toString()`.
- Formatting happens before highlighting; the formatted text is re-parsed for tokenization.

The current `JmlFmt.pp` has a bug: it returns `output.getLine(0)`, i.e. only the first line. It must return `output.toString()`. `JmlFmt` also moves from the default package to `io.github.jmltoolkit.prettyprinting`.

## Highlighting

Tokenize Java with the JavaParser token stream from a parse configured with `setProcessJml(true)`, so JML comments arrive as `JML_BLOCK_COMMENT` / `JML_LINE_COMMENT` tokens. Whitespace and comments are preserved exactly. For snippets, use the unwrapped node's `TokenRange`, so wrapper tokens never reach a renderer.

Classify tokens using the Pygments Java lexer vocabulary:

| Token | Class |
|---|---|
| control keywords (`if`, `for`, `return`, `new`, `this`, `throw`, ... ) | `k` |
| declaration keywords, `class`/`interface`/`enum`/`record`/`module`/`var` | `kd` |
| primitives (`int`, `boolean`, ...) | `kt` |
| `true`/`false`/`null` | `kc` |
| `package`/`import` | `kn` |
| declared class/interface/record/enum name | `nc` |
| method name (identifier followed by `(`) | `nf` |
| identifier after `.` | `na` |
| annotation (`@Foo`) | `nd` |
| label (identifier before `:` at statement start) | `nl` |
| integer / float / hex / octal or binary literals | `mi` / `mf` / `mh` / `mo` |
| char / string / escape sequence | `sc` / `s` / `se` |
| punctuation (`{}();:.,`) | `p` |
| operators (including `[` `]`) | `o` |
| line / block / Javadoc comment | `c1` / `cm` / `cs` |

JML comment bodies (inside `/*@ ... @*/` or `//@ ...`) and `--language jml` input are lexed by a small JML lexer:

- clause keywords (`requires`, `ensures`, `assignable`, `modifies`, `invariant`, `decreases`, `diverges`, `when`, `signals`, `behavior`, `normal_behavior`, `exceptional_behavior`, `loop_invariant`, `assert`, `assume`, `set`, `also`, ...) -> `kd`
- backslash names and logical keywords (`\old`, `\result`, `\forall`, `\exists`, `\sum`, `\nothing`, `\everything`, ...) -> `k`
- numbers, strings, operators, punctuation, comments as above
- the delimiters `/*@`, `@*/`, `//@` -> `c`

If parsing fails, the highlighter falls back to a single unstyled span covering the input and writes a warning to stderr. Exit codes: `0` on success, `1` when an input cannot be read, `2` for usage errors, and `3` when `--pretty-print` was requested but the input cannot be parsed or formatted. A failed highlight without `--pretty-print` still exits `0` after the warning.

## Renderers

### text

Unchanged or formatted source followed by a newline.

### ansi

- Pygments `default` palette mapped to xterm-256 colors (`\u001B[38;5;Nm`), with bold (`1`) and italic (`3`) attributes and per-span resets.
- Whitespace is emitted without escape codes.
- `--no-color` and the `NO_COLOR` environment variable force plain text.

### html

- Fragment: `<pre class="highlight"><code>` with `<span class="<cls>">...</span>`, HTML-escaped (`&`, `<`, `>`).
- Whitespace is emitted literally (no `w` spans), matching the visual result of Pygments.
- `--html-full` wraps the fragment in a minimal HTML document. `--css FILE` inlines the file contents in a `<style>` element in that document. No CSS is bundled.

### latex

Two modes. `--minted` (default) emits Pygments-compatible `\PYG{<class>}{...}` body content with Pygments LaTeX escaping (`\PYGZob{}`, `\PYGZcb{}`, `\PYGZgt{}`, `\PYGZlt{}`, `\PYGZbs{}`, `\PYGZus{}`, `\PYGZsh{}`, `\PYGZpc{}`, `\PYGZam{}`, `\PYGZdl{}`, `\PYGZca{}`, `\PYGZti{}`, `\PYGZhy{}`, `\PYGZsq{}`, `\PYGZdq{}`). Adjacent spans with the same class are grouped; whitespace is literal. This content is meant to be embedded where minted's `\PYG` macros are available.

`--latex-raw` emits semantic macros `\jml<cls>{...}` (for example `\jmlkd{public}`, `\jmlnf{foo}`, `\jmlo{>}`). Because TeX control words cannot contain digits, digit-bearing classes are renamed: `c1 -> cOne`, `s1 -> sOne`, `s2 -> sTwo`. `--latex-preamble` prints the matching `xcolor` `\definecolor` and `\newcommand` definitions (Pygments `default` palette, bold/italic included), so the output compiles without minted.

Both modes use plain LaTeX escaping for text: `\{`, `\}`, `\#`, `\%`, `\&`, `\_`, `\$`, `\textbackslash{}`, `\textless{}`, `\textgreater{}`, `\textasciitilde{}`, `\textasciicircum{}`, and `{-}` for each hyphen (to avoid ligatures). `--style NAME` emits `\usemintedstyle{NAME}` only in minted mode.

## Code layout

```
tools/prettyprinting/src/main/kotlin/io/github/jmltoolkit/prettyprinting/
  JmlFmt.kt            (moved, pp() fixed)
  StyleClass.kt        (Pygments class enum, ANSI palette)
  SourceUnit.kt        (input text, parse kind, node)
  SnippetParser.kt     (auto-detection and JML parsing)
  Highlighter.kt       (Java token mapping)
  JmlLexer.kt          (JML body lexer)
  Span.kt
  render/TextRenderer.kt
  render/AnsiRenderer.kt
  render/HtmlRenderer.kt
  render/LatexRenderer.kt
  RenderOptions.kt     (format, language, snippet, width, flags)
  PrettyPrinterFacade.kt (read -> format -> highlight -> render)
  PpCommand.kt         (Clikt command, shared by both entry points)
  Main.kt              (standalone main)
```

`tools/cli/src/main/kotlin/io/github/jmltoolkit/cli/Main.kt` registers `PpCommand` as `pp`; `PrettyPrintCommand` becomes a `PpCommand("format")` subclass.

## Build and integration

`tools/prettyprinting/build.gradle.kts`:

- add the `application` plugin;
- `mainClass = "io.github.jmltoolkit.prettyprinting.MainKt"`, `applicationName = "jmltk-pp"`;
- add `implementation(libs.clickt)`;
- add google-java-format's required JVM exports to `applicationDefaultJvmArgs` and the `test` task:
  `--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED`,
  `...file=ALL-UNNAMED`, `...parser=ALL-UNNAMED`, `...tree=ALL-UNNAMED`, `...util=ALL-UNNAMED`.

`tools/cli/build.gradle.kts`:

- add the same `--add-exports` options to `applicationDefaultJvmArgs`, the main `startScripts` `defaultJvmOpts`, and the `jmltk-lsp` launcher, so `jmltk pp -p` works from an installed distribution.

## Tests

Unit tests in `tools/prettyprinting/src/test/kotlin` using JUnit 5 + AssertJ:

- snippet auto-detection for CU, member, statement, expression, and JML clause/expression;
- pretty-print of a malformed one-line class and of a snippet;
- Java class mapping (keywords, types, class/method names, numbers, strings, comments);
- JML sub-lexing of `/*@ requires x > 0; @*/`;
- HTML escaping and class names;
- ANSI contains 256-color escapes and `NO_COLOR`/`--no-color` produces plain text;
- LaTeX minted escaping and `\jml*` macro output/preamble;
- CLI smoke test through the facade (file and stdin).

## Documentation

- `tools/prettyprinting/README.md` with examples for each output format.
- Changelog entry under the current `Unreleased` -> `Added` section in `jmltk-changelog.md`, citing the implementation commit hash.
- Docs-site page under `content/` in `/home/weigl/work/jmltoolkit.github.io` once the implementation lands.
