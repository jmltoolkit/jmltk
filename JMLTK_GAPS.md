# JMLtk — Closing the Syntax Gap to KeY

Status report of the remaining JML **syntax** constructs that KeY accepts but JMLtk
(JML's reference implementation being KeY's parser) previously rejected or handled
differently. Every claim below was verified **empirically**, by parsing actual KeY test
files with a freshly built `jmlparser-core` jar (JML processing enabled,
`LanguageLevel.JAVA_17` where record declarations are involved).

See `jmltk-changelog.md` for release notes, and `CONTRIBUTING.md` for the node/field
guide referenced by the fix below.

## Method

- Build: `./gradlew :jmlparser-core:jar`, then parse with `ParserConfiguration` +
  `setProcessJml(true)` via `jshell` against the built jar.
- Reference sources of truth are KeY's own test files (see per-construct rows) and
  KeY's grammar `JmlParser.g4` (`key.core/src/main/antlr4`).
- Only parser-level support is verified here; proof obligations, typing, and
  SMT/KeY-side behaviour are out of scope.

## Construct status

| # | Construct | KeY reference | Status |
|---|-----------|---------------|--------|
| 1 | `merge_point` comment form (`//@ merge_point { … }`) | `testcase/merge/Gcd.java` | skipped (see below) |
| 2 | `\by` proof scripts (`assert e \by { … }`) | `de/uka/ilkd/key/scripts/jml/NestedAssert.java` | on branch `weigl/proofscripts` |
| 3 | record-level `nullable` (`/*@ nullable */ record R(…)`) | `pipelineTests/records/input/RecordOverwriting.java` | **CLOSED** (parses fine) |
| 4 | `spec_java_math` / `spec_bigint_math` (`/*@ spec_java_math */`) | `testcase/specMath/java/Test.java` | **CLOSED** (parses fine) |
| 5 | label-after-keyword (`assert LAB: e;`, `set SN: x = …;`) | `JmlParser.g4` `assert_statement` | **FIXED** (see below) |
| 6 | `nowarn` / `debug` directives (`/*@ nowarn … */`) | — | skipped (see below) |
| 7 | `\strictly_nothing` (`assignable \strictly_nothing;`) | KeY `JML_JAVA` Math API | **CLOSED** (parses fine) |
| 8 | `when` / `captures` / `name` / `duration` / `working_space` clauses | `JmlParser.g4` clause lists | skipped (see below) |

## Verified as already closed (no work needed)

- **#3 record-level `nullable`** — `/*@ nullable */ record Point(int x, int y) {}` parses
  with `LanguageLevel.JAVA_17`; the modifier attaches to the record declaration. The
  earlier "Record Declarations are not supported" error was a language-level
  configuration artefact, not a JML gap.
- **#4 specification math modes** — `/*@ spec_java_math */`, `/*@ spec_bigint_math */`
  and `/*@ spec_bigint_math +java */` all parse in the three placement forms (class,
  method, and `//@` line form); the modifier attaches correctly.
- **#7 `\strictly_nothing`** — `//@ assignable \strictly_nothing;` parses; the
  `\strictly_nothing` token is recognized as a `JML_IDENTIFIER`, so it round-trips
  through the printer as-is. No grammar change needed.

## Fixed: #5 label-after-keyword form (option B)

KeY allows a label *after* the statement keyword — `assert LAB: x > 0;` — in addition
to the JML-standard prefix form `LAB: assert x > 0;` (which JMLtk already supports via
`JmlLabelStmt`). This is what KeY's grammar produces for formatted/`\by` statements
(`JmlParser.g4`, `assert_statement`).

### Change (option B — optional `label` on `JmlExpressionStmt`)

- `JmlExpressionStmt` gains an optional `SimpleName label`
  (`@OptionalProperty`), plus a parser constructor taking the label.
- Grammar (`java.jj`, `JmlStatement`): `[ LOOKAHEAD(<IDENTIFIER> ":") label=SimpleName() ":" ]`
  after the keyword, passed to the new constructor.
- Printing: `DefaultPrettyPrinterVisitor.visit(JmlExpressionStmt)` emits
  `assert LAB: x > 0;` (keyword, label, `:`, expression) and
  `ConcreteSyntaxModel` gained a `conditional(LABEL, IS_PRESENT, …)` element, so the
  lexical-preserving printer round-trips labels too.
- Generated code regenerated via `scripts/run-generators.sh` order (metamodel run and
  code-generator run **must stay in separate Gradle invocations** — running both with
  `--parallel` in one invocation silently skips the generated accessors).

### Verification

- `//@ assert LAB: x > 0;` → `JmlExpressionStmt` with `label=LAB`, `kind=ASSERT`,
  prints back `/*@ assert LAB: x > 0;*/`.
- `//@ assert LAB: x > 0 \by { assume x > 0; auto; };` parses with label + 2-statement
  script and round-trips.
- Prefix form `//@ LAB: assert x > 0;` still parses to `JmlLabelStmt` + assert.
- Regression tests: `jmlparser-jml-tests/.../JmlLabeledAssertTest.java`
  (KeY form, label + proof script, unlabelled, labelled `assume`/`set`, prefix form,
  round-trip printing).
- Known remaining difference (construct #2 territory, not this gap): JMLtk requires a
  trailing `;` after `\by { … }` whereas KeY makes it optional
  (`assertionProof SEMI_TOPLEVEL?`). Handled on `weigl/proofscripts`.

## Skipped (explicitly out of scope here)

- **#1 `merge_point` comment form** — JML's `merge_point` syntax is comment-oriented
  and interacts with JMLtk's comment handling; not part of the parser-gap closure.
- **#6 `nowarn` / `debug`** — KeY-specific directives without a JML-standard semantic;
  skipped.
- **#8 `when` / `captures` / `name` / `duration` / `working_space` clauses** —
  clause-level sugar; skipped for this pass.

## Cross-references

- **#2 `\by` proof scripts** — active work on branch `weigl/proofscripts`
  (top commit `35b95d424`, "update CSM"). Includes the optional-`;`-after-script
  difference noted above.
