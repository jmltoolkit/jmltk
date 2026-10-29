# Implementation Plan — JML predicate declarations (`//@ instance predicate Pred(int i) = i > 0`)

Executable checklist for the design in `predicate.md`. Each phase ends with the
listed checks green; commit per phase.

Rules that apply throughout (identical to the other fork implementation plans):

- every phase ends compile-green; commit per phase;
- never hand-edit generated files — after AST/grammar changes run
  `./gradlew :javaparser-core:compileJavacc` and then
  `./scripts/run-generators.sh` (its final `git diff --exit-code` **fails while
  your changes are uncommitted** — that is the drift check; commit the
  regenerated output and re-run to see it pass);
- use `./gradlew` from the repo root;
- build on **JDK 21**:
  `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew …`
  (palantir format lint in spotless fails otherwise).

## Phase 0 — branch base & docs

- [x] `git checkout -b weigl/predicate origin/main` (done: branch exists,
      based on `main` @ `50560208f`)
- [x] `doc/design/predicate.md` — design + language surface (this phase)
- [x] `doc/design/predicate-impl.md` — this file

## Phase 1 — AST node `JmlPredicateDeclaration`

- [x] Create
      `javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/JmlPredicateDeclaration.java`
      (model file: `JmlRepresentsDeclaration.java`):
  - `public class JmlPredicateDeclaration extends JmlClassLevelDeclaration<JmlPredicateDeclaration>
       implements NodeWithName<…>, NodeWithModifiers<…>, NodeWithAccessModifiers<…>`
  - fields in this order: `NodeList<Modifier> modifiers`, `SimpleName name`,
    `NodeList<Parameter> parameters`, `Expression body`,
    `NodeList<SimpleName> jmlTags`
  - `@AllFieldsConstructor (jmlTags, modifiers, name, parameters, body)`,
    default constructor, main constructor `(TokenRange, jmlTags, modifiers,
    name, parameters, body)`, Javadoc with the syntax example
- [x] Register `JmlPredicateDeclaration.class` in
      `MetaModelGenerator.ALL_NODE_CLASSES` (after `JmlClassAccessibleDeclaration`,
      line ~242)
- [x] `./gradlew :jmlparser-core:compileJava` green (node compiles standalone)
- [x] `./scripts/run-generators.sh` — regenerates
      `metamodel/JmlPredicateDeclarationMetaModel.java`, `JavaParserMetaModel`
      wiring, visitors (Generic/Void/Clone/Equals/Modifier/HashCode/Observer…),
      node methods (accept/property/remove/replace/clone/getMetaModel/fluent/typecast)
  - check: `git diff --stat` shows the new node + all generated files
- [x] commit per phase

## Phase 2 — grammar & tokens

- [x] `java.jj`: add `<PREDICATE: "predicate">` to the
      `<JML_MULTI_CONTRACT,JML_SINGLE_CONTRACT> TOKEN` block (no reordering)
- [x] `java.jj`: new production
      `JmlClassLevelDeclaration JmlClassPredicate(ModifierHolder modifiers):` —
      `<PREDICATE> id=SimpleName() parameters=Parameters() "=" expr=Expression() ";"`
      building `new JmlPredicateDeclaration(range(begin, token()), new NodeList<>(),
      modifiers.modifiers, id, parameters.a, expr)`, range begin via
      `orIfInvalid(modifiers.begin, begin)`
- [x] `java.jj`: `JmlClassLevelInner` gains
      `| LOOKAHEAD(1) element = JmlClassPredicate(modifiers) { ret.add(element); }`
- [x] `TokenTypes.java`: add `case PREDICATE:` to `getCategory`
- [x] `./gradlew :javaparser-core:compileJavacc` (regenerates JavaToken kinds +
      GeneratedJavaParserConstants + parser)
- [x] `./gradlew :javaparser-core:compileJava` green
- [x] smoke parse in jshell/`JavaParser`: `//@ instance predicate Pred(int i) = i > 0;`
      in a class body yields `JmlPredicateDeclaration` member with
      `parameters = [int i]`, `body = i > 0`, `modifiers = [instance]`
- [x] commit per phase

## Phase 3 — CSM & pretty printing

- [x] `ConcreteSyntaxModel.java`: entry for `JmlPredicateDeclaration.class`
  `sequence(child(MODIFIERS), space(), token(GeneratedJavaParserConstants.PREDICATE),
  space(), child(NAME), token(LPAREN), list(PARAMETERS, sequence(comma(), space())),
  token(RPAREN), space(), token(ASSIGN), space(), child(BODY), semicolon())`
- [x] `DefaultPrettyPrinterVisitor`: `visit(JmlPredicateDeclaration, Void)` inside
      `wrapInJmlIfNeeded(...)` printing modifiers, `predicate `, name,
      `(params) = body;`
  (mirrors `visit(JmlClassExprDeclaration)`)
- [x] round-trip: `toString()` of the parsed declaration reproduces the
      canonical `instance predicate Pred(int i) = i > 0;`
- [x] `./gradlew :javaparser-core:test :jmlparser-core-testing:test` green
- [x] commit per phase

## Phase 4 — validation

- [x] `ModifierValidator.visit(JmlPredicateDeclaration, ProblemReporter)`:
  - allowed modifiers: `PUBLIC, PROTECTED, PRIVATE, STATIC, FINAL, JML_MODEL,
    JML_INSTANCE, JML_NULLABLE…, math modes`;
  - parameter modifiers: `FINAL, JML_NULLABLE…`;
  - duplicate formal parameter names → `reporter.report(...)`
- [x] tests: `//@ instance predicate Pred(int i) = i > 0;` parses clean;
      `//@ static predicate P(int i, int i) = ...` reports duplicate; disallowed
      modifier reported
- [x] `./gradlew :jmlparser-core:test :jmlparser-jml-tests:test` green
- [x] commit per phase

## Phase 5 — M1 tests

- [x] `src/test/resources/fragments/decl_jml_predicate_pred.txt` (+ instance + var
      forms) — picked up by `FragmentTest.testClassLevel`
- [x] `src/test/resources/fullexamples/tested/Predicates.java` with the
      motivating example (covered by `FullExamplesTest` with `processJml=true`)
- [x] `jmlparser-jml-tests/.../parser/PredicateDeclarationTest.java`:
  - parse a full class with `processJml=true`; assert the `JmlPredicateDeclaration`
    member (name, modifiers incl. `instance`, parameter list, body AST, ranges)
  - CSM round-trip via `ConcreteSyntaxModel.genericPrettyPrint`
  - pretty-printer round-trip via `toString()`
  - lexical-preserving printer round-trip
- [x] `./gradlew :jmlparser-jml-tests:test` green
- [x] commit per phase

## Phase 6 — symbol resolution

- [x] `JmlPredicateDeclarationContext` in
      `javaparser-symbol-solver-core/.../contexts/jml/`: `solveSymbol(name)`
      over `getParameters()` → `JavaParserSymbolDeclaration.parameter`,
      fallback `super.solveSymbol`
- [x] `JavaParserFactory`: register
      `if (node instanceof JmlPredicateDeclaration) return new JmlPredicateDeclarationContext(...)`
- [x] `DefaultVisitorAdapter`: `visit(JmlPredicateDeclaration, Boolean)` throwing
      `UnsupportedOperationException` stub (matches sibling declarations)
- [x] resolution test resource
      `src/test/resources/com/github/jml/resolution/JmlPredicateResolutionTest.java`
      with `//?` expectations; `NameResolutionTest` gains `jmlPredicate()` test
- [x] `./gradlew :jmlparser-symbol-solver-testing:test :jmlparser-jml-tests:test` green
- [x] commit per phase

## Phase 7 — M3 backends

- [x] **stat**: `StatisticKey` + `totalPredicates` counter in
      `JmlStatisticsReporter.visit(JmlPredicateDeclaration)`; `StatVisitor`
      `<predicate name="…">` element with modifier attrs + body expr stats;
      `JmlStatisticsReporterTest` case
- [x] **wd**: `WDVisitor.kt` `visit(JmlPredicateDeclaration): SExpr = wd(nullable body)`
      + wd fixture (well-defined predicate body; not-wd body)
- [x] **lsp**: `JmlCatchSymbols` predicate `DocumentSymbol` (kind `Method`);
      `CodeActionCollector` well-definedness check on predicate body
- [x] module tests green for all touched tools
- [x] commit per phase

## Phase 8 — changelog & version

- [x] `gradle.properties`: `version=1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT`
      (feature ⇒ minor bump, fork-local SemVer per `AGENTS.md`)
- [x] `jmltk-changelog.md`: add `### Version 1.2.0-SNAPSHOT (unreleased)` section
      (Added: predicate declarations) + note that 1.x is fork-local SemVer
- [x] commit per phase

## Phase 9 — QA & PR

- [x] full corpus green: `FragmentTest`, `FullExamplesTest`
- [x] root `spotlessCheck` + `checkstyleMain` green (pre-existing warnings only)
- [x] `./scripts/run-generators.sh` drift check passes (committed regenerated state)
- [x] all touched module suites re-run green
- [x] `git push -u origin weigl/predicate`
- [x] `gh pr create --base main --head weigl/predicate` with phase summary

## Deviations & notes (recorded during implementation)

Commits were grouped logically rather than strictly per phase (the generated files interlock):
`2669b52ca` Phase 0 → `3fdd6dbae` Phases 1–4 → `916272a56` Phase 5 → `4decfae3b` Phase 6 →
`830a5fb3d` Phase 7 → `a6cd3a564` Phase 8.

- **Node interfaces**: `JmlPredicateDeclaration` implements `NodeWithSimpleName` (leaf name) — the draft's
  `NodeWithName` would expose the qualified-name API unnecessarily; `NodeWithModifiers` +
  `NodeWithAccessModifiers` as drafted. Field/ctor order `(jmlTags, modifiers, name, parameters, body)` kept.
- **CSM**: `child(MODIFIERS)` crashes for the list property (`CsmSingleReference`); the entry uses the
  `modifiers()` helper instead (same as the reference `weigl/lemmata` branch). JML modifiers print as
  `jml_instance` in CSM (pre-existing `PrintingHelper` enum quirk, also in the reference branch), so the exact
  CSM round-trip test uses the modifier-less form and `toString()` covers modifier printing.
- **Validation tests** (duplicate parameters, disallowed modifier) landed in Phase 5's `PredicateDeclarationTest`.
  `ModifierValidator` validates predicate parameters via a per-`Parameter` loop; duplicate detection reports
  `Parameter 'i' is already defined`.
- **Grammar**: `<PREDICATE: "predicate">` lexer token added in the JML states only (after `AXIOM`); `JmlClassPredicate`
  dispatches via `LOOKAHEAD(1)` in `JmlClassLevelInner`; `case PREDICATE:` added to `TokenTypes#getCategory`.
- **Generator ordering gotchas**:
  1. `:jmlparser-core-generators:run` needs the `:jmlparser-core:compileJavacc` output to exist first
     (`TokenKindGenerator` reads `GeneratedJavaParserConstants`), required once mid-implementation.
  2. Run the generator tasks and `spotlessApply` as **separate gradle invocations**. In a single invocation
     (`./gradlew … :jmlparser-core-generators:run spotlessApply`) Gradle parallelizes and `spotlessApply` can
     format *before* the generators write, leaving unformatted output → false-positive full-tree drift.
- **Symbol resolution**: `JmlPredicateDeclarationContext.solveSymbol` maps formal parameters via
  `JavaParserSymbolDeclaration.parameter`, falls back to `super.solveSymbol` (enclosing class scope) so predicate
  bodies can reference fields; registered in `JavaParserFactory`.
- **Phase 7 test details**: wd delegation test compares `SExpr.toString()` (SList has reference equality);
  lsp document-symbol test asserts `Pred` as a *child* of the class symbol; code-action test passes a
  zero-width cursor inside the predicate body (production passes the user's cursor position).
- **smt / jml2java / `use_lemma` scripts / body type-checking**: explicitly out of scope (declare-only).
