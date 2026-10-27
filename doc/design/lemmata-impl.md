# Implementation Plan — JML lemmas & `use_lemma` proof commands (port of KeY PR #3971)

Executable checklist for the design in `lemmata.md`: a class-body `lemma`
declaration in the KeY proof-script form only (`//@ lemma nospace(\seq s) \by {…}`;
KeY's `assertionProof: BY (proofCmd | LBRACE (proofCmd)+ RBRACE)` is mandatory, so
the `\by` script is required) and the proof-script command `use_lemma
<call>;` — parsed the way `obtain` is (contextual keyword, **no** new lexer
token), producing a `JmlProofCmdStmt`. The KeY method-level `use_lemma`
statement form is **deferred by decision** (see `lemmata.md` §3).

> **Scope note (follow-up commit):** the initial port also implemented a
> JMLtk-superset `lemma Pred(int i) = i > 0` *definitional* form and an
> `assert e` form (`JmlLemmaKeyword` kind node). Per decision both forms are
> dropped — the PR ships only the pure `\by {…}` script form, exactly matching
> KeY #3971's `lemma_declaration`. This file describes the final state.

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

## Phase 1 — AST node

One node, registered in `MetaModelGenerator.ALL_NODE_CLASSES`:

### 1a. ~~`JmlLemmaKeyword`~~ — dropped

An initial draft added a `JmlLemmaKeyword` keyword-with-position node (model:
`Modifier`, a node wrapping a `DEFINITION`/`ASSERTION`/`SCRIPT`/`NONE` kind
enum). The definitional/asserted forms are out of scope (see scope note), so no
keyword node is needed and none exists in the final tree.

### 1b. `JmlLemmaDeclaration`

- [x] Create `javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/JmlLemmaDeclaration.java`:
  - `public class JmlLemmaDeclaration extends JmlClassLevelDeclaration<JmlLemmaDeclaration>
         implements NodeWithModifiers<JmlLemmaDeclaration>, NodeWithAccessModifiers<JmlLemmaDeclaration>`
    (model file: `JmlClassExprDeclaration.java`)
  - fields:
    - `NodeList<Modifier> modifiers`
    - `SimpleName name`
    - `NodeList<Parameter> parameters`
    - `NodeList<JmlProofStmt> script` — the mandatory `\by {…}`; the **sole**
      body property (field named `script` so the generator reuses
      `ObservableProperty.SCRIPT`, mirroring `JmlExpressionStmt`)
  - accessors per the generated-property pattern (`notifyPropertyChange(
    ObservableProperty.…)` + `setAsParentNodeOf(…)`); reuse existing properties
    `NAME`, `MODIFIERS`, `PARAMETERS`, `SCRIPT` — none are new
  - constructors mirroring `JmlClassExprDeclaration`; full one
    `@AllFieldsConstructor`; keep the parser `TokenRange`-based one
  - Javadoc with syntax examples (AGENTS.md), e.g.
    `//@ public static no_state lemma nospace(\seq s) \by { oss; auto; };`
- [x] Register `JmlLemmaDeclaration.class` in `ALL_NODE_CLASSES` (after
      `JmlClassExprDeclaration`; `JmlClassLevelDeclaration` is already in the list)
- [x] `./scripts/run-generators.sh` → expect: `JmlLemmaDeclarationMetaModel`
      (child of `JmlClassLevelDeclarationMetaModel`),
      `visit(JmlLemmaDeclaration)` in generated visitors, `ObservableProperty`
      reuse. Cast methods **on the six base nodes only** (NOT on
      `JmlClassLevelDeclaration` — quantarray lesson: consumers use `instanceof`)
- [x] Post-generation compile fixes (CONTRIBUTING steps 3→4, pulled forward like
      quantarray):
  - `DefaultPrettyPrinterVisitor.visit(JmlLemmaDeclaration)`
    — real implementation (Phase 3 spec)
  - `PrettyPrintVisitor` (legacy JML-less printer) — stub
  - symbol-solver `DefaultVisitorAdapter` — throwing stub
- [x] `./gradlew compileJava` green
- [x] commit (e.g. `feat(core): Phase 1 — JmlLemmaDeclaration node`)

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
      per `lemmata.md` §4
      (`<LEMMA> SimpleName() Parameters() <BY> JmlProofBlock() ";"` →
      `JmlLemmaDeclaration(modifier.modifiers, name, params, script)`; the
      `\by` script is mandatory), plus an alternative in
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
  - `//@ static no_state lemma nospace(\seq s) \by { oss; use_lemma nospace(t); };`
    via `JavaParser.parseJmlClassLevel(...)` → `JmlLemmaDeclaration`, modifiers
    `[STATIC, JML_NO_STATE]`, script length 3, second element a
    `JmlProofCmdStmt` named `use_lemma`
  - negative: `//@ lemma P(int i) == i > 0;`, `//@ lemma helper();` (missing
    `\by`) and the dropped `= expr` / `assert e` forms → parse problems
  - no method-level `use_lemma` statement parses (expected — deferred)
  (All of the above covered by `LemmaParseSmokeTest` in `jmlparser-core-testing`.)
- [x] commit

## Phase 3 — printing

- [x] `ConcreteSyntaxModel`: entry for `JmlLemmaDeclaration` — modifiers,
      `lemma`, `name`, `( params )`, mandatory `\by { … }` suffix, `;`
      (`ConcreteSyntaxModelTest` requires an entry for every metamodel node)
- [x] `DefaultPrettyPrinterVisitor.visit(JmlLemmaDeclaration)`:
      `printModifiers(...); "lemma"; name; (params);` then `printSuffix(script)`
      (the existing proof-script suffix printer emits ` \by {…}` / ` \by …;`)
- [x] `JmlProofCmdStmt` printing: `name args suffix` — already generic; `use_lemma`
      prints via the `"use_lemma"` SimpleName (no change expected)
- [x] `./scripts/run-generators.sh` drift check; `./gradlew :javaparser-core-testing:test`
      green
- [x] commit

## Phase 4 — validation

- [x] `JmlProcessor.JmlReplaceVisitor` (or the language-level validators):
  - defensive: the `\by` script is mandatory and must be non-empty (a
    hand-built/older-parser `JmlLemmaDeclaration` with an empty script is
    reported); no kind/expression consistency checks exist anymore — the
    grammar only builds script-form lemmas
    (done in the language-level validators — a new `JmlLemmaValidator` chunk
    wired into `CommonValidators`, since `JmlReplaceVisitor` cannot reliably
    reach attached lemma nodes: `ModifierVisitor.visit(NodeList)` iterates a
    snapshot and the inner `parseJmlClassLevel` results are validated under an
    `ArbitraryNodeContainer` whose `accept(VoidVisitor)` is a no-op)
  - lemma parameters must be plain (`!parameter.isVarArgs()`, no receiver
    parameter)
    (varargs is checked on the AST; a receiver parameter is not representable
    in `NodeList<Parameter>` — the grammar's `Parameters()` silently drops one,
    to be addressed when the grammar is next touched)
  - `use_lemma` proof command: operand args should be a `MethodCallExpr` — report
    a *problem*, not a crash (lint/type level)
    (checked in `JmlLemmaValidator.visit(JmlProofCmdStmt)`)
  - `instance`+`static` mutual exclusion (if the generic modifier validator does
    not already cover JML_INSTANCE on this node)
    (the generic validator did not cover the node; added
    `ModifierValidator.visit(JmlLemmaDeclaration)` with
    `PUBLIC/PROTECTED/PRIVATE/STATIC/JML_INSTANCE/JML_NO_STATE` — the
    at-most-one-of check inside `validateModifiers` rejects `instance`+`static`)
- [x] jshell smoke: hand-built `JmlLemmaDeclaration` with an empty script
      reports; `lemma nospace(\seq s) \by { oss; auto; };` is clean
- [x] `:jmlparser-core-testing:test` + `:jmlparser-jml-tests:test` green
- [x] commit

## Phase 5 — M1 tests

- [x] Parser tests (`jmlparser-jml-tests`, new class e.g.
      `LemmaDeclarationTest` in `.../jml/parser`):
  - the `\by {…}` script form parses; AST assertions on the script list,
    modifiers; `use_lemma` inside `\by {…}` → `JmlProofCmdStmt` name `use_lemma`
  - negative: `= expr` / `assert e` forms, missing `\by`
    (`lemma helper();`), missing `;`, `lemma (…)` (no name)
  (9 tests in `jmlparser-jml-tests/.../parser/LemmaDeclarationTest.java`)
- [x] CSM round-trip: new resource `csm_jml_classlevel.txt` + a loop wired into
      `ConcreteSyntaxModelTest` (mirror quantarray's `csm_jml_expression.txt`)
      with the script lemma forms round-tripping verbatim
  (round-trips the *unmodified* lemma forms; JML-modifier'd forms
  normalize to `jml_…` in CSM by design and are covered by the parser + LPP
  tests instead. Required a CSM bugfix first: `token(BY)` printed the
  JavaCC-escaped `\\by` (doubled backslash); the lemma suffix uses
  `string(BY, "\\by")`)
- [x] `LexicalPreservingPrinterTest`: a class with a `lemma` and a method whose
      `\by {…}` script contains `use_lemma` round-trips byte-for-byte
      (`JmlLemmaLexicalPreservingPrinterTest` in `.../jml/printer`)
- [x] Corpus fixture `fullexamples/tested/Lemmas.java` (like `QuantArray.java`):
      `\by`-script lemmas (one whose script calls `use_lemma`), and a method
      with `//@ assert … \by {…}`
      (miniature of the VerifyThis 2025.1 `MinExcludant0` example, with the
      method-level `use_lemma` moved *into* the scripts per §3)
      (picked up by `FullExamplesTest.createTestedJmlTests`)
- [x] new fragment fixtures `fragments/decl_*.txt` / `stmt_*.txt` if the corpus
      harness picks them up; `FragmentTest` + `FullExamplesTest` green
      (`fragments/decl_jml_lemma.txt`, picked up by `FragmentTest.testClassLevel`)
- [x] commit

## Phase 6 — symbol solver

- [x] Lemma parameters resolvable inside `script` (reuse
      `MethodDeclaration`-style per-parameter resolution; `JmlLogicType` params
      like `\seq`/`\bigint` must not crash)
      (new `JmlLemmaDeclarationContext` in
      `javaparser-symbol-solver-core/.../contexts/jml/`, registered in
      `JavaParserFactory#getContext`; it exposes the lemma's `Parameter`s like
      `MethodContext` does, but cannot reuse
      `AbstractMethodLikeDeclarationContext` because `JmlLemmaDeclaration` is
      not a `NodeWithParameters`/`NodeWithTypeParameters` — only parameters are
      handled, everything else delegates to the enclosing class context.
      `\seq`/`\bigint` parameter names resolve fine; their *type* computation
      previously infinite-looped (`JmlLogicType.convertToUsage` → `resolve()` →
      `toResolvedType()` → `convertToUsage()` → …) and crashed with
      `StackOverflowError`. `convertToUsage` now throws a clean
      `UnsolvedSymbolException` — JML logic types have no Java representation,
      so an unresolved-type problem is the documented resolution failure.)
- [x] `NameResolutionTest`-style expectation (`//?`-file) for the script-form
      lemma `seqLemma(\seq s, \bigint n) \by { use_lemma nospace(s, n); auto; };`:
      `s`/`n` resolve to their parameters from inside the `\by {…}` script
      (fixture `JmlLemmaResolutionTest.java` + `lemmaParameters` test — the
      args sit under a `JmlProofCmdStmt`/`JmlProofArg`/`MethodCallExpr` chain;
      `AbstractJavaParserContext#getParent` walks through the unregistered
      proof nodes to the lemma context; `\seq`/`\bigint` parameter *types*
      raise a clean `UnsolvedSymbolException`, so the fixture carries
      `e type:`/`type:` expectations)
  - [x] ~~optional: `JmlLemmaDeclaration` re-exported as a resolvable symbol if a
        cheap hook exists (model-method symbols); otherwise defer and document~~
      (deferred: the symbol solver has no model-method/method-like declaration
      adapter for JML (only `JmlJavaParserQuantifiedVariableDeclaration` for
      bound vars), so there is no cheap hook to hang a lemma symbol re-export on;
      documented here for a future JavaParser-mirroring design)
- [x] `:jmlparser-symbol-solver-testing:test` + `:jmlparser-jml-tests:test` green
      (also `:jmlparser-core:test` + `:jmlparser-core-testing:test` for the
      `JmlLogicType` change)
- [x] commit (`ad7a0a9ae`)

## Phase 7 — M3 backends (no-crash + minimal semantics)

- [x] `tools/jml2java` (`Jml2JavaTranslator`): lemma declarations are spec-only —
      emit **no** Java member; verify the existing `expr.yaml`/print fixtures
      still pass
      (jml2java is expression-only — `Jml2JavaFacade.translate` never reaches a
      body declaration — so "no member" is trivially satisfied; added a
      documented no-op `visit(JmlLemmaDeclaration): Expression? = null` in
      `Jml2JavaVisitor` next to the `JmlQuantifiedExpr` override.
      `:tools:jml2java:test` incl. the `expr.yaml` fixtures stays green)
- [x] `tools/smt` (`JmlExpr2Smt`): no crash when a formula's tree contains a
      lemma declaration; lemma definitions not encoded (documented no-op)
      (`visit(JmlLemmaDeclaration): SExpr? = null` above the companion object;
      `:tools:smt:test` green)
- [x] `tools/wd` (`WDVisitor`): no-op `visit(JmlLemmaDeclaration)`
      (next to `visit(JmlClassExprDeclaration)` at `WDVisitor.kt`; a lemma is a
      proof obligation for KeY, not an assertion, so it contributes `⊤` to the
      well-definedness VC). The optional stronger body WD
      (`wd(params) ∧ wd(script)`) is deferred: WD generation is
      expression-driven and no lemma-body fixture exists yet. `:tools:wd:test`
      green.
- [x] `tools/stat` (`StatVisitor`, `StatisticKey`): `totalLemmas` plus per-name
      `LEMMA_<name>` counter, and `useLemmaCommands` for `JmlProofCmdStmt(name=
      "use_lemma")`
      (new `StatisticKey.totalLemmas`/`useLemmaCommands` data objects, public
      `StatisticKeys.lemma(name)` → `LEMMA_<name>` (normalized uppercase);
      `JmlStatisticsReporter.visit(JmlLemmaDeclaration)` and
      `visit(JmlProofCmdStmt)`; `StatVisitor.visit(JmlLemmaDeclaration)` emits a
      `<jml-lemma name=…>` element with modifiers (no expression child — lemmas
      are script-only); `testLemmaCounting` in `JmlStatisticsReporterTest`. CLI
      end-to-end on `fullexamples/tested/Lemmas.java`:
      `jml-lemma name="nospace|intEq"` with `hasno_state` modifiers present.)
- [x] `tools/lsp`:
  - `JmlCatchSymbols`: document symbol for `JmlLemmaDeclaration`
    (`SymbolKind.Function`, name, `asRange`, detail
    `"Jml lemma (proof script)"`)
  - `CodeActionCollector`: no lemma override — there is no lemma expression to
    offer a well-definedness check on (mirrors `JmlClassExprDeclaration`)
  - JML keyword highlighting: **no change needed** — `TokenTypes#getCategory`
    classifies `LEMMA` as `Keyword` (it already did in Phase 1), so
    `JmlDocumentHighlighter`'s token-category lookup highlights `lemma`
    automatically; `use_lemma` stays an `Identifier`. The `LEMMA` entry at
    `KeyDocumentHighlighter.kt:195` is for `.key` syntax and was left alone.
- [x] `tools/lint` visitors: no-crash verified for `JmlLemmaDeclaration` and
      `JmlProofCmdStmt(name="use_lemma")`
      (`JmlDuplicateNamesValidator.checkType` dispatches over members with a
      non-exhaustive `when`, so lemmas are simply skipped; all other rules visit
      clause/expression node types; `JmlLintingFacade.lint` also wraps every
      linter in try/catch. No guards needed.)
- [x] all touched tool modules' suites green (`wd`, `stat`, `jml2java`, `smt`,
      `lsp`, `lint`, plus `cli` for the end-to-end stat run); `spotlessApply` +
      `spotlessCheck` green, `checkstyleMain` green. Phase 7 touches no core or
      generated sources, so the generator drift gate stays clean (still re-run:
      no drift).
- [ ] commit

## Phase 8 — changelog & version

- [x] `jmltk-changelog.md`: add `Unreleased — 1.2.0-SNAPSHOT` entry listing the
      lemma/`use_lemma` port (Phases 1-7 with commit hashes); bump
      `1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT` (feature ⇒ minor)
      (entry also documents the fork-local SemVer scheme switch; per-phase hashes
      `ba73811bc` … `f248314f5` + design `d2c19333d`)
- [x] `gradle.properties`: `version=1.2.0-SNAPSHOT`
- [x] Full corpus (`FragmentTest`, `FullExamplesTest`) green; root
      `spotlessCheck` + `checkstyleMain` green; all touched module suites re-run
      (jml-tests forced rerun incl. `decl_jml_lemma.txt` fragment +
      `fullexamples/tested/Lemmas.java`; checkstyle reports only pre-existing
      warnings — none in the new/changed files)
- [x] commit; then open PR against `main` on `jmltoolkit/jmltk` (base `main`,
      head `weigl/lemmata`, title like
      `feat(core,tools): JML lemma declarations and use_lemma proof commands (port of KeY #3971)`)

## Follow-up — drop `= expr` and `assert e` (script-only port)

- [x] Per decision, the PR now ships **only** the KeY proof-script form
      `[modifiers] lemma Name(params) \by { … };` (see `lemmata.md` §3/§4):
  - grammar: `<LEMMA> SimpleName() Parameters() <BY> JmlProofBlock() ";"` —
    no `= expr` / `assert e` alternatives, no `kind`/`expr` locals
  - `JmlLemmaKeyword` node + its `MetaModel` file deleted; `JmlLemmaDeclaration`
    loses `kind`/`expression`, `script` becomes the **sole** (mandatory) body
    property; `ALL_NODE_CLASSES` updated; regenerated visitors/metamodel
  - `ConcreteSyntaxModel` drops the keyword entry and the kind-dependent
    connector; the lemma CSM is `modifiers lemma name ( params ) \by { script };`
  - `JmlLemmaValidator` enforces a non-empty script (+ unchanged varargs and
    `use_lemma` operand checks)
  - fixtures/corpus (`LemmaParseSmokeTest`, `LemmaDeclarationTest`,
    `JmlLemmaLexicalPreservingPrinterTest`, `decl_jml_lemma.txt`,
    `csm_jml_classlevel.txt`, `fullexamples/tested/Lemmas.java`,
    `JmlLemmaResolutionTest.java`, `JmlStatisticsReporterTest`) reworked to
    script-only lemmas, with negative tests for the dropped forms
  - tools: `stat` element loses the expression child, `lsp` code-action override
    removed, symbol detail is `"Jml lemma (proof script)"`
  - committed as a single follow-up commit (no force-push) and documented in
    `jmltk-changelog.md`

## Deferred / open

- [ ] **Method-level `use_lemma` statement** (KeY `methodlevel_element`,
      `//@ use_lemma nospace(\array2seq(a));` in a normal method body): explicitly
      deferred per design §3. Likely follow-up: a `JmlExpressionStmt`
      (`JmlStmtKind.USE_LEMMA`-equivalent) or a dedicated proof-command-in-
      statement position — only when a KeY consumer needs it parsed outside
      `\by {…}`.
- [ ] Lemma-as-resolvable-symbol (Phase 6 optional) — decide when a consumer asks
      for callable lemmas in specs.
