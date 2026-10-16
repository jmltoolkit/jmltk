# Closing JML/Java semantic gaps in VCG — work plan

Status: planned (2026-10-16). One commit per item, each with regression tests, pushed to `weigl/vcg` (PR #69). The dependency-bump PR #74 stays untouched. Update the "Known semantic divergences" section of the PR description as items land.

## Phase 0 — Establish a regression baseline (prerequisite)

- Run `./gradlew :tools:vcg:test` and record the PROVEN/FAILED/UNKNOWN counts per test class (per-condition output carries source `Range`s).
- Add a golden-query harness (dump the `SmtQuery` for selected examples) so refactors can diff generated SMT and prove "only the intended obligations changed".
- Acceptance: full suite green; clean tree after committing the baseline.

## Phase 1 — Soundness bugs (proves things Java/JML wouldn't)

### P1.1 Multiple spec cases: disjunctive JML semantics
Where: `VcgContext.of` (`tools/vcg/src/main/kotlin/io/github/jmltoolkit/vcg/VcgOptions.kt`, ~99), `contractCall` (`Vcg.kt`, 2417+), `checkPostcondition`/`assumeRequires` (`Vcg.kt`, 503–550).
Steps:
1. Stop merging clauses; keep a `List<JmlContract>` of unrolled cases on `VcgContext`.
2. Self-verification: one obligations block per case — assume `requires_i`, prove `ensures_i` (plus shared assignable; per-case vs. union per JML).
3. Call sites: assume `ensures_i` guarded by that case's `requires_i` (`requires_i -> ensures_i`) against the post-havoc state, not the current "all preconditions, all postconditions".
4. Empty `requires` case is the true fallback.
Tests: two-case callee with distinct ensures; one new regression that must now fail, one that must still prove.
Effort: medium. Impact: high (currently unsound at call sites).

### P1.2 Binary numeric promotion in BOUNDED mode
Where: `atom` `BinaryExpr` (`Vcg.kt`, 1982–1986), `fnApplyOverflow` (`Vcg.kt`, 2204), compound-assign path.
Steps:
1. Sign/zero-extend `BV8`/`BV16` operands (byte/short/char) to `BV32` before binary ops (char zero-extends; byte/short sign-extend).
2. Arithmetic at `BV32`; truncate on assignment back to byte/short/char LHS (Java wrapping cast).
3. Keep `fnApplyOverflow`'s exact-width check consistent with the promoted width.
Tests: `(byte)127 + (byte)1 == 128`; byte multiply overflow at int precision; `b += 300`; char arithmetic — all BOUNDED mode.
Effort: small–medium. Impact: high (wrong arithmetic today).

### P1.3 Unresolvable callee: fail loudly or frame soundly
Where: `handleCall` (`Vcg.kt`, 2278+) / `contractCall` decl==null branch (`Vcg.kt`, 2455–2457).
Steps:
1. Default: reject with a clear error ("callee has no JML contract and no body") instead of returning an unconstrained result.
2. Optional `--allow-unrefined-calls`: then at least havoc the caller-visible frame (`this.*`, arrays, field maps — reuse the `\everything` havoc path, line 2573).
Tests: existing suite stays green; new test expects the error for an unresolved call.
Effort: small. Impact: turns silent unsoundness into a loud diagnostic (or a bounded one).

## Phase 2 — Completeness (fails to prove things it should)

### P2.1 Constructor contracts at creation sites
Where: `ObjectCreationExpr` branch (`Vcg.kt`, ~2051); reuse `contractCall` (`Vcg.kt`, 2417).
Steps:
1. Resolve the constructor declaration (mirror `handleCall`); P1.3 policy if unresolvable.
2. Fresh `this` term; assert `requires` (guarded), havoc assignable (`this.f`, arrays), assume `ensures` + established invariant (fold like `checkPostcondition`, 532–537).
3. Reuse `calleeView`/`bindReceiver` with the fresh object as `this`.
Tests: `new Counter(5)`; constructor `requires` failure at the creation site; constructor-established invariant usable by caller.
Effort: medium (mostly reuse). Impact: medium-high.

### P2.2 Field/static-final initializers (partial)
Where: `setupContext` (`Vcg.kt`, 336+) / field declarations in `VcgFacade`.
Steps:
1. Bind instance fields with literal/`static final` constant initializers to their value in the initial env (not havoced).
2. Constant-foldable initializers only (literals, `static final` refs); everything else stays unconstrained (documented).
3. Optional: fold `static final` constants into reads.
Tests: a previously-unprovable method now proves; non-constant initializer test stays unconstrained.
Effort: small–medium. Impact: medium.

## Phase 3 — Diagnostics & explicit-limiting (cheap wins)

### P3.1 Multi-dimensional & reflective arrays: reject instead of mis-model
Where: `ArrayCreationExpr` branch (`Vcg.kt`, 2084+) + `arraySmtType` (`Vcg.kt`, 2253).
Steps: if `e.levels.size > 1`, emit "multi-dimensional arrays unsupported" (feature flag to widen later). Also reject reflective paths if not modelled.
Effort: tiny. Impact: silent mis-modelling becomes a clear error.

### P3.2 Unsupported constructs → structured diagnostics
Where: validation pass at `verify()` entry / `Normalizer`.
Steps: curated "unsupported: <construct>" errors with source `Range` (switch expressions, lambdas, anonymous classes, enums, `\fresh`/`\reach`/`\TYPE`-keywords, assert-with-message, …). Single `UnsupportedFeatureException`.
Effort: small. Impact: developer triage speed.

### P3.3 Runtime checks: document + default policy
Where: `VcgOptions`, CLI `vcg` subcommand, LSP.
Steps: decide and document the default set (`checkNull/checkIndex/checkDivision`/…); add `--checks=all|none|default`; align LSP; update README + PR description bullet.
Effort: tiny.

## Phase 4 — Design-level changes (decide first, then implement)

### P4.1 Visible-state class invariants
Where: `contractCall` (pre/post call), `inlineCall`, `isHelperDecl`.
Steps (per earlier sketch): pre-call receiver/`this` invariant **check**, post-call **assume** (minimal sound variant); skip `\helper` callees; extend `needsEarlyReceiver` to share receiver evaluation; constructor call sites assume established invariant (ties into P2.1).
Tests: break invariant mid-method before a call → must fail; KeY binary-search stays green.
Effort: medium. Impact: high (largest semantic decision).

### P4.2 Array nullability model
Where: array value-map model (`Vcg.kt`, 883–899, 1987–2007).
Steps: design decision — (a) distinguished `null` array constant + explicit null checks, (b) nullable reference encoding, or (c) keep never-null as a documented approximation. Only commit to (a)/(b) after a test spike showing the impact on the 125-case suite.
Effort: high. Impact: medium.

### P4.3 Strings
Where: `stringLengthOf`, `stringIndexChecker`, `isStringLike`.
Steps: axioms for `equals`, `charAt`, `concat`, `indexOf` over the uninterpreted string sort (bounded length), gated behind the existing string-index option. Likely last — unblocks `String`-heavy methods.
Effort: high.

## Execution order
1. Phase 0 baseline → P1.1 → P1.2 → P1.3, one commit each, rerun suite, push to `weigl/vcg`.
2. P2.1 → P2.2, then Phase 3 (diagnostics).
3. P4.1, then decide P4.2/P4.3 (likely separate PRs + documented limitation).

## Open decisions
- P1.3: reject-by-default or `--allow-unrefined` best-effort?
- P3.3: which runtime checks on by default?
- P4.2: "arrays never-null" acceptable as a documented approximation, or must it be fixed?
