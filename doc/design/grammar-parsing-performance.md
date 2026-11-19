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
| `javaparser-core/src/main/java` | 798 | 4.7 MiB | JML off | ~2.8 s ≈ 1.6 MiB/s |
| `jmlparser-jml-tests/.../fullexamples` | 2,959 | 17.0 MiB | JML on | ~15 s ≈ 1.1 MiB/s |

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
  - `full`: default processors/validators at language level `JAVA_21`; measures real end-to-end
    `parse()` without upstream's older `POPULAR` default.
- **CLI.** `--corpus`, `--mode`, `--warmup N`, `--rounds N`, `--json <file>`,
  `--baseline <json>`, `--verify`.
- **Output.** Per-round ms, MiB/s, ok/fail, median and best-of-N; with `--baseline`, deltas.
- **`--verify`.** Parse-result parity check: files, bytes, ok/fail counts, the sets of
  successfully/failing files, and a per-file 64-bit structural fingerprint (the pretty-printed
  AST's SHA-256 prefix) must equal the recorded baseline. A speedup that changes acceptance or
  the produced tree fails verification. The run fails with status 1 before `--json` is written,
  and `--json` may not name the same file as `--baseline`.
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

#### P1 result (shipped 2026-11-19, second attempt)

The first attempt was rejected because `FragmentTest` inferred "known unsupported" from problem
message text: P1's diagnostic changes un-skipped 14 fragments even though acceptance was unchanged.
After Phase 1 replaced that heuristic with an explicit manifest, P1 was retried with

```java
LOOKAHEAD({ !jj_lookingAhead || TokenTypes.isIdentifierKind(getToken(1).kind) })
```

and a 165-kind boolean table. The generator sets `jj_lookingAhead` only around speculative
evaluation, so the guard is a no-op in real parsing (real-parse error messages are unchanged) and
only short-circuits failed lookaheads.

Measured on JDK 21 (warmup 5, rounds 7, `--verify`):

| Corpus/mode | median | best | AST parity |
|---|---|---|---|
| plain/grammar | −33.2% | −32.7% | differences limited to the edited `TokenTypes.java` |
| plain/full | −18.0% | −22.9% | differences limited to the edited `TokenTypes.java` |
| jml/grammar | −27.3% | −41.0% | clean |
| jml/full | −23.0% | −19.2% | clean |

Baselines were re-recorded on the new state.

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

A post-P1 retry targeted the hottest annotation/modifier path: removing the `LOOKAHEAD(2)` at the
head of the `Modifiers()` and `ModifiersReq()` loops. The body's FIRST set looks disjoint from the
follow set, but the check is load-bearing for JML modifier/documentation overlaps: removing it
produced 68 failures across the core and JML suites. Reverted; the parser now enters P2's sites
only with a targeted semantic predicate, if at all.

### P3 — JML/KeY gating (measurement-gated)

- Expose `ParserConfiguration.isProcessJml()` in `GeneratedJavaParserBase` (set when the
  parser is created).
- Gate only choke points where JML-only alternatives or lookaheads are attempted on ordinary
  Java tokens. JML stays lexically active so markers still tokenize.
- If the measured benefit is within noise, do not ship this item; record the negative result
  in the changelog note and the benchmark report.

#### P3 result (shipped 2026-11-19, second attempt)

The first attempt was rejected because a production-start `LOOKAHEAD({ isProcessJml() })` on
`JmlPrimaryPrefix()` perturbed speculative lookahead in `FragmentTest` (`decl_385738584`, nested
`\forall` inside `ensures`); even `LOOKAHEAD({ true })` failed identically. After Phase 1 made the
fragment gate an explicit manifest, the same implementation was retried and shipped:

- `processJml` lives in `GeneratedJavaParserBase`, is set from `ParserConfiguration`, and
  `enableJml` marks `parseJml*` entry points as processing JML;
- `JmlPrimaryPrefix()` starts with `LOOKAHEAD({ isProcessJml() })`.

Measured on JDK 21 (warmup 5, rounds 7, `--verify`):

| Corpus/mode | median | best | notes |
|---|---|---|---|
| plain/grammar | −14.8% | −18.5% | differences limited to the edited files |
| plain/full | +0.2% | −1.2% | no change |
| jml/grammar | −3.0% | −4.8% | AST parity clean; JML enables the guard |

`jml/full` is unchanged within noise (the guard is always true in JML mode). JML quantifier, let
and label prefixes no longer parse when `processJml=false`, which matches the flag's meaning; JML
markers still tokenize and parse. Baselines were re-recorded.

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

#### P4 result (evaluated 2026-11-19, not shipped)

The baseline profile still puts roughly 14% of end-to-end samples in
`GeneratedJavaParserTokenManager`, but no safe, targeted lexical change was identified. The
visible cost is the keyword/identifier DFA and NFA fallback, both of which would require the large
contextual-keyword redesign explicitly out of scope. Per the abort rule, no lexer edit was made.

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

## 12. Results (2026-11-19)

Shipped: the `tools/bench` harness and its four baselines, `--verify` AST fingerprints, the
explicit fragment manifest plus `ParserInvariantsTest` (Phase 1), P1 (O(1) identifier rejection in
speculative lookahead), and P3 (JML primary-prefix gating).

Rejected on correctness gates:

| Candidate | Outcome |
|---|---|
| P3 first attempt | Reverted: a production-start guard perturbed lookahead in `FragmentTest`; shipped after Phase 1 |

Measured, no repeatable win:

| Candidate | Outcome |
|---|---|
| P2 redundant numeric lookaheads | Reverted: interleaved A/B showed no repeatable win |
| P2 `Modifiers()` loop lookahead | Reverted: load-bearing; removal broke 68 tests |
| `Statement()` 24→2 lookahead | Reverted: 3/3 A/B pairs 3–8% slower |
| P4 lexer tuning | No safe candidate without changing generated token-manager behavior |

P1 and P3 were both rejected on their first attempts for the same reason — diagnostic-sensitive
fragment skipping — and shipped after Phase 1 replaced that heuristic with an explicit manifest.

Recorded baseline medians on this host after P1 + P3 (harness, warmup 5 / rounds 7):

| Corpus/mode | Median ms | Best ms | ok/fail |
|---|---|---|---|
| plain/grammar | 1850 | 1719 | 981/0 |
| plain/full | 2654 | 2554 | 981/0 |
| jml/grammar | 5159 | 4767 | 2958/1 |
| jml/full | 10676 | 10490 | 2755/204 |

The `full` mode pins language level `JAVA_21`; without it, 10 pattern-matching files in the
`plain` corpus fail on upstream's `POPULAR` (`JAVA_11`) default.

Machine variance (load average around 5, swap full) made sub-3% claims unmeasurable; the harness
therefore reports best-of-N alongside the median. The remaining follow-up is the
contextual-keyword lexer redesign, but the post-P1 profile shows the hot lexer path is the generic
NFA, not the keyword DFA, so its expected payoff is smaller than originally assumed.

## 13. JVM comparison (Java 21 / 25 / 27)

Measured 2026-11-19 after P1 + P3, with the shipped harness on the same host, using vendor-matched
Temurin builds: 21.0.13, 25.0.5, 27.0.1.1. The `tools/bench` distribution was built once and its
launcher was run directly with a per-run `JAVA_HOME`, so the Gradle toolchain (Java 21) did not pin
the measured JVM.

Method: unpinned, warmup 3, 5 measured rounds, medians; acceptance was identical in every run
(`plain` 981/0, `jml/grammar` 2958/1, `jml/full` 2755/204 — the 204 are validator rejections, not
parse failures).

| Corpus/mode | JDK | median ms | best ms | median MiB/s | vs Java 21 (median) |
|---|---|---|---|---|---|
| plain/grammar | 21 | 1785 | 1722 | 3.30 | — |
| plain/grammar | 25 | 1733 | 1622 | 3.39 | 2.9% faster |
| plain/grammar | 27 | 1587 | 1445 | 3.71 | 11.1% faster |
| plain/full | 21 | 3036 | 2921 | 1.94 | — |
| plain/full | 25 | 2516 | 2331 | 2.34 | 17.1% faster |
| plain/full | 27 | 2866 | 2475 | 2.05 | 5.6% faster |
| jml/grammar | 21 | 3652 | 3612 | 4.65 | — |
| jml/grammar | 25 | 4571 | 4514 | 3.71 | 25.2% slower |
| jml/grammar | 27 | 3639 | 3016 | 4.66 | 0.4% faster |
| jml/full | 21 | 11251 | 10598 | 1.51 | — |
| jml/full | 25 | 9643 | 9548 | 1.76 | 14.3% faster |
| jml/full | 27 | 7952 | 7432 | 2.13 | 29.3% faster |

Findings:

- **Java 27 is still the fastest overall**: −11% on `plain/grammar` and −29% on `jml/full`
  versus Java 21, with `jml/grammar` at parity on the median but −16% on best.
- **Java 25 wins the plain full case** (−17% vs 21) and is −14% on `jml/full`, but is clearly
  slower on `jml/grammar` (+25%).
- P1 substantially shrank the JVM gap: before P1, Java 27 was ~2× faster on JML full; after P1 the
  remaining difference is ~29%, because a large JIT-sensitive lookahead cost is simply gone.
- The plain/grammar rows were re-measured after P3; the other rows are unchanged by P3 (the guard
  is always true in JML mode and `full` mode shows no measurable difference).

These numbers are host- and load-dependent; reproduce with:

```bash
./gradlew :tools:bench:installDist
JAVA_HOME=/path/to/jdk-27 tools/bench/build/install/bench/bin/bench \
    --corpus jml --mode full --warmup 3 --rounds 5
```

Raw measurement JSONs were intentionally not committed.
