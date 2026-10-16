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

### P2.1 Constructor contracts at creation sites — DONE
Where: `ObjectCreationExpr` branch (`Vcg.kt`, ~2174), `constructorContract`/`findConstructor`/`havocCtorLocation`/`havocCallerKey` (`Vcg.kt`, 2822–3055).
What was implemented:
- `new T(...)` is now a *contract call site* when the constructor is in the
  verification CU and carries a JML contract (`findConstructor` prefers
  `creation.resolve().toAst()`, falls back to a same-CU search by resolved
  qualified name + arity).
- The callee view binds `this` to the fresh object and exposes every
  non-static scalar/object field of the constructed class as
  `select(field-map, fresh)`; the field maps are *declared eagerly* so the
  clause reads and the caller's post-construction reads share the same select
  terms regardless of translation order (a bare/`this.f` read must not fall
  back to the enclosing method's flat `this.f` slot).
- Creation site semantics (mirrors `contractCall`): the caller must establish
  the *disjunction* of the per-case `requires` (obligation); the per-case
  `ensures` are assumed guarded by their own pre-state `requires`; and the
  constructed class's invariant is assumed (`invariant_for(fresh)` — a valid
  constructor contract establishes it, exactly as `checkPostcondition` folds it
  into the constructor's own verification).
- Assignable: the fresh object's own state (`this.f`, bare fields) is already
  unconstrained, so only *aliasable* caller-visible state named in the union of
  the assignable clauses is refreshed (receiver fields via their map entries,
  array slots via fresh versions, scalar params via fresh constants). No
  assignable / `\everything` (the JML default) refreshes the reachable frame —
  tested fixtures carry explicit assignables.
- **Documented deviation from the original sketch (step 1, "P1.3 policy if
  unresolvable")**: unresolvable constructors (library classes such as
  `new Object()`/`new ArrayList<>()`, implicit default constructors, contrary
  in-CU constructors) are **not** rejected — they keep the fresh-object model
  with unconstrained fields. Rationale: rejecting would break construction of
  every library/plain class, and unlike an unrefined *call* (an uninterpreted
  result value whose side effects are unseen), an unrefined *construction*
  yields a fresh object with unconstrained fields — a sound over-approximation;
  any later method call on it is already guarded by P1.3. This mirrors the
  P1.3 carve-out that contractless-with-body callees keep the old (havoc)
  behavior.
- Also fixed along the way: locals now carry their resolved declared type as
  meta-information on the SSA term (`flatWrite`), so `bindReceiver` can expose
  the fields of a *local receiver* (`o.m()` with `o` a local) — previously the
  callee-view field reads fell back to an uninterpreted constant, a latent gap
  that only surfaces now that local receivers hit contract calls.
Tests: `useCountedCtor` (proves in both U and BOUNDED — `new Counted(5)` then
`c.get()` reads count through the same field map); `ctorRequiresViolated`
(falsifiable "precondition of constructor" obligation at the creation site);
`ctorInvariantUsable` (assert `c.count >= 0` discharges only through the
assumed class invariant, since the tested constructor pins nothing via
ensures). Golden guardrails: `useCountedCtor__U_CONTRACT`, `ctorInvariantUsable__U`.
Effort: medium. Impact: medium-high.

### P2.2 Field/static-final initializers (partial) — DONE
Where: `setupContext` (`Vcg.kt`, 336+) / field declarations in `VcgFacade`.
Steps:
1. Bind instance fields with literal/`static final` constant initializers to their value in the initial env (not havoced).
2. Constant-foldable initializers only (literals, `static final` refs); everything else stays unconstrained (documented).
3. Optional: fold `static final` constants into reads.
Tests: a previously-unprovable method now proves; non-constant initializer test stays unconstrained.
Effort: small–medium. Impact: medium.

Implemented (2026-10-16):
- **Constant folder** (`foldFieldConstants` + `foldFieldConst`): folds only JLS-style
  constant expressions in *textual declaration order* (JLS 8.3.3 — an initializer
  may only fold through *earlier*-declared fields, so forward references never
  leak a value Java would still see as the default). Supported shapes: decimal /
  hex / octal / binary int literals (JLS 3.10.1 typing incl. two's-complement
  hex literals like `0xffffffff`, `int` vs `long` classification), `long`
  literals, `char`/`boolean` literals, unary `+ - ! ~`, `+ - * / % << >> & | ^`,
  comparisons and `&& || == !=`. Everything else (method calls, casts,
  `>>>` — its value differs between the two arithmetic modes —, non-constant
  reads, forward references) stays unconstrained; the folder never approximates.
- **Intermediate-overflow discipline**: every intermediate result must lie in the
  signed range of its promoted type (int: `2^31`, long: `2^63`), so the folded
  value never wraps — it is the exact Java value in *both* modes (BOUNDED's
  bit-vectors wrap on `store`-style ops, UNBOUNDED's `Int` never does; a pin with
  a wrapped value would be mode-dependent and unsound in one of them). `x / 0`,
  `MIN / -1` and wrapping shifts (`1 << 31`) are left unconstrained.
- **Entry binding rules** (`bindFieldConstant`) — a constant initializer pins the
  `this.<f>` / bare slot only where *every* legal execution reaches the entry
  state with that value:
  - `static final` constants: bound for every callable (class-init runs first,
    `final` forbids later writes) — this also delivers "fold constants into
    reads" for the enclosing class;
  - any constant instance initializer: bound when verifying a *constructor*
    (JLS 12.5: initializers run before the body) — e.g. an empty default ctor
    now establishes `count == 0` and the class invariant through the folded
    initializer alone;
  - `final` instance constants: additionally bound for instance methods (no
    constructor can rewrite a `final`-with-initializer field);
  - never bound: mutable `static` fields (another method may have written them
    before entry), mutable instance fields in methods (a competing constructor
    may have overwritten them), reference-typed fields (unconstrained slots).
- **Pins are SSA-first-class**: they assert on the base entry constant, so later
  writes version the slot normally, `\old` sees the pinned value, and the
  identity-keyed field-map anchor (`select(base, this) == this.<f>`) carries the
  constant into alias reads for free.
Tests: `p22StaticFinalBlocks` (`static final` arithmetic chain proves — was
unprovable), `p22ConstantShapes` (minus/hex/char/long/comparison shapes, U+B),
`p22NonConstInit` (method-call initializer stays unconstrained — falsifiable),
`p22ForwardRefInstance` (JLS textual order: stays unconstrained, falsifiable);
`ConstantInitCtor` fixture: default ctor proves `count == 0` + `invariant count
>= 0` from the folded initializer, `capacity()` reads the `final` instance
constant, `countAfterAnyCtor` stays falsifiable (mutable instance field is NOT
folded into methods — the `(int n)` ctor overwrites it, soundness boundary).
Golden guardrail: `p22StaticFinalBlocks__U` (shows `(= P22_BLOCK 1024)` /
`(= P22_TWO_BLOCKS 2048)` pins; `P22_DYNAMIC` / `p22FwdInstance` explicitly
absent from the pins). 36 existing goldens regenerated — only the new type
constant, the new field slots/pins and the two new instance slots havoced by the
`\everything`-contract frame.
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

### P3.3 Runtime checks: document + default policy — DONE
Where: `VcgOptions`, CLI `vcg` subcommand, LSP.
Steps: decide and document the default set (`checkNull/checkIndex/checkDivision`/…); add `--checks=all|none|default`; align LSP; update README + PR description bullet.
Effort: tiny.
Decision (user, 2026-10-16): default-on for the six runtime-exception checks
(`checkNull`, `checkDivision`, `checkIndex`, `checkCast`, `checkNegativeArraySize`,
`checkStringIndex`); `checkOverflow` stays opt-in (BOUNDED wrap-around already matches
Java; the check only flags wraparounds, and is vacuous in UNBOUNDED). Implementation:
`VcgOptions` defaults flipped; `--checks=all|none|default` selector + missing
`--check-overflow` flag on the CLI; the LSP inherits the policy via the `VcgOptions`
defaults. Supporting modeling fixes that default-on checks exposed:
- `new T(...)` results are axiomatized non-null (Java `new` never returns null), so a
  default-on `nullcheck` on a `new T().m()` receiver proves;
- the first store to an array-typed *receiver* field (`h.vals[i] = v`) initializes the
  flat slot from the field-selector read (`field__vals(h)`) instead of a fresh
  unconstrained array, so a default-on index check discharges against a
  `requires h.vals.length >= n`;
- `stringLength` is now declared lazily at first use (the string-index feature is part
  of the default set, but string-free queries must not carry the declaration).
Tests: `testRuntimeChecksOnByDefault` (replaces `testRuntimeChecksOffByDefault`) +
`testRuntimeChecksOffWithNonePolicy` (opt-out still silent); strengthened
`arrayStoreInLoop` requires (`a.length >= 3`); regenerated 7 golden snapshots that
gained only the new default-on obligations plus obligation-id renumbering.

## Phase 4 — Design-level changes (decide first, then implement)

### P4.1 Visible-state class invariants
Where: `contractCall` (pre/post call), `inlineCall`, `isHelperDecl`.
Steps (per earlier sketch): pre-call receiver/`this` invariant **check**, post-call **assume** (minimal sound variant); skip `\helper` callees; extend `needsEarlyReceiver` to share receiver evaluation. (Constructor call sites already assume the constructed class's invariant qua P2.1.)
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
- P1.3: reject-by-default or `--allow-unrefined` best-effort? **Decided (P1.3): reject-by-default, opt-in `--allow-unrefined-calls`.**
- P2.1: reject unresolvable constructors (P1.3 mirror) or keep the fresh-object approximation? **Decided (P2.1): keep the fresh-object approximation** — unrefined construction is a sound over-approximation, and reads on such objects are already guarded by P1.3 (deviation documented in the P2.1 section).
- P2.2: which entry bindings are sound? **Decided (P2.2): bind only where every legal execution reaches the entry state with the constant** — `static final` everywhere, constant instance initializers in constructors (initializer-before-body), `final` instance constants in methods; mutable static and mutable instance fields are never bound (documented in the P2.2 section).
- P3.3: which runtime checks on by default? **Decided (P3.3): the six exception checks, `checkOverflow` opt-in, `--checks=all|none|default`.**
- P4.2: "arrays never-null" acceptable as a documented approximation, or must it be fixed?
