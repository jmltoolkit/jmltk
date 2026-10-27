# Implementation Plan — quantarray (Design F, v4 scope)

Executable checklist for the design in `quantarray.md` (v4): `JmlQuantifiedVariable
extends VariableDeclarator` + single optional iterable binding; index+element
multi-binding deferred. Design F chosen over the alternatives for minimal ripple
(see `quantarray.md` §3.4).

Rules that apply throughout:

- every phase ends compile-green with the listed checks passing; commit per phase;
- never hand-edit generated files — run `./scripts/run-generators.sh` (its final
  `git diff --exit-code` **fails while your changes are uncommitted**; that is the
  drift check — commit the regenerated output and re-run to see it pass);
- use `./gradlew` (wrapper), commands from the repo root;
- grammar edits: `./gradlew :javaparser-core:compileJavacc` afterwards.

## Phase 1 — AST node

- [x] Create `javaparser-core/src/main/java/com/github/javaparser/ast/jml/expr/JmlQuantifiedVariable.java`:
  - `public class JmlQuantifiedVariable extends VariableDeclarator` (ast.body)
  - `@OptionalProperty private Expression iterable;`
  - accessors following the generated-property pattern:
    `notifyPropertyChange(ObservableProperty.ITERABLE, …)` + `setAsParentNodeOf(…)`
  - constructors mirroring `VariableDeclarator` plus iterable-carrying variants;
    mark the full one `@AllFieldsConstructor` — follow the
    `RecordDeclaration` precedent (subclass of a field-bearing node,
    `RecordDeclaration.java:122`; the generator validates the signature and will
    tell you if an inherited property is missing/extra)
  - Javadoc with syntax examples (AGENTS.md requirement), e.g.
    `(\forall int e : a; e > 0)`
- [x] Register `JmlQuantifiedVariable.class` in `MetaModelGenerator.ALL_NODE_CLASSES`
      (after `VariableDeclarator.class`, which sits at line ~100; the JML block at
      ~197–204 is a valid position — parents must precede children)
- [x] `./scripts/run-generators.sh` → expect: `JmlQuantifiedVariableMetaModel`
      (child of `VariableDeclaratorMetaModel`), `ObservableProperty.ITERABLE`,
      `visit(JmlQuantifiedVariable)` in all generated visitors,
      `isJmlQuantifiedVariable()` casting on `VariableDeclarator`
      (precedent: `Expression.isJmlQuantifiedExpr()`)
- [x] `./gradlew compileJava` green

  Notes from the run (divergences from the plan, all positive):
  - a **generator fix** was required first: `NodeMetaModelGenerator` only emits the
    protected 6-arg metamodel constructor for *abstract* nodes; `VariableDeclarator`
    is the first *concrete* node to gain a subclass, so `MetaModelGenerator` now
    computes the set of direct superclasses in `ALL_NODE_CLASSES` and passes
    `hasSubclasses` down (both generator files changed; `VariableDeclaratorMetaModel`
    gains the 6-arg constructor, `JmlQuantifiedVariableMetaModel` inherits it);
  - **no casting methods** are generated onto `VariableDeclarator`: the
    `TypeCastingGenerator` only targets the six base nodes
    (statement/expression/type/moduleDirective/bodyDeclaration/comment), so the
    design-doc prediction `VariableDeclarator.isJmlQuantifiedVariable()` does not
    hold — consumers use `instanceof JmlQuantifiedVariable` (already the design's
    operating rule: "instanceof only where iterables matter");
  - the printers were pulled forward: `visit(JmlQuantifiedVariable)` had to be added
    to `DefaultPrettyPrinterVisitor` (real per-binding implementation:
    `type name : iterable`; bound lists skip the common-type scheme) and
    `PrettyPrintVisitor` (legacy JML-less stub) to fix the post-generation compile
    (CONTRIBUTING step 3→4); `DefaultVisitorAdapter` (symbol-solver) got the
    throwing stub too;
  - `customInitialization()` is overridden to a no-op: the inherited observer kept
    `MAXIMUM_COMMON_TYPE` of the parent `NodeWithVariables` in sync (irrelevant for
    iterable-bound bindings) and would have been registered twice since both
    constructor chains call `customInitialization()`;
  - environment note: the build must run on **JDK 21** (CI's version); on JDK 27 the
    palantir-java-format lint inside `spotlessApply` fails
    (`JCCompilationUnit.endPositions` NoSuchFieldError). Use
    `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (provisioned
    toolchain) for the generator/spotless/build commands.

## Phase 2 — grammar

- [x] `javaparser-core/src/main/javacc/java.jj`, quantifier alternative in
      `JmlPrimaryPrefix()` (~4725):
      ```java
      vars=QuantifiedVariables()
      [ ":" iterable=Expression()
        { if (vars.size() != 1) throw new ParseException(
              "an iterable domain binds exactly one variable; nest quantifiers for more");
          VariableDeclarator d = vars.get(0);
          Type t = d.getType();
          if (t instanceof ClassOrInterfaceType && ((ClassOrInterfaceType) t).getNameAsString().equals("var")
                  && !((ClassOrInterfaceType) t).getTypeArguments().isPresent() && t.getArrayLevel() == 0) {
              t = new VarType(t.getTokenRange().orElse(null));
          }
          vars = new NodeList<>(new JmlQuantifiedVariable(t, d.getName(), iterable)); } ]
      ";" expr=Expression() …
      ```
  - `JmlQuantifiedExpr` itself is **not changed** (iterable lives on the binding)
  - `var` type: normalize `ClassOrInterfaceType("var")` (no args/dims) to `VarType`
    in the same action (uses `getTokenRange()`, not `getRange()` — the latter is
    the source-position `Range`, not the trace `TokenRange`)
- [x] No new tokens → `TokenTypes` untouched; no lexer-rule reordering
- [x] `./gradlew :jmlparser-core:compileJavacc` (0 errors); no new LOOKAHEAD
      conflicts near the quantifier
- [x] Smoke test (jshell against `javaparser-core/build/classes/java/main`):
      - classic `(\forall int i; 0 <= i < 5; a[i])` unchanged (`VariableDeclarator`)
      - `(\forall int e : a; e > 0)` parses → `JmlQuantifiedVariable`, iterable `a`;
        `(\forall var e : a; e != null)` parses → type is `VarType`
      - `(\forall int i, j : 0..n; …)` → ParseException with the fix-it message
      - `(\forall int i : 0..n; a[i])` → iterable `0 .. n` (range kept)

  Divergent from the plan: `VarValidator` (Java11 language level) rejected `var` in
  the quantifier binding (`"var" is not allowed here.`, since a `JmlQuantifiedVariable`
  is not a `VariableDeclarationExpr`). Added a targeted exemption: `var` is legal in a
  `JmlQuantifiedVariable` (the concrete type checks arrive with the symbol-solver
  phases).

## Phase 3 — printing

Facts to respect: `ConcreteSyntaxModel`'s `VariableDeclarator` entry (~389) prints
only `name` (+ initializer) — the enclosing node prints the type once via
`MAXIMUM_COMMON_TYPE` (JmlQuantifiedExpr entry, ~1322). A bound binding must print
its own type, so the "print common type once" scheme must be bypassed for bound
lists.

- [x] `DefaultPrettyPrinterVisitor.visit(JmlQuantifiedExpr)` (~914): branch — if
      `variables` contains a `JmlQuantifiedVariable`, skip the common-type print and
      let each binding print itself (`printList` already dispatches per node); else
      unchanged classic path
- [x] New `DefaultPrettyPrinterVisitor.visit(JmlQuantifiedVariable)`:
      `type` + space + `name` + (if iterable) `" : "` + iterable
      (note: unlike `visit(VariableDeclarator)` at :672, the binding prints its type)
- [x] `ConcreteSyntaxModel`:
      - new entry for `JmlQuantifiedVariable`: `child(TYPE), space(), child(NAME),
        conditional(ITERABLE, IS_PRESENT, sequence(space(), token(COLON), space(), child(ITERABLE)))`
        (spaces round the colon to match the printer's `" : "`)
      - `JmlQuantifiedExpr` entry (~1322): the `MAXIMUM_COMMON_TYPE` child is wrapped
        in `conditional(…, IS_PRESENT, …)` — for bound lists the derived
        `getMaximumCommonType()` override in `JmlQuantifiedExpr` reports
        `Optional.empty()`, so the common-type print is suppressed and no double
        space remains
- [x] Known API wart (fix if trivial, else document): `NodeWithType<VariableDeclarator, Type>`
      fixes the self-type — `setType` on a `JmlQuantifiedVariable` returns
      `VariableDeclarator`. **Documented, not fixed**: the same wart already exists
      for the inherited `setInitializer`/`setName` (interface N is fixed at
      `VariableDeclarator`); fixing it means changing `NodeWithType`'s self-type to
      a second type parameter, which the upstream uses everywhere as the only
      parameter. Consumers use the generated `setType` via the `VariableDeclarator`
      reference and rely on `instanceof JmlQuantifiedVariable` for the iterable part.

  Note: done as part of the Phase 1 commit; the CSM work was pulled forward and
  committed separately (`ConcreteSyntaxModelTest` demands a CSM entry for every
  metamodel node).

## Phase 4 — validation (syntactic, M1 scope)

Hook: `JmlProcessor.JmlReplaceVisitor` already carries a `ProblemReporter`
(`jml/JmlProcessor.java:83–97`). Add `visit(JmlQuantifiedExpr)` /
`visit(JmlQuantifiedVariable)` checks:

- [ ] bound binding has no initializer
- [ ] the iterable does not reference the bound variable (SimpleName scan —
      self-index sugar is deferred; `(\forall int e : a[e]; …)` is an error)
- [ ] multi-declarator + iterable cannot occur (parse-level), keep a defensive
      validator anyway
- [ ] type-based kind checks (range → integer, array → component, Iterable →
      element) come with M2

## Phase 5 — M1 test suite

- [x] `jmlparser-jml-tests/src/test/resources/jml-exprs.txt` — added (parsed by
      `JmlExpressionTest` automatically):
      ```text
      (\forall int e : a; e > 0)
      (\forall var e : a; e != null)
      (\exists T e : c; e.isEmpty())
      (\sum int e : a; e * e)
      (\num_of String s : words; s.length() > 3)
      (\min int e : a; e)
      (\forall int e : a; e != null; e.id > 0)
      (\forall int i : 0..a.length-1; a[i] <= a[i+1])
      (\forall int[] row : m; row.length == n)
      (\forall int e : a; (\exists int f : a; f.equals(e)))
      ```
- [x] `src/test/resources/csm_jml_expression.txt` — same lines for the CSM
      round-trip (`ConcreteSyntaxModelTest.printQuantifiedExpr`). The range
      example is spaced to the CSM canonical form
      (`0 .. a.length - 1`, `a[i] <= a[i + 1]`); the other nine round-trip
      verbatim. 19/19 CSM round-trips pass.
- [x] `LexicalPreservingPrinterTest` (jmlparser-jml-tests,
      `com/github/jml/printer/`) — round-trips a contract containing both
      `(\forall int e : a; …)` and a range-domain quantifier byte-for-byte.
- [x] Negative tests in a new `QuantifiedExprValidationTest`
      (parser with `setProcessJml(true)`, since the checks live in
      `JmlProcessor`): `(\forall int i, j : 0..n; …)` (parse-level),
      `(\forall int e : a[e]; …)` and `(\forall int e = 3 : a; …)`
      (validator-level); plus a positive control asserting no problems on
      valid forms.
- [x] One new `FragmentTest` corpus fragment
      (`fragments/decl_quantarray_iterable.txt`) derived from a real contract,
      plus the source `fullexamples/tested/QuantArray.java` which is picked up
      by `FullExamplesTest` and parsed through the whole `processJml=true`
      pipeline.
- [x] `./gradlew :jmlparser-jml-tests:test spotlessApply` green
      (16 JmlExpressionTest, 4 QuantifiedExprValidationTest, 19 CSM,
      1 LPP, fragments + full examples all pass).

## Phase 6 — M2 resolution & type inference

- [ ] `JmlQuantifiedExprContext.solveSymbol` (symbol-solver-core,
      `contexts/jml/JmlQuantifiedExprContext.java:34–40`) — the classic loop keeps
      working; add type inference for bound bindings in
      `JmlJavaParserQuantifiedVariableDeclaration` (declared `VarType` or kind
      demands it):
      - `BinaryExpr(Operator.RANGE)` → `int`
      - resolved iterable type `isArray` → component type (element of `T[][]` is `T[]`)
      - assignable to `Iterable` → first type argument
      - else → declared type / report problem
- [ ] Resolution expectation tests: extend the
      `jmlparser-jml-tests/.../resolution/JmlQuantifiedExprResolutionTest.java`
      pattern (`//?` annotations) with array / `Iterable` / `SequencedCollection` /
      range domains and shadowing
- [ ] `./gradlew :javaparser-symbol-solver-testing:test` green

## Phase 7 — M3 backends

- [ ] Desugarer: new `com.github.javaparser.jml.JmlQuantifierDesugarer` (core) —
      kinds 2 (range) / 3 (array) → classic `JmlQuantifiedExpr`
      (`(\forall int _i; 0 <= _i && _i < a.length; P[x := a[_i]])` or via
      `JmlLetExpr`); unit tests; used by the KeY path and the LSP quick-fix
- [ ] `tools/jml2java` `Jml2JavaTranslator.kt` (binder switch ~132, `visitForall`):
      bound binding → enhanced-for (`ForStmt` with
      `initialization = [VariableDeclarationExpr(declarator), iterable]`); range →
      index loop; `\sum`/`\num_of`/`\product` accumulate; update
      `tools/jml2java/src/test/resources/expr.yaml` + translator tests
- [ ] `tools/smt` `JmlExpr2Smt.kt` (~218): kinds 2/3 →
      `forall ((i Int)) (=> (and (<= 0 i) (< i len)) P[a[i]])`; kind 4 → clear
      "not supported yet" error; update `tools/smt/src/test/resources/expr.yaml`
- [ ] `tools/wd` `WDVisitor.kt` (~137): domain WD conditions (non-null, purity)
- [ ] `tools/stat` `Statistics.kt`/`StatVisitor.kt`/`JmlStatisticsReporter.kt`:
      count element quantifiers per binder
- [ ] `tools/lsp` `CodeActionCollector.kt`: no crash on the new node; optional
      quick-fix expanding to classic form via the desugarer
- [ ] Run each tool's test suite

## Phase 8 — M4 docs & release

- [ ] `jmltk-changelog.md`: Unreleased entry (fork-local SemVer; feature ⇒ minor
      bump — `gradle.properties` `version=1.2.0-SNAPSHOT` at merge, keep `-SNAPSHOT`)
- [ ] `doc/readme.md`: add the syntax to the JML showcase if one exists (do **not**
      touch the generated root `README.md`)
- [ ] Full corpus green: `FragmentTest`, `FullExamplesTest` (openjml/key/tested)
- [ ] Final QA: `./gradlew spotlessCheck checkstyleMain` and the AGENTS.md test
      commands for the touched modules

## Definition of done

- [ ] classic quantifier AST/print/round-trip byte-identical to `main` (regression
      guard: existing `jml-exprs.txt`, `csm_jml_expression.txt`, fragments, corpus)
- [ ] single-binding element quantifiers parse, print, round-trip (default printer,
      CSM, LexicalPreservingPrinter)
- [ ] name/type resolution with inferred element types
- [ ] jml2java emits enhanced-for; smt encodes array/range domains; wd checks domain
      well-definedness; stat/lsp aware
- [ ] changelog + version bumped; all module test suites green
