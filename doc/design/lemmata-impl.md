# Implementation Plan — JML lemmas & `use_lemma` proof commands (port of KeY PR #3971)

Executable checklist for the design in `lemmata.md`: a class-body `lemma`
declaration (`//@ instance lemma Pred(int i) = i > 0`, the KeY `\by {…}` script
form, the `assert e \by {…}` form) and the proof-script command `use_lemma
<call>;` — parsed the way `obtain` is (contextual keyword, **no** new lexer
token), producing a `JmlProofCmdStmt`. The KeY method-level `use_lemma`
statement form is **deferred by decision** (see `lemmata.md` §3).

Rules that apply throughout (identical to `quantarray-impl.md`):

- every phase ends compile-green with the listed checks passing; commit per phase;
- never hand-edit generated files — run `./scripts/run-generators.sh` (its final
  `git diff --exit-code` **fails while your changes are uncommitted**; that is the
  drift check — commit the regenerated output and re-run to see it pass);
- use `./gradlew` (wrapper), commands from the repo root;
- grammar edits: `./gradlew :javaparser-core:compileJavacc` afterwards;
- build on **JDK 21**: prefix everything with
  `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (palantir
  format lint in spotless fails on other JDKs).

## Phase 0 — branch base & docs

- [x] **Branch base decision.** `weigl/lemmata` was initially cut from
      `weigl/quantarray` (19 commits, PR #83 still open); since the lemma port
      has **zero dependency** on quantarray (proof scripts, `\seq`, `instance`,
      `JmlExpressionStmt` all exist on `main`), the branch was rebased onto
      `origin/main` (`git rebase --onto origin/main weigl/quantarray
      weigl/lemmata`). Current state: 0 ahead / 0 behind `origin/main`; Phase 8
      bumps `1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT`.
- [x] `doc/design/lemmata.md` (design + port mapping) — this phase's plan doc
- [x] `doc/design/lemmata-impl.md` (this file)

## Phase 1 — AST nodes

Two nodes, both registered in `MetaModelGenerator.ALL_NODE_CLASSES`:

### 1a. `JmlLemmaKeyword` — keyword-with-position node (kind)

- [x] Create `javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/JmlLemmaKeyword.java`
      (model: `Modifier` — a **node** wrapping a keyword enum, not a bare enum):
  - `public class JmlLemmaKeyword extends Node implements Jmlish, JmlKeyword`
  - nested `public enum Kind implements JmlKeyword { DEFINITION("="), ASSERTION("assert"),
    SCRIPT("\\by"), NONE }`
  - `private Kind keyword;` with generated-property accessors over
    `ObservableProperty.KEYWORD` (like `Modifier`), plus `jmlSymbol()` delegating
    to `keyword.jmlSymbol()`
  - constructors: `(Kind)` and TokenRange-carrying `(TokenRange, Kind)` for the
    parser; Javadoc with syntax examples
- [x] Register `JmlLemmaKeyword.class` in `ALL_NODE_CLASSES` (parents before
      children; parent `Node` already present)

### 1b. `JmlLemmaDeclaration`

- [x] Create `javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/JmlLemmaDeclaration.java`:
  - `public class JmlLemmaDeclaration extends JmlClassLevelDeclaration<JmlLemmaDeclaration>
         implements NodeWithModifiers<JmlLemmaDeclaration>, NodeWithAccessModifiers<JmlLemmaDeclaration>`
    (model file: `JmlClassExprDeclaration.java`)
  - fields:
    - `NodeList<Modifier> modifiers`
    - `SimpleName name`
    - `NodeList<Parameter> parameters`
    - `@OptionalProperty JmlLemmaKeyword kind` — the §1a keyword node
      (absent ⇔ `Kind.NONE`); **not** a bare enum
    - `@OptionalProperty Expression expression` (the `= e` / `assert e` definiens)
    - `@OptionalProperty NodeList<JmlProofStmt> script` (the `\by {…}`;
      field named `script` so the generator reuses `ObservableProperty.SCRIPT`,
      mirroring `JmlExpressionStmt`)
  - accessors per the generated-property pattern (`notifyPropertyChange(
    ObservableProperty.…)` + `setAsParentNodeOf(…)`); reuse existing properties
    `KEYWORD`/`KIND`, `NAME`, `MODIFIERS`, `PARAMETERS`, `EXPRESSION`, `SCRIPT`
    — none are new
  - constructors mirroring `JmlClassExprDeclaration`; full one
    `@AllFieldsConstructor`; keep the parser `TokenRange`-based one
  - Javadoc with syntax examples (AGENTS.md), e.g.
    `//@ instance lemma Pred(int i) = i > 0;` and the `\by { auto; }` form
- [x] Register `JmlLemmaDeclaration.class` in `ALL_NODE_CLASSES` (after
      `JmlClassExprDeclaration`; `JmlClassLevelDeclaration` is already in the list)
- [x] `./scripts/run-generators.sh` → expect: `JmlLemmaKeywordMetaModel`,
      `JmlLemmaDeclarationMetaModel` (child of
      `JmlClassLevelDeclarationMetaModel`), `visit(JmlLemmaKeyword)` +
      `visit(JmlLemmaDeclaration)` in generated visitors, `ObservableProperty`
      reuse. Cast methods **on the six base nodes only** (NOT on
      `JmlClassLevelDeclaration` — quantarray lesson: consumers use `instanceof`)
- [x] Post-generation compile fixes (CONTRIBUTING steps 3→4, pulled forward like
      quantarray):
  - `DefaultPrettyPrinterVisitor.visit(JmlLemmaKeyword)` + `.visit(JmlLemmaDeclaration)`
    — real implementations (Phase 3 spec)
  - `PrettyPrintVisitor` (legacy JML-less printer) — stubs
  - symbol-solver `DefaultVisitorAdapter` — throwing stubs
- [x] `./gradlew compileJava` green
- [x] commit (e.g. `feat(core): Phase 1 — JmlLemmaKeyword + JmlLemmaDeclaration nodes`)

## Phase 2 — grammar

- [x] Lexer: add `<LEMMA: "lemma">` in the JML keyword token block next to
      `<AXIOM: "axiom">` (`java.jj:~243`). Dedicated lexer rule, no reordering
      (AGENTS.md). **No `USE_LEMMA` token** — `use_lemma` must stay an identifier
      (otherwise the generic proof-command branch stops matching it).
- [x] `TokenTypes#getCategory(int)`: classify `LEMMA` as a keyword next to `AXIOM`
      (`TokenTypes.java:~183`) or `TokenTypesTest` breaks. (`USE_LEMMA`: nothing to
      classify — no such token.)
- [x] Class level: new production
      `JmlClassLevelDeclaration JmlLemmaDeclaration(ModifierHolder modifier):`
      per `lemmata.md` §4 (`<LEMMA> SimpleName() Parameters() ( "=" Expression()
      | <JML_ASSERT> Expression() [ <BY> JmlProofBlock() ]
      | <BY> JmlProofBlock() | {} ) ";"` → `JmlLemmaDeclaration`, kind wrapped in
      `new JmlLemmaKeyword(tokenRange(), Kind.…)`), plus an alternative in
      `JmlClassLevelInner` (`java.jj:7747`):
      `| LOOKAHEAD(1) element = JmlLemmaDeclaration(modifiers) { ret.add(element); }`
- [x] Proof command: in the proof-command production (`java.jj:7156`, inside
      `JmlProofStmt`), add the `obtain`-style contextual branch for `use_lemma`
      (`lemmata.md` §4):
      ```java
      LOOKAHEAD({ getToken(1).kind == IDENTIFIER && "use_lemma".equals(getToken(1).image) })
      <IDENTIFIER> { begin = token(); }
      args = JmlProofArgList()
      suffix = JmlProofCmdSuffix()
      { return new JmlProofCmdStmt(range(begin, token()),
            new SimpleName("use_lemma"), args, suffix); }
      ```
      Mirror the existing `obtain` lookahead at `java.jj:7162`. Place it before
      the generic `name=SimpleName() …` alternative.
  - [x] **Nice-to-have check:** since the generic command alternative already
        matches `use_lemma nospace(t);`, verify why the explicit branch matters —
        it upgrades `use_lemma` from "any command named identically" to a
        recognized command (future dedicated node/stat handling) and keeps the
        option to reject a non-call operand later. If it turns out redundant for
        the corpus, keep the branch anyway (explicitness, cheap).
- [x] **No changes** to `JmlStatement()` / `JmlExpressionStmt` / `JmlStmtKind`
      (`use_lemma` is a proof command, not a method-body statement)
- [x] `./gradlew :javaparser-core:compileJavacc` — 0 errors; no new LOOKAHEAD
      conflicts in `JmlClassLevelInner` / proof-command production
- [x] Smoke (jshell against `javaparser-core/build/classes/java/main`):
  - `//@ instance lemma Pred(int i) = i > 0;` via `JavaParser.parseJmlClassLevel(...)`
    → `JmlLemmaDeclaration`, modifiers `[JML_INSTANCE]`,
    kind node `Kind.DEFINITION` with the `=` token range, expr `i > 0`
  - `//@ static no_state lemma nospace(\seq s) \by { oss; use_lemma nospace(t); };`
    → kind `SCRIPT`, script length 2, second element a
    `JmlProofCmdStmt` named `use_lemma`
  - `//@ lemma P(int i) assert i > 0 \by { auto; };` → kind `ASSERTION`
  - negative: `//@ lemma P(int i) == i > 0;` → parse problem
  - no method-level `use_lemma` statement parses (expected — deferred)
  (All of the above covered by `LemmaParseSmokeTest` in `jmlparser-core-testing`.)
- [x] commit

## Phase 3 — printing

- [x] `ConcreteSyntaxModel`: entries for `JmlLemmaKeyword` and
      `JmlLemmaDeclaration` — modifiers, `lemma`, `name`, `( params )`, per kind
      `= expr` / `assert expr [\by {…}]` / `\by {…}`, `;`
      (`ConcreteSyntaxModelTest` requires an entry for every metamodel node)
- [x] `DefaultPrettyPrinterVisitor.visit(JmlLemmaDeclaration)`:
      `printModifiers(...); "lemma"; name; (params); kind.jmlSymbol() (from the
      kind node) + expr; JmlProofPrint(script) if present; ";"`
- [x] `JmlProofCmdStmt` printing: `name args suffix` — already generic; `use_lemma`
      prints via the `"use_lemma"` SimpleName (no change expected)
- [x] `./scripts/run-generators.sh` drift check; `./gradlew :javaparser-core-testing:test`
      green
- [ ] commit

## Phase 4 — validation

- [ ] `JmlProcessor.JmlReplaceVisitor` (or the language-level validators):
  - defensive: exactly one body form — kind node must be consistent with the
    presence of `expression`/`script` (`DEFINITION` ⇔ `=`, `ASSERTION` ⇔
    `assert`, `SCRIPT` ⇔ `\by`, `NONE` ⇔ no body); reports a problem on
    hand-built inconsistent trees
  - lemma parameters must be plain (`!parameter.isVarArgs()`, no receiver
    parameter)
  - `use_lemma` proof command: operand args should be a `MethodCallExpr` — report
    a *problem*, not a crash (lint/type level)
  - `instance`+`static` mutual exclusion (if the generic modifier validator does
    not already cover JML_INSTANCE on this node)
- [ ] jshell smoke: hand-built `JmlLemmaDeclaration` with kind `DEFINITION` but no
      expression reports; `instance lemma Pred(int i) = i > 0;` is clean
- [ ] `:jmlparser-core-testing:test` + `:jmlparser-jml-tests:test` green
- [ ] commit

## Phase 5 — M1 tests

- [ ] Parser tests (`jmlparser-jml-tests`, new class e.g.
      `LemmaDeclarationTest` in `.../jml/parser`):
  - all four body forms parse; AST assertions on kind node (enum + token range),
    expr, script, modifiers
  - `use_lemma` inside `\by {…}` → `JmlProofCmdStmt` name `use_lemma`
  - negative: `==`-form, missing `;`, `lemma (…)` (no name)
- [ ] CSM round-trip: new resource `csm_jml_classlevel.txt` + a loop wired into
      `ConcreteSyntaxModelTest` (mirror quantarray's `csm_jml_expression.txt`)
      with the four lemma forms round-tripping verbatim
- [ ] `LexicalPreservingPrinterTest`: a class with a `lemma` and a method whose
      `\by {…}` script contains `use_lemma` round-trips byte-for-byte
- [ ] Corpus fixture `fullexamples/tested/Lemmas.java` (like `QuantArray.java`):
      `instance lemma Pred(int i) = i > 0`, a `\by`-script lemma whose script
      calls `use_lemma nospace(t)`, and a method with `//@ assert … \by {…}`
      (miniature of the VerifyThis 2025.1 `MinExcludant0` example, with the
      method-level `use_lemma` moved *into* the scripts per §3)
- [ ] new fragment fixtures `fragments/decl_*.txt` / `stmt_*.txt` if the corpus
      harness picks them up; `FragmentTest` + `FullExamplesTest` green
- [ ] commit

## Phase 6 — symbol solver

- [ ] Lemma parameters resolvable inside `expression` and `script` (reuse
      `MethodDeclaration`-style per-parameter resolution; `JmlLogicType` params
      like `\seq`/`\bigint` must not crash)
- [ ] `NameResolutionTest`-style expectation (`//?`-file) for
      `Pred(int i) = i > 0`: `i` resolves to `int` param, body to `boolean`
- [ ] `use_lemma` proof-command args resolve as ordinary `JmlProofArg`
      expressions (`nospace(\array2seq(a))` as a `MethodCallExpr`)
  - [ ] optional: `JmlLemmaDeclaration` re-exported as a resolvable symbol if a
        cheap hook exists (model-method symbols); otherwise defer and document
- [ ] `:jmlparser-symbol-solver-testing:test` + `:jmlparser-jml-tests:test` green
- [ ] commit

## Phase 7 — M3 backends (no-crash + minimal semantics)

- [ ] `tools/jml2java` (`Jml2JavaTranslator`): lemma declarations are spec-only —
      emit **no** Java member; verify the existing `expr.yaml`/print fixtures
      still pass
- [ ] `tools/smt` (`JmlExpr2Smt`): no crash when a formula's tree contains a
      lemma declaration; lemma definitions not encoded (documented no-op)
- [ ] `tools/wd` (`WDVisitor`): no-op `visit(JmlLemmaDeclaration)`
      (like the existing `visit(JmlClassExprDeclaration)` at `WDVisitor.kt:185`);
      optional: well-definedness of the lemma body = `wd(params) ∧ wd(expr) ∧
      wd(script)` with a CSV fixture
- [ ] `tools/stat` (`StatVisitor`, `StatisticKey`): `totalLemmas` plus per-name
      `LEMMA_<name>` counter; optional: `useLemmaCommands` counter for
      `JmlProofCmdStmt(name="use_lemma")`
- [ ] `tools/lsp`:
  - `JmlCatchSymbols`: document symbol (Kind.Method/Function) for
    `JmlLemmaDeclaration`
  - `CodeActionCollector`: no crash on the new nodes
  - JML keyword highlighting: add `lemma` to the JML construct keywords (the
    `LEMMA` entry at `KeyDocumentHighlighter.kt:195` is for `.key` files — do not
    conflate; `use_lemma` stays an identifier, highlighted as a command)
- [ ] `tools/lint` visitors: no-crash visits for `JmlLemmaDeclaration`,
      `JmlLemmaKeyword`, and `JmlProofCmdStmt(name="use_lemma")`
- [ ] all touched tool modules' suites green; `spotlessApply` + `spotlessCheck`,
      checkstyleMain (root) green
- [ ] commit

## Phase 8 — changelog & version

- [ ] `jmltk-changelog.md`: add `Unreleased — 1.2.0-SNAPSHOT` entry listing the
      lemma/`use_lemma` port (Phases 1-7 with commit hashes); bump
      `1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT` (feature ⇒ minor)
- [ ] `gradle.properties`: `version=1.2.0-SNAPSHOT`
- [ ] Full corpus (`FragmentTest`, `FullExamplesTest`) green; root
      `spotlessCheck` + `checkstyleMain` green; all touched module suites re-run
- [ ] commit; then open PR against `main` on `jmltoolkit/jmltk` (base `main`,
      head `weigl/lemmata`, title like
      `feat(core,tools): JML lemma declarations and use_lemma proof commands (port of KeY #3971)`)

## Deferred / open

- [ ] **Method-level `use_lemma` statement** (KeY `methodlevel_element`,
      `//@ use_lemma nospace(\array2seq(a));` in a normal method body): explicitly
      deferred per design §3. Likely follow-up: a `JmlExpressionStmt`
      (`JmlStmtKind.USE_LEMMA`-equivalent) or a dedicated proof-command-in-
      statement position — only when a KeY consumer needs it parsed outside
      `\by {…}`.
- [ ] Lemma-as-resolvable-symbol (Phase 6 optional) — decide when a consumer asks
      for callable lemmas in specs.
