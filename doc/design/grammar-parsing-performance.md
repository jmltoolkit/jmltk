# Grammar parsing performance — design

- **Status:** implemented — benchmark shipped; grammar candidates P1–P4 evaluated, none shipped (negative results recorded below)
- **Date:** 2026-11-19
- **Branch:** `perf/grammar-optimization`
- **Scope owner:** JMLtk core parser (`javaparser-core/src/main/javacc/java.jj`)

## 1. Problem

Parsing throughput of JMLtk is unmeasured and the grammar carries a large amount of JML/KeY
surface area that ordinary Java parsing also executes. There is no benchmark harness in the
repository, so any "optimization" would otherwise be unverifiable.

### Measured baseline (ad-hoc, uncommitted driver, JDK 21, current `main` build, end-to-end `parse()` with default processors)

| Corpus | Files | Bytes | Mode | Throughput |
|---|---|---|---|---|
| `javaparser-core/src/main/java` | 798 | 4.9 MB | JML off | ~2.8 s ≈ 1.6 MB/s |
| `jmlparser-jml-tests/.../fullexamples` | 2,959 | 17.8 MB | JML on | ~15 s ≈ 1.1 MB/s |

The committed harness measures a slightly wider `plain` corpus (it also includes the
symbol-solver main sources) and records its own fresh baseline, so the table above is only the
starting reference.

JFR profile of the plain-Java run:

- ~29% of samples in generated parser code (`GeneratedJavaParser`)
- ~14% in `GeneratedJavaParserTokenManager`
- ~60% in post-parse AST validation/walking (`Node.PreOrderIterator`, `TreeVisitorValidator`)
- Hot parser chain: `jj_3R_139 → jj_3R_144 → jj_3R_248 → jj_3R_480 → jj_3R_380`

`jj_3R_139` is the generated FIRST-set routine for `Identifier()`, which currently lists
~180 token alternatives (`java.jj` lines 3554–3606). Inside a syntactic `LOOKAHEAD(...)`
JavaCC expands that set into a long `jj_scan_token` if-chain; the following links are
`Identifier → ClassOrInterfaceType → type`, so the cost is multiplied across type, cast,
lambda and name lookaheads.

Structural finding: `ParserConfiguration.isProcessJml()` is **not consulted by the grammar**.
It only controls post-processing (`JmlProcessor`, `JmlDocHardRemover`, `JmlWarnRemaingJmlDoc`).
Plain Java parsing therefore evaluates JML/KeY alternatives and lookaheads unconditionally.

## 2. Goals and non-goals

### Goals

1. Add a repeatable, committed benchmark that isolates grammar cost from validation cost.
2. Reduce grammar/tokenizer time for plain Java and JML workloads, measured per change.
3. Keep every existing test green and preserve parse acceptance/error recovery exactly.

### Non-goals

- Optimizing validators, post-processors or `Node.walk` (~60% of end-to-end time).
- Symbol solver, type resolution, or AST API shape changes.
- New language features or accepting new syntax.
- Changing ambiguity resolution or error-recovery semantics.

## 3. Components

### 3.1 `tools/bench` (`:tools:bench`)

A Kotlin CLI module following the existing `tools/*` convention. No JMH dependency.

Responsibilities:

- **Workload loading.** Deterministically collect and sort files; preload contents before
  timing so I/O and char decoding are outside the measured region. Report files, bytes,
  ok/fail counts.
- **Corpora.**
  - `plain`: `javaparser-core/src/main/java` + `javaparser-symbol-solver-core/src/main/java`.
  - `jml`: `jmlparser-jml-tests/src/test/resources/fullexamples`, parsed with
    `setProcessJml(true)`.
  - `path:<dir>`: arbitrary directory of `.java` files.
- **Modes.**
  - `grammar`: clear `ParserConfiguration.getProcessors()` before timing; measures tokenizer,
    grammar and AST construction only.
  - `full`: default processors/validators; measures real end-to-end `parse()`.
- **CLI.** `--corpus`, `--mode`, `--warmup N`, `--rounds N`, `--json <file>`,
  `--baseline <json>`, `--verify`.
- **Output.** Per-round ms, MB/s, ok/fail, median; with `--baseline`, per-corpus/-mode deltas.
- **`--verify`.** Parse-result parity check: the set of successfully parsed files and the set
  of failing files must equal the recorded baseline. A speedup that changes acceptance fails
  verification.
- **Invocation.** `./gradlew :tools:bench:run --args="..."`. Not wired into CI gates initially
  because shared runners are too noisy; usage documented in `tools/README.md`.
- Baseline JSONs are committed under `tools/bench/baseline/`.

### 3.2 Identifier predicate

A total predicate over token kinds, e.g. `TokenTypes.isIdentifierKind(int kind)` or a static
set in `GeneratedJavaParserBase`, covering exactly the token kinds currently accepted by
`Identifier()`. It must be derived from the existing alternative list and guarded by an
exhaustive test, so the accepted set cannot drift.

The predicate is emitted as a leading semantic `LOOKAHEAD` inside `Identifier()`:

```java
String Identifier():
{ String ret; }
{
    LOOKAHEAD({ isIdentifierKind(getToken(1).kind) })
    ( <IDENTIFIER> | ... existing alternatives ... )
    { ... }
}
```

Verified behavior (scratch grammar, JavaCC 4.0): inside a caller's syntactic lookahead this
generates `jj_semLA = isIdentifierKind(getToken(1).kind); if (!jj_semLA || jj_3R_n()) ...`,
so non-identifier tokens fail the check in O(1) instead of walking the whole alternative
chain, and nothing is consumed during speculation because semantic lookaheads are evaluated
under the `lookingAhead` flag.

A `JAVACODE` production was evaluated and rejected: JavaCC calls it directly during
syntactic lookahead, where it consumes tokens and throws, corrupting speculation.

## 4. Data flow

```
tools/bench
  -> load corpus (preloaded, sorted)
  -> per file: new JavaParser(config).parse(source)
       -> GeneratedJavaParserTokenManager   (lexing)
       -> GeneratedJavaParser productions   (grammar + lookahead + AST)
       -> Processors/validators (full mode only)
  -> collect timings and ok/fail sets
  -> print / JSON / baseline delta / --verify parity
```

The grammar changes only affect the middle stage; the benchmark keeps that visible.

## 5. Workstream

Every item is measured independently. Each item is one commit. An item ships only if it
improves at least one corpus/mode beyond noise and regresses none.

### P1 — O(1) identifier rejection in lookaheads (highest confidence)

- Add a total `isIdentifierKind(int)` predicate covering exactly the token kinds currently
  accepted by `Identifier()`.
- Prefix the existing `Identifier()` alternative list with
  `LOOKAHEAD({ isIdentifierKind(getToken(1).kind) })`. The alternatives stay unchanged, so the
  accepted language is identical; only the speculative failure path becomes O(1).
- Effect: the ~180-branch `jj_3R_139` chain is skipped for every non-identifier token tried
  during type, cast, lambda and name lookaheads. `SimpleName`, `Name`,
  `ClassOrInterfaceType`, `Type` and all type-based lookaheads inherit the win.
- Tests: exhaustive kind↔`tokenImage` test (for every `GeneratedJavaParserConstants` kind,
  assert that parsing the token image in an identifier position succeeds iff the predicate
  says it is identifier-like); targeted keyword-as-identifier parser tests.
- Rejected alternative: turning `Identifier()` into a `JAVACODE` production. Verified in a
  scratch grammar that JavaCC invokes such a production directly during syntactic lookahead,
  where it consumes tokens and throws, breaking speculation.

#### P1 result (evaluated 2026-11-19, not shipped)

The predicate covered exactly the grammar's `Identifier()` kinds, and the short-circuit was
verified in the generated lookahead routine. It still changed speculative-lookahead outcomes:
14 `FragmentTest` class-level fragments that are known-unsupported and skipped at baseline began
failing assertions because their diagnostics changed (production-wide abort bypassed the choice's
expected-token bookkeeping). Two variants were tried and rejected: gating with `jj_lookingAhead`
(ineffective — the generator sets it true *around* predicate evaluation) and a fallback
`<IDENTIFIER>` branch (still aborted the production). P1 was fully reverted with no grammar
change. Any future attempt must preserve `FragmentTest` behavior and expected-token diagnostics,
not just acceptance of the main corpora.

### P2 — Replace full-rule lookaheads with token-set/bounded decisions

Candidate sites, each evaluated separately:

| Site | Current | Replacement |
|---|---|---|
| `Statement()` | `LOOKAHEAD(24) StatementExpression()` | first-token set check + bounded lookahead |
| `Statement()` | `LOOKAHEAD(KeyMethodBodyStatement())` | Key-first-token kind gate |
| `Statement()` | `LOOKAHEAD(2) StatementSV()`, `LOOKAHEAD(3) YieldStatement()` | keep or bound after measurement |
| `PrimaryPrefix` | `LOOKAHEAD(ResultType() "." "class")` | first-token gate |
| `PrimaryPrefix` | `LOOKAHEAD(AnnotatedType() "::")` | first-token gate |
| `UnaryExpression`/cast site | `LOOKAHEAD(CastExpression())` | first-token gate or bounded lookahead |
| lambda site | `LOOKAHEAD(Parameter())` | first-token gate |
| lambda site | `LOOKAHEAD(VariableDeclaratorId() ",")` | bounded lookahead |
| class body | `LOOKAHEAD(FieldDeclaration())` | first-token gate |

Rules:

- No change to which parse tree a given input produces.
- Predicates are total: `false` on EOF/unknown kinds, never throw.
- When a site is ambiguous by construction, keep full-rule lookahead and document why.

#### P2 result (evaluated 2026-11-19, not shipped)

Removed the two redundant `LOOKAHEAD(2)` guards before `TypeArguments` in
`ClassOrInterfaceType`, plus two `;` lookaheads (compilation-unit leading semicolons and
`ResourceSpecification`). The two comma lookaheads were **kept**: enum constants and array
initializers allow a trailing comma, so `("," entry)* [","]` needs the two-token check.
Tests and `--verify` parity were green, but an interleaved A/B against a pre-change worktree
(three pairs, plain/grammar, best-of-7 rounds) showed the change faster in 1 pair (−20%) and
slower in 2 pairs (+1%, +16%). No repeatable beyond-noise win, so it was reverted.

### P3 — JML/KeY gating (measurement-gated)

- Expose `ParserConfiguration.isProcessJml()` in `GeneratedJavaParserBase` (set when the
  parser is created).
- Gate only choke points where JML-only alternatives or lookaheads are attempted on ordinary
  Java tokens. JML stays lexically active so markers still tokenize.
- If the measured benefit is within noise, do not ship this item; record the negative result
  in the changelog note and the benchmark report.

#### P3 result (evaluated 2026-11-19, not shipped)

Added `processJml` plumbing (including `enableJml` setting it for `parseJml*` entry points) and a
production-start `LOOKAHEAD({ isProcessJml() })` guard on `JmlPrimaryPrefix()`. One
`FragmentTest` class-level fragment (`decl_385738584`, nested `\forall` inside `ensures`) failed.
Replacing the guard with `LOOKAHEAD({ true })` failed identically, proving that a leading
semantic lookahead on this production perturbs speculative lookahead regardless of its value.
The task was reverted per its own abort rule.

#### Statement lookahead experiment (evaluated 2026-11-19, not shipped)

`Statement()`'s `LOOKAHEAD(24) StatementExpression()` was reduced to `LOOKAHEAD(2)`. All core and
JML tests passed, but three interleaved A/B pairs on plain/grammar (best-of-7) showed the change
3–8% **slower** every time (base 2289–2344 ms vs new 2374–2498 ms). Reverted. Smaller lookahead
here appears to route more statements through a longer parse path, outweighing the saved scan.

### P4 — Lexer tuning (stretch, measured like every other item)

Only if P1–P3 plateau and the profile still shows a meaningful token-manager share:

- `jjMoveNfa_0` fallback paths, comment/string rules, SKIP rule ordering.
- `CommonTokenAction` allocation cost.

No lexical behavior change is acceptable; parity via `--verify` plus full test suite.

## 6. Measurement protocol

- JDK 21, same machine, no other heavy load.
- ≥3 warmup rounds, ≥5 measured rounds; report median and all rounds.
- Treat <3% delta as noise.
- Compare per corpus and mode: `plain/grammar`, `plain/full`, `jml/grammar`, `jml/full`.
- Store the pre-change baseline JSON under `tools/bench/baseline/` and compare with
  `--baseline` in every follow-up measurement.
- Record the result (including "no change") in the commit message body.

## 7. Correctness and error handling

- New predicates never throw; unknown kinds return `false`, letting JavaCC fall through to the
  existing alternatives and the current `recover`/`recoverStatement`/`Unparsable` paths.
- Grammar edits must not change what is accepted. Corpus parity (`--verify`) and the full test
  suite are the gate.
- Generated sources are never hand-edited. After each grammar change:
  `./gradlew :jmlparser-core:compileJavacc` and, before merge,
  `./scripts/run-generators.sh`.

## 8. Verification gates per item

1. `./gradlew :jmlparser-core:compileJavacc`
2. `./gradlew :jmlparser-jml-tests:test :jmlparser-core-testing:test :jmlparser-symbol-solver-testing:test`
3. `./gradlew spotlessCheck checkstyleMain`
4. `./scripts/run-generators.sh` then `git diff --exit-code`
5. `:tools:bench` for `{plain,jml} × {grammar,full}` with `--baseline` and `--verify`

## 9. Rollout

| Commit | Content |
|---|---|
| 1 | `bench: add tools/bench parse benchmark` — harness, baseline JSON, `tools/README.md` |
| 2 | `perf(grammar): O(1) identifier lookahead` — P1 + tests |
| 3..n | `perf(grammar): ...` — P2 sites, one per commit |
| optional | `perf(grammar): gate JML/KeY alternatives by configuration` — P3, only on measured win |
| optional | `perf(grammar): ...` — P4, only on measured win |
| last | `docs(changelog): record parsing optimizations` — `jmltk-changelog.md` Unreleased |

## 10. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Ambiguity resolution changes | Medium | full module tests, corpus parity, per-site commits, revert on any parse difference |
| Identifier set drift (P1) | Medium | exhaustive token-kind test derived from the current alternative list |
| Benchmark noise | High | median of ≥5 rounds, relative deltas, <3% treated as noise |
| Generated-code churn | Low | regenerate via scripts; never hand-edit generated files |
| Scope creep into validators | Medium | explicit non-goal |

## 11. Expected outcome

Grammar-mode gains are plausible in the low-to-mid double digits because P1 attacks the
dominant parser-side hotspot; end-to-end gains will be much smaller because post-parse
validation accounts for roughly 60% of the measured baseline and is out of scope. If
measurement contradicts this expectation, the design still delivers the benchmark, the
negative results, and any individually verified wins.
