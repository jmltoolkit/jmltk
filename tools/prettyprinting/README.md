# Pretty printing and highlighting

The `:tools:prettyprinting` module contains the JML-aware google-java-format wrapper (`JmlFmt`), the `jmltk-pp` CLI, and the aggregate CLI's `jmltk pp` / `jmltk format` subcommands. The CLI reads Java or JML from files or stdin, optionally pretty-prints it, and optionally adds syntax highlighting for terminals, HTML, or LaTeX.

## Invocation

```bash
# standalone launcher
jmltk-pp [OPTIONS] [FILES...]

# the same command through the aggregate CLI
jmltk pp [OPTIONS] [FILES...]
jmltk format [OPTIONS] [FILES...]   # alias

# read stdin (no arguments, or `-`)
printf 'public class A { int f(){return 1;} }' | jmltk-pp --pretty-print
```

Without `--format`, the output is plain text. `--pretty-print` is opt-in.

## Options

| Option | Meaning |
| --- | --- |
| `-p`, `--pretty-print` | Format before rendering |
| `-f`, `--format text\|ansi\|html\|latex` | Output format (default: `text`) |
| `--language java\|jml` | Input language (default: `java`) |
| `--snippet auto\|cu\|member\|stmt\|expr` | How to interpret Java input (default: `auto`) |
| `-w`, `--width N` | Pretty-print width (default: `100`) |
| `--style NAME` | Pygments/minted style name, emitted as `\usemintedstyle` in LaTeX minted mode |
| `--minted` | LaTeX: emit `\PYG` body content (default) |
| `--latex-raw` | LaTeX: emit semantic `\jml<cls>{...}` macros |
| `--latex-preamble` | LaTeX raw: print the `\definecolor`/`\newcommand` definitions |
| `--html-full` | HTML: emit a complete document |
| `--css FILE` | HTML: inline this stylesheet in the full document |
| `--no-color` | Force plain text (also honors `NO_COLOR`) |
| `--version` | Print the project version |

Multiple inputs are rendered independently and concatenated; in HTML each gets its own `<pre class="highlight">` block.

## Examples

```bash
# format a file
jmltk-pp -p Example.java

# 256-color terminal output
jmltk-pp -f ansi Example.java

# Pygments class names, complete document with your own stylesheet
jmltk-pp -f html --html-full --css pygments.css Example.java

# LaTeX: \PYG body content for an existing minted context
jmltk-pp -f latex Example.java

# LaTeX: semantic \jml macros plus the matching preamble
jmltk-pp -f latex --latex-raw Example.java
jmltk-pp -f latex --latex-raw --latex-preamble > preamble.tex

# JML input
printf 'requires x > 0;\nensures \\result > 0;' | jmltk-pp --language jml -f html
```

HTML output uses Pygments class names only (`kd`, `kt`, `nc`, `nf`, `mi`, `cm`, `c1`, ...); no CSS is bundled. In minted mode the tool emits `\PYG{class}{text}` content intended for a context where minted's `\PYG` macros are available. In raw mode it emits `\jml<class>{text}` macros such as `\jmlkd{public}`; `--latex-preamble` prints only the `xcolor`/`fontenc` definitions, including the digit-class renames (`c1` -> `cOne`), and ignores its input.

Compilation-unit formatting preserves JML comments, including `//@` line comments, and highlights them inside Java files. Snippet formatting uses JavaParser's pretty printer and may normalize or drop comments around the snippet; use the original text (without `-p`) when comment layout matters. JML clause and expression inputs are lexed by the module's JML lexer.
