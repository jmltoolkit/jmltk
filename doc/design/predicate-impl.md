# Implementation Plan — JML lemma declarations (`//@ instance lemma Pred(int i) = i > 0`)

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

- [ ] `git checkout -b weigl/predicate origin/main` (done: branch exists,
      based on `main` @ `50560208f`)
- [ ] `doc/design/predicate.md` — design + language surface (this phase)
- [ ] `doc/design/predicate-impl.md` — this file

## Phase 1 — AST node `JmlLemmaDeclaration`

- [ ] Create
      `javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/JmlLemmaDeclaration.java`
      (model file: `JmlRepresentsDeclaration.java`):
  - `public class JmlLemmaDeclaration extends JmlClassLevelDeclaration<JmlLemmaDeclaration>
       implements NodeWithName<…>, NodeWithModifiers<…>, NodeWithAccessModifiers<…>`
  - fields in this order: `NodeList<Modifier> modifiers`, `SimpleName name`,
    `NodeList<Parameter> parameters`, `Expression body`,
    `NodeList<SimpleName> jmlTags`
  - `@AllFieldsConstructor (jmlTags, modifiers, name, parameters, body)`,
    default constructor, main constructor `(TokenRange, jmlTags, modifiers,
    name, parameters, body)`, Javadoc with the syntax example
- [ ] Register `JmlLemmaDeclaration.class` in
      `MetaModelGenerator.ALL_NODE_CLASSES` (after `JmlClassAccessibleDeclaration`,
      line ~242)
- [ ] `./gradlew :jmlparser-core:compileJava` green (node compiles standalone)
- [ ] `./scripts/run-generators.sh` — regenerates
      `metamodel/JmlLemmaDeclarationMetaModel.java`, `JavaParserMetaModel`
      wiring, visitors (Generic/Void/Clone/Equals/Modifier/HashCode/Observer…),
      node methods (accept/property/remove/replace/clone/getMetaModel/fluent/typecast)
  - check: `git diff --stat` shows the new node + all generated files
- [ ] commit per phase

## Phase 2 — grammar & tokens

- [ ] `java.jj`: add `<LEMMA: "lemma">` to the
      `<JML_MULTI_CONTRACT,JML_SINGLE_CONTRACT> TOKEN` block (no reordering)
- [ ] `java.jj`: new production
      `JmlClassLevelDeclaration JmlClassLemma(ModifierHolder modifiers):` —
      `<LEMMA> id=SimpleName() parameters=Parameters() "=" expr=Expression() ";"`
      building `new JmlLemmaDeclaration(range(begin, token()), new NodeList<>(),
      modifiers.modifiers, id, parameters.a, expr)`, range begin via
      `orIfInvalid(modifiers.begin, begin)`
- [ ] `java.jj`: `JmlClassLevelInner` gains
      `| LOOKAHEAD(1) element = JmlClassLemma(modifiers) { ret.add(element); }`
- [ ] `TokenTypes.java`: add `case LEMMA:` to `getCategory`
- [ ] `./gradlew :javaparser-core:compileJavacc` (regenerates JavaToken kinds +
      GeneratedJavaParserConstants + parser)
- [ ] `./gradlew :javaparser-core:compileJava` green
- [ ] smoke parse in jshell/`JavaParser`: `//@ instance lemma Pred(int i) = i > 0;`
      in a class body yields `JmlLemmaDeclaration` member with
      `parameters = [int i]`, `body = i > 0`, `modifiers = [instance]`
- [ ] commit per phase

## Phase 3 — CSM & pretty printing

- [ ] `ConcreteSyntaxModel.java`: entry for `JmlLemmaDeclaration.class`
  `sequence(child(MODIFIERS), space(), token(GeneratedJavaParserConstants.LEMMA),
  space(), child(NAME), token(LPAREN), list(PARAMETERS, sequence(comma(), space())),
  token(RPAREN), space(), token(ASSIGN), space(), child(BODY), semicolon())`
- [ ] `DefaultPrettyPrinterVisitor`: `visit(JmlLemmaDeclaration, Void)` inside
      `wrapInJmlIfNeeded(...)` printing modifiers, `lemma `, name,
      `(params) = body;`
  (mirrors `visit(JmlClassExprDeclaration)`)
- [ ] round-trip: `toString()` of the parsed declaration reproduces the
      canonical `instance lemma Pred(int i) = i > 0;`
- [ ] `./gradlew :javaparser-core:test :jmlparser-core-testing:test` green
- [ ] commit per phase

## Phase 4 — validation

- [ ] `ModifierValidator.visit(JmlLemmaDeclaration, ProblemReporter)`:
  - allowed modifiers: `PUBLIC, PROTECTED, PRIVATE, STATIC, FINAL, JML_MODEL,
    JML_INSTANCE, JML_NULLABLE…, math modes`;
  - parameter modifiers: `FINAL, JML_NULLABLE…`;
  - duplicate formal parameter names → `reporter.report(...)`
- [ ] tests: `//@ instance lemma Pred(int i) = i > 0;` parses clean;
      `//@ static lemma P(int i, int i) = ...` reports duplicate; disallowed
      modifier reported
- [ ] `./gradlew :jmlparser-core:test :jmlparser-jml-tests:test` green
- [ ] commit per phase

## Phase 5 — M1 tests

- [ ] `src/test/resources/fragments/decl_jml_lemma_pred.txt` (+ instance + var
      forms) — picked up by `FragmentTest.testClassLevel`
- [ ] `src/test/resources/fullexamples/tested/Predicates.java` with the
      motivating example (covered by `FullExamplesTest` with `processJml=true`)
- [ ] `jmlparser-jml-tests/.../parser/LemmaDeclarationTest.java`:
  - parse a full class with `processJml=true`; assert the `JmlLemmaDeclaration`
    member (name, modifiers incl. `instance`, parameter list, body AST, ranges)
  - CSM round-trip via `ConcreteSyntaxModel.genericPrettyPrint`
  - pretty-printer round-trip via `toString()`
  - lexical-preserving printer round-trip
- [ ] `./gradlew :jmlparser-jml-tests:test` green
- [ ] commit per phase

## Phase 6 — symbol resolution

- [ ] `JmlLemmaDeclarationContext` in
      `javaparser-symbol-solver-core/.../contexts/jml/`: `solveSymbol(name)`
      over `getParameters()` → `JavaParserSymbolDeclaration.parameter`,
      fallback `super.solveSymbol`
- [ ] `JavaParserFactory`: register
      `if (node instanceof JmlLemmaDeclaration) return new JmlLemmaDeclarationContext(...)`
- [ ] `DefaultVisitorAdapter`: `visit(JmlLemmaDeclaration, Boolean)` throwing
      `UnsupportedOperationException` stub (matches sibling declarations)
- [ ] resolution test resource
      `src/test/resources/com/github/jml/resolution/JmlLemmaResolutionTest.java`
      with `//?` expectations; `NameResolutionTest` gains `jmlLemma()` test
- [ ] `./gradlew :jmlparser-symbol-solver-testing:test :jmlparser-jml-tests:test` green
- [ ] commit per phase

## Phase 7 — M3 backends

- [ ] **stat**: `StatisticKey` + `totalLemmas` counter in
      `JmlStatisticsReporter.visit(JmlLemmaDeclaration)`; `StatVisitor`
      `<lemma name="…">` element with modifier attrs + body expr stats;
      `JmlStatisticsReporterTest` case
- [ ] **wd**: `WDVisitor.kt` `visit(JmlLemmaDeclaration): SExpr = wd(nullable body)`
      + wd fixture (well-defined lemma body; not-wd body)
- [ ] **lsp**: `JmlCatchSymbols` lemma `DocumentSymbol` (kind `Method`);
      `CodeActionCollector` well-definedness check on lemma body
- [ ] module tests green for all touched tools
- [ ] commit per phase

## Phase 8 — changelog & version

- [ ] `gradle.properties`: `version=1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT`
      (feature ⇒ minor bump, fork-local SemVer per `AGENTS.md`)
- [ ] `jmltk-changelog.md`: add `### Version 1.2.0-SNAPSHOT (unreleased)` section
      (Added: lemma declarations) + note that 1.x is fork-local SemVer
- [ ] commit per phase

## Phase 9 — QA & PR

- [ ] full corpus green: `FragmentTest`, `FullExamplesTest`
- [ ] root `spotlessCheck` + `checkstyleMain` green (pre-existing warnings only)
- [ ] `./scripts/run-generators.sh` drift check passes (committed regenerated state)
- [ ] all touched module suites re-run green
- [ ] `git push -u origin weigl/predicate`
- [ ] `gh pr create --base main --head weigl/predicate` with phase summary
