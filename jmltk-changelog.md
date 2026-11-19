# JMLtk — J/K Release Changelog

Complementary to [`changelog.md`](./changelog.md), which documents the upstream JavaParser releases.

This file documents the fork releases that carry the **J** (JML) and **K** (KeY) feature sets, reconstructed
from the git history of the two forks that were merged into this repository:

- **KeY fork** — [wadoon/key-javaparser](https://github.com/wadoon/key-javaparser): releases `K1` … `K13.6`.
- **JML fork** — [jmltoolkit/jmltk](https://github.com/jmltoolkit/jmltk) (formerly `wadoon/jmlparser`): releases
  `-b2` … `-b7` before the merge, then the combined `-J<JML>-K<KeY>` scheme.

Commit hashes are given for every entry so that each claim can be verified with `git show <hash>` / `git log`.
Hashes that refer to a feature branch rather than to `main` are marked as such. A few historical hashes
(`82b6d7869`, `b66825037`, `0b2a7a359`, `183af569e`, `3da7c7825`) come from pre-merge fork histories and are
kept for provenance; they are not reachable from `main`, so `git show` only works if that history is present
locally.

## Version scheme

Fork-local SemVer (`MAJOR.MINOR.PATCH`), independent of the upstream JavaParser version. Source of truth:
`version=` in `gradle.properties`. Bump rules: patch = bugfix, minor = feature/upstream merge, major = breaking
change; keep `-SNAPSHOT` between releases. Version `1.0.0` (2026-10-25) was the first SemVer release; it
corresponds to the composite `3.28.2-J8.0-K13.6` below. `main` carries the next minor (`1.2.0-SNAPSHOT`); the
`1.0.x` maintenance line is released from `releases/JMLtk-v1.0.x` branches.

Up to `1.0.0` the fork used the composite scheme `<JavaParser>-J<JML>-K<KeY>`:

- **J** counts the JML (Java Modeling Language) feature level — JML AST nodes (`ast/jml/**`), JML grammar, JMLDoc,
  contracts, stipulations (`\by` scripts), etc. Built in the JML fork since 2022.
- **K** counts the KeY feature level — KeY-specific AST nodes (`KeyCatchBranch`, `KeyMarkerStatement`,
  `ArrayLengthValueDeclaration`, `LogicalType`, …), ghost variables/modifiers, method-frame resolution, KeY-style
  semantics. Developed in the KeY fork (key-javaparser) since 2021.
- The combined scheme starts with `3.28.0-J7.0-K10.1-SNAPSHOT` (2026-02-03), when `key/master` was merged into
  the JML fork; the J count continued as J7.0 → J8.0, while the K count continued from K10.1 (KeY fork) through
  K13.6.

Release markers found in the history (tags): `k1`, `k2`, `k3`, `v3.25.3-K3`, `v3.25.3-K5`, `K11.1`,
`KEY-JAVAPARSER-3.28.0-K13.6`, `JMLtk-v1.0.0`. Old releases were published from `pom.xml` (Maven); since the
July 2026 Gradle migration the version lives in `gradle.properties`.

---

## Unreleased — 1.2.0-SNAPSHOT

This section covers all user-visible changes on `main` since `1.0.0`. The `1.1.0` line was never released, so
work done while `gradle.properties` said `1.1.0-SNAPSHOT` is recorded here as well. The generic KeY
meta-construct statement is additionally listed under `Unreleased — 1.0.1-SNAPSHOT`, because it is backported
to the `1.0.x` maintenance line.

Single-binding element quantification over arrays, ranges and `Iterable`/`SequencedCollection` (feature branch
`weigl/quantarray`, design `doc/design/quantarray.md`, implementation plan `doc/design/quantarray-impl.md`):
for-each style binder domains `(\forall int e : a; e > 0)`, `(\forall int i : 0..n; a[i] > 0)` and
`(\forall var x : list; x != null)`, uniformly for `\forall`, `\exists`, `\sum`, `\product` and `\num_of`.
Index+element multi-binding (`int i, T e : a[i]`) stays deferred. Feature ⇒ minor bump
(`1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT`). Merged to `main` as the squashed commit `a36ea22be` (PR #83); the
per-phase hashes below refer to the `weigl/quantarray` feature branch (`origin/weigl/quantarray`), not to
`main`.

**VCG (`tools/vcg`, branch `weigl/vcg`, PR #69; experimental)** — JML `\sum` generalized quantifiers with a
*bounded* integer range and a *direct array-read body* are modeled in the VCG as a lazily-declared recursive SMT
function (`(define-fun-rec jmlSum a lo hi …)`, unfolding along the index range with `hi <= lo` → `0`), so KeY's
firstTouch 08 for-each sum contract (`maintaining s == \sum int j; 0 <= j && j < \index; a[j]`) verifies in both
BOUNDED and UNBOUNDED mode; every other `\sum` shape is rejected with a structured `UnsupportedFeatureException`.
The whole tool is marked experimental: semantics are under active development, and unsupported constructs are
rejected rather than approximated.

**Added**

- `tools/bench`: new parse benchmark (`--corpus plain|jml|path:<dir>`, `--mode grammar|full`, `--baseline`, `--verify`) that separates grammar cost from validator cost, reports per-round medians and best-of-N, and verifies per-file AST fingerprints against committed baselines; four baselines live under `tools/bench/baseline/` (`da0b05cdb`, feature branch `perf/grammar-optimization`, not yet merged).
- `tools/prettyprinting`: new `jmltk-pp` CLI (also `jmltk pp`, `jmltk format`) that pretty-prints Java/JML from files or stdin and highlights it for terminals (256-color ANSI), HTML (Pygments class names), and LaTeX (minted `\PYG` body or semantic `\jml` macros); snippets are auto-detected and JML comments are highlighted (`f4365af91`, feature branch `weigl/prettyprint-cli`, PR #101).
- AST: `JmlQuantifiedVariable extends VariableDeclarator` with one optional iterable/range domain per binding,
  including metamodel/CSM/printer support (`cad7b3e33`; printers/CSM `63929f86f`).
- Grammar (Phase 2, `8bedf4eeb`): an iterable domain binds exactly one variable — multi-binding domains are
  rejected with a fix-it message; `var` in a binding is normalized to `VarType` and accepted (validator
  exemption in `8bedf4eeb`).
- Validation (Phase 4): the iterable must be pure and must not reference the bound variable; binding-kind
  checks for ranges, arrays and `Iterable` generators (`cbb0656a5`).
- Type inference (Phase 6): the static type of a bound variable is the element type of its domain — component
  type for arrays, first `Iterable<T>` type argument, `int` for ranges (`46f413f44`, Phase 6).
- Backends (Phase 7):
  - core `JmlQuantifierDesugarer` rewriting range/array domains into the classic index form, used by the KeY
    path and the LSP quick-fix (`2b5a6cfcd`);
  - jml2java: enhanced-for (array/`Iterable`) and index-loop (range) translations with `\sum`/`\product`/
    `\num_of` accumulation (`2b5a6cfcd`);
  - smt: `forall` encodings for range/array domains, loud "not supported yet" for `Iterable`; also fixes
    `SmtTermFactory.list(List<SExpr>)` dropping quantifier binder variables (`cada36591`);
  - wd: domain well-definedness (non-null, purity, evaluated once) for element domains (`da286b21c`);
  - stat: `totalElementQuantifiers` plus per-binder `BINDER_ELEMENT_*` counters (`9c1dcaf1d`);
  - lsp: "Expand quantifier domain" quick-fix, no crash on kind 4 (`f8f156aec`).
- Tests & corpus: `jmlparser-jml-tests` M1 suite (`28e1e7afc`), desugarer unit tests, the `QuantArray.java`
  corpus fixture exercised by `FullExamplesTest`, and the ghost-array example `GhostArrayQuantifier.java`
  (`879cff35c`; all parser/backend suites green). Review/formatting follow-ups: `710ccbbe8`, `40912bfdf`,
  `ddb240755`.
- Proof scripts on JML formatted statements (`//@ assert … \by { … };`, also `assume`/`set`): the grammar now
  accepts an optional `\by` block after the expression, ported from KeY's
  `assertionProof`/`proofCmd`/`proofCmdCase`/`proofArg`/`proofCmdSuffix` productions (`050050976`, PR #77).
  Adds the `FROM_GOAL` token (`\from_goal`); `obtain` remains contextual. The proof-script AST was reworked into
  a `JmlProofStmt` hierarchy — `JmlProofCmdStmt`, `JmlProofCaseStmt`, `JmlProofObtainStmt` (kinds `=`,
  `\such_that`, `\from_goal`) and `JmlProofArg`; the old `JmlProofScript`/`JmlProofCmd`/`JmlProofCmdCase`
  names and `Node.associatedSpecificationComments` are gone. Printers, visitors, metamodel and symbol-solver
  support are updated.
- JML math-mode modifiers: KeY's `spec_bigint_math` / `spec_java_math` / `spec_safe_math` pragmas now parse
  beside the existing `code_*_math` modifiers, as class/type/method/field/constructor modifiers
  (`Modifier(JML_SPEC_*_MATH)`; `ModifierValidator` accepts all six math keywords) (`4e3d2af0b`, PR #78).
  Type-level pragmas are attached to the following type declaration by `JmlProcessor`. Also fixes
  `JmlDocSanitizer`: the `@` of a single-line `@*/` closing is blanked unconditionally, so single-line JML
  comments on line 1 (or on the same line as a declaration) no longer leave a stray `@`.
- KeY label-after-keyword JML statements: `JmlExpressionStmt` gained an optional `label` (`SimpleName`), so
  KeY's formatted `assert LAB: e;` / `set SN: x = …;` parse and round-trip next to the JML-standard prefix
  form (`JmlLabelStmt`); grammar, CSM and pretty printer updated, regression tests in `JmlLabeledAssertTest`
  (`42eded62c`, PR #86).
- Lint: rule registration re-enabled and seven new JML lint rules added — `AssignableLocsetValidator`,
  `DefaultNullityValidator`, `ExceptionVarCheck`, `JmlDuplicateNamesValidator`, `SignalsExceptionTypeCheck`,
  `SpecVisibilityValidator`, `TrivialFormulaValidator` — plus KSP-based export of rule documentation via the
  new `tools/build-helpers` processor module (`50560208f`, PR #68); the rules are exposed through the LSP.
  Fixes the missing KSP provider service file (the `build-*` ignore pattern hid `build-helpers`), rule
  initialization before use, and `DefaultNullityValidator` reading its config before initialization.
- **Experimental** — `//@ lemma Name(params) \by { … };` proof-command lemmas as class-body declarations (only
  KeY's `assertionProof` script form — a definitional `lemma … = expr;` or `assert …` form is deliberately
  **not** part of this port, `a193b16ad`): `JmlLemmaDeclaration` AST node (`cafecb694`, Phase 1), `JavaToken`
  kind renumber for the `LEMMA` token (`963e22c3e`), grammar incl. the mandatory `\by` script and the
  `use_lemma` proof command following the `obtain` pattern (`2f7a5dbba`, Phase 2),
  `ConcreteSyntaxModel`/pretty-printer entries (`3a0e365f0`, Phase 3; visitor methods restored in
  `4c183a33a`), language-level validation incl.
  lemma modifier rules (`b5c6f61ec`, Phase 4), M1 parser/printer/lexical-preserving-printer test corpus
  with the VerifyThis 2025.1 MinExcludant0 miniature (`0a2b5207a`, Phase 5), symbol-solver resolution of
  lemma parameters from inside the lemma body and its `\by` script — `\seq`/`\bigint` parameter names
  resolve and their types fail cleanly instead of crashing (`14ba7bd97`, Phase 6), and M3 tool backends:
  `wd`/`jml2java`/`smt` no-op visits, `stat` `totalLemmas`/`LEMMA_<name>`/`useLemmaCommands` counters
  plus `<jml-lemma>` XML elements, `lsp` document symbols and expression well-definedness code actions
  (`830d1346d`, Phase 7).
- Design & implementation plan: `doc/design/lemmata-impl.md` (`96d4bd0b6`, Phase 0).
- AST + grammar: `JmlMergePointStmt` for JML state-merging points, e.g.
  `merge_point \by "MergeByPredicateAbstraction" \lattice conjunctive \predicate (int x) : x >= 0, x == \old(b) || x == -\old(b);`
  — all of `\by`, `\lattice` and `\predicate` are optional and must appear in this fixed order. The values of
  `\by`/`\lattice` may be given as a string literal or as a plain identifier; both forms are normalized to a
  `StringLiteralExpr` in the AST, exposed as the `by`, `lattice`, `predicateVariable` and `predicates` properties
  on `JmlMergePointStmt`. Includes metamodel, CSM, visitor and pretty-printer support, plus parser tests
  (`5753c8615`; the option `\using` was renamed to `\by` in `ee02b030a`/`105b2c45b`; PR #90).
- Examples: unblocked `key/firstTouch/11-StateMerging/A.java` (bare `merge_point;`) and rewrote
  `key/firstTouch/11-StateMerging/Gcd.java` from the legacy KeY `merge_proc`/`merge_params` syntax to the new
  `merge_point` options in `FullExamplesTest`.
- **Experimental** — vcg: `\sum` encoding — range-bound checks, array-read-body validation (`KeySumBadShapes`),
  and the BOUNDED length fold (`bv$length sym` → shared implicit `$len_<sym>` constant plus one ground `>= 0`
  assert) that makes the recursive-function queries terminate under Z3; KeY loop-position `\index` binds the
  type-tagged `$idxN` loop-counter register; `\strictly_nothing` joins the assignable whitelist (`e8c120ab9`).
  The element/range-domain bindings introduced with #83 (`\forall int e : a; …`, `\sum int e : a; …`) are
  rejected loudly — no desugaring exists, and expanding the body over the whole index type would be unsound.
- cli: `jmltk vcg` end-to-end subprocess tests and shell fixtures for the four KeY firstTouch examples
  (05-ReverseArray honest UNKNOWN/FAIL, 06-BinarySearch and 10-SITA proven via loop invariants, 08-Java5 proven
  in both modes); the shell tester gained a `sortOutput` comparison mode (`d0ee30440`).

**Deferred**

- Method-level `use_lemma` statements (`//@ use_lemma …;` outside a `\by {…}` proof script); lemma
  declarations as resolvable symbols — see `doc/design/lemmata-impl.md` → "Deferred / open".

**Changed**

- CI: the test matrix runs on macOS and Windows again (`7605d114d`, PR #18). The Windows combination had been
  disabled in PR #102 after its jobs hung in the Gradle test step and the cache-save post steps failed; the
  cause was repository cache pressure, not VCG/SMT: the repository held 40 Actions cache entries totalling about
  10.4 GiB, above GitHub's 10 GiB limit, so cache saves raced and were evicted. The VCG/SMT tests pass on
  Windows, and after clearing all caches the previously failing job completed in 15 minutes on a cold cache.
  Windows is re-enabled (feature branch `ci/re-enable-windows-tests`, commit `1948b08d3`). Pushes to
  `releases/*` branches run the test and formatting checks (`87f7d1329`, PR #96).

**Fixed**

- CLI/LSP: `jmltk` crashed on JDK 21 as soon as it ran on a real terminal, with
  `ClassCastException: Cannot cast java.lang.Integer to java.lang.foreign.MemorySegment` at
  `mordant...ffm.FfmLayoutsKt.varHandle`. Clikt 5.1.0 pulls Mordant 3.0.2, whose FFM terminal backend calls
  `MemoryLayout.varHandle` with the JDK 22 signature (an extra `long` offset coordinate); the CLI's
  `--enable-native-access` makes Mordant select that backend on JDK 21, where the preview FFM API yields a
  different coordinate type. Mordant is now constrained to 3.1.1, which skips the FFM provider below Java 22
  and falls back to JNA (feature branch `weigl/prettyprint-cli`, commit `db7d5d10b`, PR #101).
- Build: `:dokkaGeneratePublicationHtml` failed with `Unexpected classifier: "#"`, breaking the Weekly Builds
  job since the `tools/vcg` module was merged. Dokka treats every module's `README.md` as module documentation
  and splits it at any `#` followed by `Module` or `Package`, so the vcg README's `## Module layout` heading was
  parsed as a second module-doc fragment. The README now starts with Dokka's required `# Module vcg` header and
  the section is named `## Source layout` (feature branch `fix/dokka-vcg-module-docs`, commit `81f34fcfe`).
- Parser: JML clauses now keep **all** heap specifiers instead of only the last one. `JmlHeaps()` returned
  a single-element list, and both `JmlAccessibleClause` and `JmlMultiExprClauses` looped with
  `(heaps=JmlHeaps())*`, reassigning the list on every iteration: `assignable<heap><savedHeap> a[0..3];`
  parsed as `heaps = [savedHeap]` and the `<heap>` specifier was silently dropped. KeY then saw no assignable
  clause for the base heap and `JMLSpecFactory.translateAssignable` widened the frame to `\everything`, which
  made the JavaCard proofs `setArray1`, `setArray2` and `arrayFillNonAtomic` fail to close (the anonymisation
  set included unrelated locations, so `isTransient(a)` and the transaction-depth side conditions could not
  be discharged). `JmlHeaps()` now consumes every consecutive `<heap>` specifier into one list, so all call
  sites use a single `(heaps=JmlHeaps())`. The two dropped-heap TODOs are fixed as well: `loop_invariant`
  heaps are passed to the `JmlSimpleExprClause`, and class-level `accessible` declarations store them in a new
  `heaps` property of `JmlClassAccessibleDeclaration` (metamodel, visitors and clone code regenerated). The
  pretty printer now renders each specifier in angle brackets (`JmlMultiExprClause` used to concatenate the
  names). Regression test `MultiHeapClauseTest` covers `assignable`, `accessible`, `loop_invariant`, the
  class-level declaration, printing and the single-heap case (`9c4f5235c`, feature branch
  `fix/jml-multi-heaps-append`, PR #102).
- Parser: `assignable_free` (the free counterpart of `assignable`, used by KeY) is now a clause keyword:
  it parses into a `JmlMultiExprClause` with `JmlClauseKind.ASSIGNABLE_FREE`, in method and loop contracts
  alike (`ba2c0f3d3`, PR #100). It previously fell through the clause parser, and the leftover
  `assignable_free field;` matched the class-level JML fallback as a `FieldDeclaration`, inserting a bogus
  field of that type into the class body (KeY's converter failed with `UnsolvedSymbolException`); loop
  contracts rejected it loudly. The new lexer token shifts the numeric `JavaToken.Kind` values after
  `ASSIGNABLE` by one (`JavaToken` regenerated).
- KeY proof scripts inside method bodies: a single-command `\by cmd;` form no longer needs a second `;`;
  nested `rule "…" \by { case … }` scripts are no longer swallowed by the proof-command argument list
  (`\by` is a valid identifier token, so it used to be taken as an argument); nested
  `assert … \by auto;` commands inside a proof block parse (the `assert` keyword is lexed as
  `JML_ASSERT`); and the Java ≥ 1.4 `assert`-identifier validation exempts proof-command names. This makes
  KeY's `heap/quicksort/Quicksort.java` parse again — the ghost local grouped with its following `\by`
  script is now a `JmlGhostStmt` → `ExpressionStmt` → `VariableDeclarationExpr` instead of being reported
  as a misplaced class-level `JmlFieldDeclaration` (`java.jj`, `Java1_0Validator`,
  `ModelDeclarationInMethodTest`; feature branch `weigl/fixjmltkkey`, pre-merge commit `8f9ab4462`, PR #99).
- Model methods, ghost methods, model/ghost fields (and other class-level JML declarations) placed *inside
  a method body* are now rejected with a targeted problem — e.g. `Model method modelMethod is not allowed
  within methods.` / `Ghost method ghostMethod is not allowed within methods.` / `Model field modelField
  is not allowed within methods.` — instead of the raw method-level grammar error ("Encountered unexpected
  token …") plus the generic `Could not handle the JML comment.` fallback (`JmlProcessor`; parser tests in
  `ModelDeclarationInMethodTest`, commit `7332d0126`). Local ghost variables (`/*@ ghost int local; */`),
  which JML deliberately allows inside methods, keep parsing.
- Symbol resolution: interface methods with no explicit access modifier are public, not package-private, so
  they are no longer discarded when called from another package (`MethodResolutionLogic`, `f680c8f6f`, PR #92).
- cli: Windows `jmltk` launcher loaded the wrong `ParserConfiguration` — the distribution shipped two
  `com.github.javaparser` implementations, the fork core and KeY's proofjava fork
  (`org.key-project.proofjava:javaparser-core:3.28.0-K13.6`, pulled in by the LSP's `key.core`). The Windows
  start script must use the `%APP_HOME%\lib\*` classpath wildcard (cmd.exe cannot hold an explicit 100+-jar
  classpath: `%APP_HOME%` expands to absolute paths and any single `set CLASSPATH=` line exceeds the
  8191-character limit), and Java expands that wildcard in directory-enumeration order, so Windows could load
  KeY's `ParserConfiguration` first and `jmltk vcg` crashed with `NoSuchMethodError` on
  `ParserConfiguration.setProcessJml(boolean)`. The CLI runtime now excludes the proofjava fork, so the
  distribution carries exactly one `com.github.javaparser` implementation (the fork core) and directory order
  cannot matter (`63aaf0907`).

**Removed**

- AST + grammar: `KeyMergePointStmt` (KeY ProofJava merge points, `merge_point ( <expr> );`; introduced in
  1.0.0, `d71ad6e6c`) — the `KeyMergePointStatement()` grammar rule, its `ConcreteSyntaxModel` and
  pretty-printer entries, all generated visitor/metamodel support, and the `isKeyMergePointStmt` /
  `asKeyMergePointStmt` / `toKeyMergePointStmt` / `ifKeyMergePointStmt` type-casting methods on `Statement`
  are gone. The `MERGE_POINT` lexer token is gone as well: `merge_point` is no longer a reserved word and
  is recognized as a JML merge point contextually — the same identifier-by-image pattern as `obtain` and
  `use_lemma` — only when directly followed by `;` or one of the `\by` / `\lattice` / `\predicate` options,
  in both the `merge_point` and the KeY `$mergePoint` spelling (PR #97, `b8136e914` + `9a5e1ed85`). The KeY
  ProofJava form `merge_point ( expr );` therefore no longer parses as a merge point at all — with a
  following `(` it is an ordinary method invocation (`ExpressionStmt` of a `MethodCallExpr`) — and
  `merge_point` can be used as a plain identifier (fields, local variables, method names). **Breaking**:
  the AST class and the `JavaToken.MERGE_POINT` kind are gone, and the numeric `JavaToken` kinds of all
  subsequent token types are shifted down by one.

### Language Server Protocol & keyword-node AST (PR #61, `weigl/lsp`)

Makes JML keywords first-class AST nodes and reworks the jmltk LSP server (code actions, document symbols,
server plumbing). Rebased onto `main` after PR #69 (`7db10d373`); commit hashes below refer to the rebased `weigl/lsp` branch.

**Added**

- JML keywords are now AST nodes: abstract `JmlKeywordNode` (implements `Jmlish`) with a `JmlKeyword`
  interface (`jmlSymbol()`/`asString()`), and three keyword nodes — `JmlContractBehavior` (a contract's
  behavior keyword, incl. British spellings and `feasible_behavior` → `behavior`), `JmlClauseKind`
  (wrapping `JmlClauseKeyword`, e.g. `requires`/`ensures`/`assignable`/`signals`/`forall`) and
  `JmlBodyClauseKind` (wrapping `JmlBodyClauseKeyword`, e.g. `invariant`/`constraint`/`axiom`/`initially`).
  Clause/body-clause keywords are now child nodes with a token range (`3fe1e049e`, `059960182`).
- Grammar (`java.jj`): behavior and clause productions now build the keyword nodes; loop/model contracts pass
  `null` behavior where none is present.
- LSP code actions reworked to a common `LspAction` interface discovered via `ServiceLoader`:
  `AddBehaviorKeyword`, `AddNameToClauseOrContract`, `AddQuantification`, `AddBoundary` +
  `ExpandQuantifierDomain` (via `JmlQuantifierDesugarer`), `AddStubs` code lens; reworked
  `WellDefinednessCheck`, `VerifyAgainstParent`, `CreateKeyProjectFile`; `StartKey` removed (`2a6964969`,
  `4060e3764`, `0bf519ce9`).
- Document symbols: `JmlCatchSymbols` (contracts, clauses, JML fields/represents/accessible declarations) and
  `KeyCatchSymbols` (function declarations, predicates, schema variables, taclets).
- Server/launcher: custom `Launcher` construction (executor service, message validation, new
  `--trace [-|file]`, `tcpNoDelay` + `keepAlive`), semantic tokens for `**/*.{jml,java}` and `**/*.key`,
  expanded docs (`doc.md`, new `sol.md`), `eglot.el`.
- Tests: `VariableDeclarationErrorPositionTest`, `CodeActionsUnitTest`, `JmlCatchSymbolsTest`,
  `KeyCatchSymbolsTest`, `CodeActionCollectorTest`, `ElementQuantifierCodeActionTest`, `JmlClauseKeywordTest`;
  LSP suite grows 17 → 53 tests (instruction coverage 39.6% → 61.6%) (`2a6964969`).
- Utility helpers in `tools/utils` (`JMLUtils`), `stat` counters, `lint` rule package move, `redux`
  `RecordClassBuilder`, `jmlstub` `JREClassStubGenerator` updated for the keyword nodes.

**Changed**

- *Breaking* (downstream, in particular KeY): `com.github.javaparser.ast.stmt.Behavior` is removed;
  `JmlContract` now stores a `JmlContractBehavior` node or `null` instead of `Behavior.NONE`
  (`getBehavior()` → `Optional<JmlContractBehavior>`, `behavior()` now `@Nullable`). `JmlClause` subclasses no
  longer keep their own `kind` field — the keyword node is a regular child, so clause child-node counts shift
  by one. `abrupt_behavior` now maps to `JmlBehaviorKeyword.ABRUPT` instead of being folded into `BREAK`.
  `Node.associatedSpecificationComments`/`ObservableProperty.ASSOCIATED_SPECIFICATION_COMMENTS` removed;
  `Node.treeIterator` is public.
- Error positions for malformed local variable declarations: `BlockStatement` lookahead rewritten to
  `Modifiers() Type() Identifier() (ArrayBracketPair())* ("," | "=" | ";")` so `int i = ;` is reported at the
  offending token instead of the statement start (regression `VariableDeclarationErrorPositionTest`).
- Regenerated metamodel, `JavaParserMetaModel`, visitors and pretty-printers for the keyword nodes and the
  merged `JmlQuantifiedVariable`/KeY label nodes (`08e80b657`, `932199960`, `381bd2f68`, `982a4dd9d`,
  `0285d722d`); canonical regeneration after the rebase is `efa77e201`.

**Fixed**

- `AddNameToClauseOrContract` no longer crashes the whole `textDocument/codeAction` request for unsupported
  declarations (only claims nodes `createEdit`/`hasName` support); `CodeActionCollector` guards
  `isCallableForNode`/`createCodeAction` so a failing action degrades to "no action" instead of aborting the
  request (`AddQuantification`'s unresolved-symbol path included).
- `AddBehaviorKeyword` emits a valid VS Code snippet choice and drops the empty `NONE` keyword from the
  choices (`4060e3764`).
- `JmlClauseKeyword` token constants corrected (`DETERMINES`/`LOOP_DETERMINES`/`SEPARATES`/
  `LOOP_SEPARATES` no longer point at `DURATION_REDUNDANTLY`); `DECREASING_REDUNDANTLY` added so
  `decreasing_redundantly` loop contracts parse (`0bf519ce9`).
- Unimplemented actions no longer advertised: `WellDefinednessCheck` stops offering its quick-fix, `AddStubs`
  stops registering the code lens; `VerifyAgainstParent` cache populated so the offered command reports.
- KeY label-after-keyword printing (`assert LAB: e;`) restored in the hand-maintained printers after the
  rebase (`381bd2f68`, `982a4dd9d`, `0285d722d`); `JmlProcessor`/grammar error-positioning restored.

---

## Unreleased — 1.0.1-SNAPSHOT

Generic KeY meta-construct statement (feature branch `feat/generic-keymeta-construct`): one
function-call-style production `#name(arg1, …, argN)` replaces the 23 name-specific
`KeyMetaConstructStatement()` alternatives, the construct name is lexed as `SVIDENTIFIER`
instead of a dedicated token, and `KeyMetaConstruct` stores its arguments in a single ordered
child list. Breaking API and grammar change; carried on the 1.0.x release line
(`1.0.0` → `1.0.1-SNAPSHOT`). This is a deliberate exception to the bump rules above (a breaking change
shipped as a patch on the maintenance line); the same change is also on `main` for `1.2.0-SNAPSHOT`.

Mainline squash: `a2e5e4b23` (PR #95). On `releases/JMLtk-v1.0.1` the feature commit is `e11972fe6` and the
version is set to `1.0.1` in `c25a757e1`; no `JMLtk-v1.0.1` tag exists yet.

**Breaking**

- `KeyMetaConstruct`: `getChild()` (`Node`) + `getSchemas()` (`NodeList<Node>`) are replaced by a
  single ordered `getChildren()` (`NodeList<Node>`). Parser constructor is now
  `KeyMetaConstruct(TokenRange, String kind, NodeList<Node> children)` with varargs helpers
  `(TokenRange, String | JavaToken, Node...)`; fluent accessors `kind()` / `children()`.
- Grammar: the 23 dedicated `#name` statement tokens are deleted. Construct names are lexed as
  `SVIDENTIFIER` and disambiguated in `Statement()` by a pure semantic lookahead over the
  reserved-name set (`GeneratedJavaParserBase.isKeyMetaConstructName`), which is now configurable
  per parser configuration (see **Added** below). A statement-level `#name(…)` with a
  *non-reserved* name is parsed as a plain expression statement, exactly as before; a bare
  `#ident` without `(` remains a statement placeholder.
- Construct names containing hyphens are renamed to camelCase, so every `#name` is a valid Java
  identifier: `#switch-to-if` → `#switchToIf`, `#do-break` → `#doBreak`, `#enhancedfor-elim` →
  `#enhancedforElim`, `#evaluate-arguments` → `#evaluateArguments`, `#expand-method-body` →
  `#expandMethodBody`, `#for-to-while` → `#forToWhile`, `#method-call` → `#methodCall`,
  `#static-initialisation` → `#staticInitialisation`, `#array-post-declaration` →
  `#arrayPostDeclaration`, `#resolve-multiple-var-decl` → `#resolveMultipleVarDecl`,
  `#init-array-creation-transient` → `#initArrayCreationTransient`, `#init-array-assignments` →
  `#initArrayAssignments`, `#init-array-creation` → `#initArrayCreation`, `#unwind-loop-bounded`
  → `#unwindLoopBounded`, `#unwind-loop` → `#unwindLoop`, `#special-constructor-call` →
  `#specialConstructorCall`, `#post-work` → `#postWork`, `#constructor-call` →
  `#constructorCall`. The `schemajava.txt` test corpus is updated accordingly.
- The expression-position constructs `#static-evaluate` / `#create-object` /
  `#length-reference` are renamed to `#staticEvaluate` / `#createObject` /
  `#lengthReference`, and the `#isstatic` token with its `IsStaticMC()` production is
  removed. `#create-object` and `#length-reference` are adopted as `#createObject` and
  `#lengthReference` for use in KeY schemas. None of the four is given any special case in
  the grammar: `#staticEvaluate(…)`, `#createObject(…)`, `#lengthReference(…)` and
  `#isstatic(…)` parse as plain `MethodCallExpr` calls in both expression and statement
  position (like any non-reserved `#name(…)`). The `KeyMetaConstructExpression` AST node —
  whose only parser producer was `#isstatic` — is removed, along with
  `Expression.asKeyMetaConstructExpression()` and its visitor/metamodel/pretty-printer
  entries.
- `ObservableProperty`: `SCHEMAS` is replaced by `CHILDREN`.

**Changed**

- The trailing `;` of a meta-construct statement is uniformly optional (previously mandatory for
  most constructs and absent for `#forToWhile`/`#switchToIf`). The pretty printer does not print
  a `;`, keeping the canonical no-semicolon form for the loop/if transformers.
- Arguments may be a schema variable (bare `#ident` → `KeyExpressionSV`), a nested construct
  call (`#mn(#elist)`), an assignment, a labeled statement (`#lb0: break #lb1;`), or a control
  statement (`for`/`while`/`do`/`if`/`switch`/`try`/`synchronized`/`assert`/`break`/`continue`/
  `return`/`throw`).
- Out of scope (unchanged): `KeyMetaConstructType` (`#typeof`) keeps its dedicated token, as
  do the `#set` / `#commit` / `#finish` / `#abort` / `#catchAll` statement tokens.

**Added**

- `ParserConfiguration`: the reserved names of the generic `#name(…)` meta-construct statements
  are now configurable — `setKeyMetaConstructNames(Set<String>)` / `getKeyMetaConstructNames()`,
  with the previous 23-name table moved to the public constant
  `ParserConfiguration.DEFAULT_KEY_META_CONSTRUCT_NAMES` (the default). Semantics are full
  replacement: names outside the configured set fall back to ordinary expression statements, and
  an empty set disables meta-construct parsing. The configured set is pushed into the parser on
  every parse, like the other configuration values.
- `KeyMetaConstructArg()` grammar production; the now-unused `VariableSV()`/`JumpLabelSV()`
  productions are removed.
- `KeyMetaConstructTest` (javaparser-key-testing): round-trip tests — parse → print → re-parse
  preserves the ordered arguments; mixed argument shapes; labeled-statement arguments; the
  no-semicolon forms `#forToWhile`/`#switchToIf`; and non-reserved `#name(…)` staying an
  expression statement. Configured-name tests: full replacement (a custom name becomes a
  meta-construct while a standard one falls back), an empty set (expression statement with `;`,
  parse failure without), and a custom construct round-trip. The `#isstatic(…)` fallback test
  (two-argument statement form, one-argument expression form, round-trip) covers the removed
  `#isstatic` production.
- Fallback tests for the renamed expression-position constructs: `#createObject(…)`,
  `#lengthReference(…)` and `#staticEvaluate(…)` parse as `MethodCallExpr` and round-trip.

**Docs**

- The jmltoolkit.github.io documentation of the meta-construct names must be updated to the new
  camelCase identifiers and the generic `#name(…)` syntax.

---

## Combined releases (JML + KeY, this repository)

### Version 3.28.2-J8.0-K13.6  (2026-10-25, prepared)

Second combined release. Completes the K13.6 feature level on this lineage: the KeY fork published K13.6 on
2026-07-20 (`c26f62ea3`, tagged `KEY-JAVAPARSER-3.28.0-K13.6`), but that fix never crossed back into the JML
fork's release line — this release cherry-picks it. The pre-merge prepare commit is `0b2a7a359` "prepare
release 3.28.2-J8.0-K13.6" and the cherry-pick is `183af569e` (patch-identical to `c26f62ea3`); neither is
reachable from `main`. The published commit is `0e5e605fb` on `main` / `33b8f38a2` at tag `JMLtk-v1.0.0`.

**Fixed**

- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive
  integral type — cherry-picked from the KeY fork (`c26f62ea3`; the pre-merge cherry-pick `183af569e` has the
  same patch): during the recursive descend
  of an array type the primitive check now uses `equals` instead of `isAssignableBy`.
- The other three K13.6 fixes from the KeY fork were already present in `3.28.2-J8.0-K13.5`: weak
  `JavaParserFacade` cache (`febe5a8a9`), call-context threading through `solveMethodAsUsage` (`f6b2260d4`),
  and the missing `ConcreteSyntaxModel` classes (`a8bb0355b`).

### Version 3.28.2-J8.0-K13.5  (2026-10-25)

First combined release, and the first release built with Gradle instead of Maven. Base
JavaParser is 3.28.2. Release commits: `82b6d7869`/`b66825037` "prepare release" (pre-merge forks; not
reachable from `main`).

**Added**

- JML contracts on `LambdaExpression` (PR #60, `82b57d207`)
- Contracts on `CompactConstructorDeclaration` (PR #59, `9bfabd52e`)
- KeY integration updates — "changes for KeY" (PR #54, `cbb924bbd`): grammar/`JavaToken` handling for KeY
  ghost syntax, `GhostTest`
- LSP distribution revived (PR #55, `0a68a74e2`)
- JRE stub generation restored — "Stubby is back" (PR #35, `5106df9a3`)
- `Stat` tool (PR #36, `f3cb5f207`)
- `JmlQuantifiedExpr` now uses `VariableDeclarator` (PR #48, `418503588`); `JmlLabeledClause` renamed (PR #49, `caadb91ac`)
- Test suite re-activated (PR #21, `570aec9d9`)

**Changed**

- Build migrated to Gradle; Maven build files removed (`d13918922`, `a384db3bc`); Spotless/Checkstyle alignment
  (`e0ee2bab4`, `1fb8c8376`); default branch `master` → `main` (`b5eb7957b`)
- CodeQL workflow added; SonarQube removed (`a4f72415e`, `3913e89c2`)
- Upstream JavaParser 3.28.1 and 3.28.2 merged (includes phase-2 poly-expression inference for method/constructor
  references, multi-pattern case-label rules, OSGi support)

**Fixed**

- Grammar: precise error positions for malformed variable declarations (PRs #71/#72, squashed as
  `6782cb6fc`; the original PR commit `3da7c7825` is not reachable from `main`)
- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive integral
  type (`7883cd9d0`)
- CodeQL findings: unused imports, statements with no effect, unclosed files (`14327cd25`, `5194c58f1`,
  `a3c1c9d5f`, `7795d099d`)

---

## KeY releases (wadoon/key-javaparser)

### Version 3.28.0-K13.6  (2026-07-20)

Tagged `KEY-JAVAPARSER-3.28.0-K13.6`. Release commit: `056533f92` "release 3.28.0-K13.6".

**Fixed**

- `StackOverflowError`: thread the call context through `solveMethodAsUsage` (`f6b2260d4`)
- Memory leak: `JavaParserFacade` cache now holds its values weakly (`febe5a8a9`)
- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive integral
  type (`c26f62ea3`)
- Missing `ConcreteSyntaxModel` classes (`a8bb0355b`)

### Version 3.28.0-K13.5  (2026-07-04)

Release commit: `7ecfed818` "the version 3.28.0-K13.5".

**Fixed**

- Method-frame resolution (`c863d4768`)

### Version 3.28.0-K13.4  (2026-03-05)

Release commit: `dd2ad4fba` "Fix protected visibility rules and version bump".

**Fixed**

- Protected-visibility rules in the symbol resolver

### Version 3.28.0-K13.3  (2026-03-05)

`b5a850cbf` "Fix internal types test and bump version" — internal-types handling.

### Version 3.28.0-K13.2  (2026-03-05)

`0d48924ca` "inner classes can see anything" — inner classes can resolve members of their enclosing scopes.

### Version 3.28.0-K13.1  (2026-03-04)

`6a841a2e3` "Bump to 13.1" — minor release after K13.0.

### Version 3.28.0-K13.0  (2026-03-04)

**Added**

- `KeyMarkerStatement` AST node (`e92599acb`)

### Version 3.28.0-K12.10  (2026-03-04)

**Changed**

- Method resolution now considers the call context (`71f74fb40` + bump `566636ea1`)

### Version 3.28.0-K12.9  (2026-03-04)

`4fbdf1492` "K12.9" — minor release.

### Version 3.28.0-K12.8  (2026-03-04)

`08c4721bc` "K12.8" — minor release.

### Version 3.28.0-K12.7  (2026-03-04)

`3ad29b010` "set version to K12.7" — minor release.

### Version 3.28.0-K12.5  (2026-03-03)

Release commit: `4564ffe65` "set version to K12.5". Covers the K11.2 → K12.5 iterations.

**Added**

- JML commentary handling in the parser (`a944396b3`)
- JMLDoc support for KeY AST nodes — "K12.2 now with JMLDoc" (`6a260468d`)
- Setters and pretty printing for the JML/KeY nodes (`e8da511eb`); missing meta classes (`12a358398`)

**Fixed**

- `AssignableToPrimitive` interface (`7ac76ac42`, K11.2)
- Resolution inside `KeyCatchBranch` now respects its parameters (`de35187e1`)
- Error in `JmlDocsStatements` (`d5b953858`)

### Version 3.28.0-K11.1  (2026-03-02)

Tagged `K11.1`. Release commit: `e09ddd7b0` "AssignableToPrimitive interface". This entry also covers the
development iterations since K7, which the fork versioned as K8…K11-pre and K10.1:

**Added**

- `AssignableToPrimitive` interface (`e09ddd7b0`)
- `ArrayLengthValueDeclaration` opened up (K10, `bd3e650ac`)
- Non-`Optional` getters using nullable (JSpecify) annotations (K11-pre, `6125dfb99`); JSpecify annotations on
  setters and return values (K10.1, `bcce6b70c`)
- New annotation to avoid lexical-preserving-printer errors caused by dynamic method resolution (`3004dfd69`)
- Catch-clause groundwork — "catch is required" (`3ee9eba0f`, K8 iteration)

**Fixed**

- `KeyCatchClauseSV` (K9, `d325b863d`)
- Static `JavaParser` disabled in the tooling path (`c2507bfdd`)

**Changed**

- Base rebased from 3.26.3 via 3.27.0/3.27.1 to 3.28.0 (upstream merges `71dcd5c56`, `55348354e`, `6dbb2be00`)
- Grammar rule rename (`aaba2f83f`)

### Version 3.26.3-K7  (2024-12-23)

"Fix test cases, release complete" (`ca6d9dc97`, `86115cb03`). Base JavaParser 3.26.3; the fork tracked upstream
3.25.4 → 3.25.8 → 3.26.2 → 3.26.3 in this cycle and prepared the KeY catch-clause support released in the
following iterations.

**Changed**

- Upstream JavaParser 3.25.4 → 3.26.3 tracked (`67dcf3c50`, `afaffe24e`, `c37b0d5b0`, …); base rebased to 3.25.8
  (`d38558571`) and finally 3.26.3 (`874efbc1f`, `24dd80a0a`)

### Version 3.25.3-K6  (2023-06-29)

"New release" (`dd2a13d15`).

**Changed**

- Ghost-modifier pretty printing (`424e7e5b6`)

### Version 3.25.3-K5  (2023-06-23)

Tagged `v3.25.3-K5`. Release commit: `d0eb777bc` "relese K5: KeY as class types".

**Added**

- "KeY as class types": new `LogicalType` AST node and `LogicalTypes` utility, grammar updates (`d0eb777bc`)
- `javaparser-key-testing` module

**Fixed**

- Pretty-printer holes for the KeY constructs — issues #2/#3/#4 (`89724d7f0`, `c8223e169`, `e65ab3f05`)
- `setTransactionType` in constructor (issue #5, `c92594cd1`)

### Version 3.25.2-K3  (2023-04-17)

Tagged `k3`. Release commit: `af7303951` "version K3".

**Changed**

- Java 17 sealed/non-sealed classes support (`25b12b3ed`)
- Empty `BlockStmt` without range no longer errors (`9f925d5a7`)

(Previous iteration K2, `3.25.2-K2-SNAPSHOT`/tag `k2`, added method-frame name resolution — `490278748`.)

### Version 3.24.1-K1  (2022-03-04)

Tagged `k1`. Release commit: `211d31f46` "set non-snapshot" (merge of the `weigl/keyadditions` branch,
`027c37505`). Initial KeY fork release.

**Added**

- KeY syntax / proof parser (`9810b8ded`, `f8ecacf87`) — the basis of `\by` proof scripts
- KeY's modifiers and implicit identifiers (`b650a92fe`); `KEYIDENTIFIER` changed to the `$<id>` token rule
  (`e945cc169`)
- `Key`-prefixed AST classes (`KeyTypeSV` et al.; KeY-prefix classes renamed to a `Key` prefix, `2c07a5c50`;
  `KeyTypeSV` attribute renamed `name` → `text`, `7e8783227`)

---

## JML releases (this repository, before the KeY merge)

### Version 3.26.2-b7  (2025-01-23)

"Release 3.26.2-b7" (`54a11df57`).

**Added**

- Generation of annotated getters without `Optional` (`870b5ce70`)
- Javadoc-error-ignorance fix and token-types test fix (`ccdd9901a`, `a6decfba8`)

**Fixed**

- Regression in the printer (`82e3e91b2`)

### Version 3.26.2-b6  (2024-11-15)

"Set version to 3.26.2-b6-SNAPSHOT" (`b89f51c48`) after merging upstream 3.26.2 — "fix merge errors" (`cb46f5b57`).

### Version 3.25.10-b6  (2024-05-23)

**Fixed**

- Missing JML delimiters in pretty printing (`72507343c`)

### Version 3.25.10-b3  (2024-05-04)

**Changed**

- Clean-up of deprecated JML AST classes (`5a1852c82`)
- Renaming of AST nodes (`bfaa6b1c2`)
- Docs moved to the GitHub wiki (`4ea8d929f`)

### Version 3.25.10-b2  (2024-05-03)

**Added**

- `JmlClauseIfClause` and `JmlCallableClause` (`f2ef7f2f0`)
- JML clauses in `TypeCastingGenerator` (`0f20de0d6`); contract lists in methods/constructors are mandatory
  (`ba3385f58`); meta-model regeneration (`30ce07988`)

---

## Version history (all distinct J/K version states in the git history)

| Version state | Date | Commit / tag | Fork |
|---|---|---|---|
| `3.24.1-K1-SNAPSHOT` | 2022-03-04 | `49afba28d` | KeY |
| `3.24.1-K1` | 2022-03-04 | `211d31f46`, tag `k1` | KeY |
| `3.24.1-K2-SNAPSHOT` | 2022-03-04 | `2ea2789e0` | KeY |
| `3.25.2-K2-SNAPSHOT` | 2023-04-15 | `e5d29e4a7`, tag `k2` | KeY |
| `3.25.2-K3-SNAPSHOT` | 2023-04-17 | `af7303951`, tag `k3` | KeY |
| `3.25.2-K4-SNAPSHOT` | 2023-05-24 | `575a36ac1` | KeY |
| `3.25.3-K4-SNAPSHOT` | 2023-05-24 | `185b6a8a5`, tag `v3.25.3-K3` | KeY |
| `3.25.3-K5-SNAPSHOT` | 2023-05-24 | `bd8db3116` | KeY |
| `3.25.3-K5` | 2023-06-23 | `d0eb777bc`, tag `v3.25.3-K5` | KeY |
| `3.25.3-K6-SNAPSHOT` | 2023-06-23 | `14997a791` | KeY |
| `3.25.3-K7-SNAPSHOT` | 2023-06-29 | `dd2a13d15` (release of K6) | KeY |
| `3.25.8-K7-SNAPSHOT` | 2024-02-03 | `d38558571` | KeY |
| `3.26.2-K7-SNAPSHOT` | 2024-12-09/21 | `4739d5750`, `874efbc1f` | KeY |
| `3.26.3-K7-SNAPSHOT` | 2024-12-21/23 | `24dd80a0a`, `ca6d9dc97` (release of K7) | KeY |
| `3.26.3-K8-SNAPSHOT` | 2024-12-26 | `3ee9eba0f` | KeY |
| `3.26.3-K9-SNAPSHOT` | 2025-01-03 | `d325b863d` | KeY |
| `3.26.3-K10-SNAPSHOT` | 2025-01-08/11 | `bd3e650ac`, `6125dfb99` (K11-pre) | KeY |
| `3.27.0-K10-SNAPSHOT` | 2025-06-19 | `63f374098` | KeY |
| `3.28.0-K10-SNAPSHOT` | 2026-02-01/02 | `6dbb2be00`, `86a694b8d` | KeY |
| `3.28.0-K10.1-SNAPSHOT` | 2026-02-02 | `2d516a68e` | KeY |
| `3.28.0-J7.0-K10.1-SNAPSHOT` | 2026-02-03 | `a096d093f` (first combined) | JML (jmltk) |
| `3.28.0-J8.0-K10.1-SNAPSHOT` | 2026-02-03 | `02012f2ca` | JML (jmltk) |
| `3.28.0-K11.1-SNAPSHOT` | 2026-03-02 | `e09ddd7b0`, tag `K11.1` | KeY |
| `3.28.0-K12.0/12.2/12.3-SNAPSHOT` | 2026-03-03 | `a944396b3`, `6a260468d`, `de35187e1` | KeY |
| `3.28.0-K12.5` | 2026-03-03 | `4564ffe65` | KeY |
| `3.28.0-K12.7` | 2026-03-04 | `3ad29b010` | KeY |
| `3.28.0-K12.8` | 2026-03-04 | `08c4721bc` | KeY |
| `3.28.0-K12.9` | 2026-03-04 | `4fbdf1492` | KeY |
| `3.28.0-K12.10` | 2026-03-04 | `566636ea1` | KeY |
| `3.28.0-K13.0` | 2026-03-04 | `e92599acb` | KeY |
| `3.28.0-K13.1` | 2026-03-04 | `6a841a2e3` | KeY |
| `3.28.0-K13.2` | 2026-03-05 | `0d48924ca` | KeY |
| `3.28.0-K13.3` | 2026-03-05 | `b5a850cbf` | KeY |
| `3.28.0-K13.4` | 2026-03-05 | `dd2ad4fba` | KeY |
| `3.28.0-J8.0-K13.4` | 2026-03-07 | `de600d8e9` | JML (jmltk) |
| `3.28.1-J8.0-K13.4` | 2026-05-10 | `1d5f30f1b` | JML (jmltk) |
| `3.28.1-J8.0-K13.6-SNAPSHOT` | 2026-07-04 | `085061d4a` | JML (jmltk) |
| `3.28.0-K13.5` | 2026-07-04 | `7ecfed818` | KeY |
| `3.28.1-J8.0-K13.5-SNAPSHOT` | 2026-07-06 | `a8238e94a` | JML (jmltk) |
| `3.28.2-J8.0-K13.5-SNAPSHOT` | 2026-07-06 | `acf8dc398` | JML (jmltk) |
| `3.28.0-K13.6` | 2026-07-20 | `056533f92`, tag `KEY-JAVAPARSER-3.28.0-K13.6` | KeY |
| `3.28.0-K13.7-SNAPSHOT` | 2026-07-20 | `0336c93d6` | KeY |
| `3.28.2-J8.0-K13.5` | 2026-10-25 | `82b6d7869` (release) | JML (jmltk) |
| `3.28.2-J8.0-K13.6` | 2026-10-25 | `0b2a7a359` (prepare release) | JML (jmltk) |
| `1.0.0` (= `3.28.2-J8.0-K13.6`) | 2026-10-25 | `0e5e605fb` (prepare and release, `main`); tag `JMLtk-v1.0.0` → `33b8f38a2` (same tree) | JML (jmltk) |
| `1.1.0-SNAPSHOT` | 2026-10-25 | `df1e08542` (starting version) | JML (jmltk) |
| `1.2.0-SNAPSHOT` | 2026-11-01/07 | `a36ea22be`, `37d5a8db4` | JML (jmltk) |
| `1.0.1-SNAPSHOT` | 2026-11-08 | `3d5260a47` | JML (jmltk, `releases/JMLtk-v1.0.1`) |
| `1.0.1` | 2026-11-14 | `c25a757e1` (set version) | JML (jmltk, `releases/JMLtk-v1.0.1`) |

`222cd3086` briefly reset the working version on `main` to `1.1.0-SNAPSHOT` on 2026-11-02; the following
`weigl/lemmata` merge (`37d5a8db4`) restored `1.2.0-SNAPSHOT`. The `1.0.1` maintenance branch is cut from
`1.0.0` and does not contain the `1.2.0-SNAPSHOT` changes.

JML `-b` iterations: `3.25.10-b2` (`f2ef7f2f0`, 2024-05-03), `3.25.10-b3` (`6cca21e71`, 2024-05-04),
`3.25.10-b6` (`72507343c`, 2024-05-23), `3.26.2-b6` (`b89f51c48`, 2024-11-15), `3.26.2-b7`
(`54a11df57`, 2025-01-23), then `3.27.0-b7-SNAPSHOT` / `3.27.1-b7-SNAPSHOT` until the KeY merge
(`71dcd5c56`, 2025-06-19 / `55348354e`, 2025-12-31).
