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

### P3.1 Multi-dimensional & reflective arrays: reject instead of mis-model — DONE
Where: `ArrayCreationExpr` branch (`Vcg.kt`, 2523+).
Implementation: `new int[n][m]` and `new int[n][]` (any `e.levels.size > 1`) now
raise `UnsupportedFeatureException("unsupported: multi-dimensional array
creation …")` carrying the node's source `Range`. Before, the model read only the
first dimension off the levels and silently collapsed the element sort to a 1-D
array of the base type.
Reflective array construction (`java.lang.reflect.Array.newInstance`) needs no
extra guard: it resolves to an external call without contract/body and is
already rejected by P1.3 (or over-approximated under `--allow-unrefined-calls`).
Also introduces `io.github.jmltoolkit.vcg.UnsupportedFeatureException`
(extends `UnsupportedOperationException`, carries a `Range`) as the single
diagnostic type for constructs the VCG cannot model (P3.2 routes the remaining
ones through it).
Tests: `testMultiDimArrayGenerationRejected` covers `multiDim`
(`new int[n][n]`) and the jagged `multiDimJagged` (`new int[n][]`), asserting
the message and the source range. Goldens unchanged — no existing fixture or
golden case uses multi-dimensional arrays.
Effort: tiny. Impact: silent mis-modelling becomes a clear error.

### P3.2 Unsupported constructs → structured diagnostics — DONE
Where: `validateSupported()` at `verify()` entry (`Vcg.kt`) + `Normalizer`.
Implementation: a pre-query validation pass walks the callable (body + JML
contract clauses) and aborts on the *first* construct the VCG cannot model with
`UnsupportedFeatureException("unsupported: <construct> at line N", range)`:
  - switch *expressions* (`SwitchExpr`) — only switch statements are modeled;
  - lambdas, method references, anonymous classes (`ObjectCreationExpr` with a
    body);
  - Java `assert` statements (with or without a message — the supported form is
    a JML `//@ assert` statement);
  - JML keywords outside the supported set (`\fresh`, `\reach`, `\TYPE`, …
    would otherwise be silently mis-modeled or fail with an unrelated resolver
    error). Whitelisted: `\result`, `\old`, `\invariant_for`,
    `\static_invariant_for`, `\everything`, `\nothing`; quantifier/label
    binders (`\forall`, `\exists`, `\sum`, `\lbl`, …) are separate node kinds
    and need no entry;
  - enums: verifying a member of an `EnumDeclaration` is rejected up front (an
    ordinal/constant model would be required).
`UnsupportedFeatureException` (introduced in P3.1) is the *single* diagnostic
type; the `Normalizer`'s generic statement/expression/assignment-target throws
now use it too (it extends `UnsupportedOperationException`, so existing
`assertThrows(UnsupportedOperationException::class.java)` guards stay green).
Fail-fast: the pass runs before any query work.
Tests: `testUnsupportedConstructsRejected` (lambda, method reference, anonymous
class, Java assert, `\fresh`, `\reach`), `testSwitchExpressionRejected` (a
switch expression can only parse at a JAVA_12+ level, so the probe parses a
JAVA_17 snippet — the fixture file stays at the default JAVA_11 level), and
`testEnumVerificationRejected` (the `Color` enum appended to the fixture file —
its new top-level type adds the `sort_color` type constant + subtype edge to
every golden query: add-only, verified by diffing the regenerated goldens).
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

### P4.1 Visible-state class invariants  ✅ DONE
Where: `contractCall` (pre/post call), `inlineCall`, `isHelperDecl`.
Decision (per earlier sketch, consistent with the already-on entry-assume `assumeClassInvariant` and exit-obligation `checkPostcondition`): **a call to a non-helper instance method is a visible state** — the receiver's class invariants are **checked as an obligation before the call** and **re-assumed as an assumption after it**. `this`-receivers fold `\invariant_for(this)` (the enclosing type's statically-visible set); explicit receivers `o.m(...)` fold `\invariant_for(o)` (the receiver's static type), driven by the existing `InvariantFold`/`targetTypeForInvariant` machinery. `\helper` callees are exempt on both sides (helpers may break and need not preserve the invariants); static callees and classes without in-CU invariants add nothing. `needsEarlyReceiver` now also covers the invariant path, so the pre-call check and the contract view share one receiver evaluation. Constructor sites stay as-is (P2.1 already assumes the constructed object's invariant — `new` is not a visible state for the caller's `this`).
Encoding chosen (contract strategy): pre-call obligation `guard -> \invariant_for(receiver)` emitted before the assignable havoc; post-call `emit(guard, \invariant_for(receiver))` against the post-havoc state (a fresh translation reads the updated field maps/flat slots). Inline strategy mirrors it around the spliced body (the callee activation is still a visible state). The callee-side fold happens through `callNeedsInvariant`/`callInvariantTerm`.
Tests: `callWithInvariantHeld` (held → proven), `breakInvariantBeforeCall` (guardrail: broken then restored only *after* the call → falsifiable), `relyOnInvariantAfterCall` (the post-call assume is load-bearing: after an assignable havoc the callee's `ensures` says nothing, yet the `//@ assert` + exit obligation discharge through the re-assumed invariant), `helperCallExempt` (helper call unchecks), `receiverInvariantHeld`/`receiverInvariantBroken` (explicit-receiver variants, holding vs. broken). KeY binary-search stays green; suite grew 277 → 282 semantics cases, all green; 8 new goldens + one substantive drift (`useCountedCtor`: the pre-call check becomes `v3`, the post-call assume is added, the exit postcondition renumbers to `v4`; all other goldens gained only the new `sort_invariantreceiver` declaration line); whole-project compile + spotless green.
Documented limitation: an unconditional `ensures` on an `assignable \nothing` callee contradicts a broken pre-state (ex-falso through the contract axioms) — the visible-state check is evaluated against the same axiom set, so such a callee must make its assignable explicit (`assignable this.counter`) for the pre-call check to be the discriminating obligation (fixtures note this).

### P4.2 Array nullability model  ✅ DONE
Where: array value-map model (`Vcg.kt`), `ExprTranslator` array folding, fixtures/goldens.
Decision (spike measured first: 14 of 260 suite cases failed, all reconciled): **arrays behave like ordinary references.** Reference nullability is driven by the JML modifiers exactly as for objects — `non_null` opts a location out of being `null`; element nullability is the store obligation (elements non-null by default; `nonnullelements` is the explicit statement of that default; a `nullable` element store is the binding tell that cannot be expressed, so it stays a documented deviation).
Encoding chosen: per-sort null-array constant `$null_array_<tag>` (declared lazily), real `(= arr $null_array_<tag>)` for `arr == null`, deref nullchecks on `a[i]` / `a.length` / element stores, `new T[n]` asserted non-null, `non_null` locations fold + entry assumptions (params = implicit requires; fields = type invariant, asserted at entry, not in constructors; locals = store obligations), element-store obligations `e != null` (reads stay unconstrained — documented deviation: folding reads would break fresh-array elements). Object element sorts now print `U` (the declared reference sort, not the never-declared `Object` placeholder) and array lengths use per-sort `bv$length_<tag>`/`int$length_<tag>` functions (SMT-LIB forbids overloading); alias frames only apply to same-sort arrays (Java forbids mixed-sort identity). Anonymous array constants use a per-verification counter so queries are reproducible.
Tests: 8 proven + 2 falsifiable fixtures, `NonNullHolder` (`non_null int[]` field) case, 6 new goldens; suite grew 260 → 277 semantics cases, all green; whole-project compile + spotless green.

### P4.3 Strings  ✅ DONE
Where: `stringLengthOf`, `stringIndexChecker`, `isStringLike`.
Decision (spike measured first: layered axiom sets were triaged on Z3 timing): under the existing `--check-string-index` option the common `String` operations — `equals`, `charAt`/`codePointAt`, `concat`, `indexOf`, `substring`, `startsWith`, `replace(char,char)` — become reads of a small abstract-string model (`steq`/`strval`/`strcat`/`strindex`/`strsub`/`strsub2`/`strpre`/`strrep`) instead of opaque unrefined library calls, so `String`-heavy methods verify. Each modeled operation declares its function and length-bounded axioms once, lazily, per operation (a `charAt`-only query never pays for the `strindex` axioms).
Encoding chosen: `equals` ≈ extensional equality cut by runtime length (both directions, so `s.equals(s)` provable and `s.equals(null)` false — the argument is null-guarded reference); `charAt`/`codePointAt` ≈ per-position character code `(strval s i)` plus the existing `stringIndexChecker` bounds obligation; `concat` ≈ non-null result whose length is the sum of the parts and whose content is the prefix of the first argument then the second (non-null axiom lets `s.concat(t).length()` discharge its deref nullcheck); `indexOf` ≈ `(strindex s c)` = first position of the character, `-1` if absent (completeness + found + absent + minimality axioms, each binding only the variables it reads). The quantified axioms carry explicit `:pattern` annotations (Z3's E-matching, not MBQI, drives them — without this, some satisfiability checks churned to the 60 s watchdog); absence/minimality are flattened to a single universal level with a multi-pattern over `strindex` *and* `strval`, so they fire only when a ground content term is present and satisfiability-only searches stay instant. A `char` search argument upcasts to the mode's int sort in BOUNDED mode (chars are 16-bit bit-vectors there).
Tests: `stringSelfEquals` (reflexivity + null contract), `stringCharAtIndexOf` (`k >= 0` ⇒ `s.charAt(k) == 'x'`, statement-position so the bounds/guard discharge), `stringConcatLen` (chained length reads, both modes), `stringIndexOfAbsent` (requires `indexOf == -1` ⇒ a grounded `charAt(0)` read is provably `!= 'q'`), `stringCharAtUnknown` (unguarded first-position claim stays falsifiable — falsifiable queries cannot carry the `indexOf` axioms, whose MBQI mirroring defeats instant SAT answers); suite grew 282 → 287 semantics cases, all green; 6 new goldens, all other goldens unchanged; whole-project compile + spotless green.
Documented limitation: char-level reads in BOUNDED mode compare 16-bit extracts, so char-equality claims don't follow from the model's 32-bit codepoint inequalities — such fixtures keep reads in statement position or UNBOUNDED mode; and an `indexOf`-axiom query's satisfiability check is only instant when no falsifiable claim rides the same model (the proven directions are unaffected).

#### P4.3 extension: substring / startsWith / replace(char,char)  ✅ DONE
Follow-up to the same model (commit "vcg: model substring/startsWith/replace as abstract-string reads (P4.3)"): the remaining reads that the plan's "known divergences" PR body had listed.
- `substring(i)` / `substring(i,j)` ≈ `strsub`/`strsub2`: non-null slice whose runtime length is the slice width and whose content is the receiver's content shifted by the start index (position-wise, like `strcat`), so slice content reads tickle the same `strval` model. The call-site bounds obligations come from `stringIndexChecker` (`0 <= i` / `i <= j` / `j <= len`).
- `startsWith(String)` ≈ `strpre`: the prefix relation, both directions like `steq` — `pre` ⇒ `len(p) <= len(s)` and position-wise content agreement below `len(p)` (so an assumed `s.startsWith(p)` yields `p.length() <= s.length()` and grounding content facts); the converse (length + content ⇒ pre) proves literal-prefix claims from the literal's bound content. As in Java, the argument is null-checked (`s.startsWith(null)` throws NPE).
- `replace(char,char)` ≈ `strrep`: non-null result of the same runtime length whose content is the receiver's with the replaced character mapped to the replacement (`ite` per position).
- Encoding rule from the `indexOf` spike carried over: **every content axiom is flattened to a single universal level and carries a pattern over the *content-application* term** (`(strval (strsub s$a s$k) s$j)`, `(strpre s$a s$b)` + `(strval s$a s$i)`, `(strval (strrep s$a s$c s$d) s$i)`). A nested index quantifier whose shifted index is the arithmetic `(+ s$k s$j)` cannot be E-matched, so Z3 falls back to model-based instantiation and even a *satisfiable* bounds check (the `substringLen` string-index obligation alone blew the watchdog) churns; the flattened pattern keeps the axiom inert until a ground content read is present.
- String literals now bind their per-position content under `--check-string-index`: `strval(strlit, i) = utf16-code(value[i])` alongside the declared length, so literal-based proofs (`"ab".charAt(i) == 'a'`, `s.startsWith("ab")`, slice reads) discharge through the char reads. Repeated literals alias one constant.
Tests: `substringLen`/`substringLen2` (slice-width length, both modes), `literalStartsWith` (a literal prefix claim proves, both modes), `literalStartsWithBroken` (too-short receiver stays falsifiable), `startsWithLengthBound` (`s.startsWith(p)` ⇒ `p.length() <= s.length()`), `replaceLen` (length-preserving, both modes), `replaceContentAt0` (replaced char reads back the replacement); 4 new goldens (U_STR); whole-project compile + spotless green.

#### P4.3 pivot: native `strContent` in UNBOUNDED mode  ✅ DONE
Follow-up (PR #69 "VCG by AI"): in UNBOUNDED mode the abstract-string functions/axioms are **replaced by a native sequence-theory encoding** — `strContent : U -> (Seq Int)` is the single content view and the modeled operations compile to direct sequence terms:
- `length()` ≈ `(seq.len (strContent s))` (non-negativity is a sequence-theory fact; the `stringNonnegDeclared` axiom and `stringLength` are U-mode dead);
- `charAt`/`codePointAt` ≈ `(seq.nth (strContent s) i)` (bounds obligation unchanged, from `stringIndexChecker`);
- `equals` ≈ `(= (strContent s) (strContent t))` (null-guard at the call site, as before);
- `concat`/`substring(i)`/`substring(i,j)` results are **fresh non-null reference constants** whose content is bound by a ground equality (`(seq.++ …)`, `(seq.extract …)`) — no wrapper function, no quantifier;
- `startsWith` ≈ `(seq.prefixof (strContent p) (strContent s))`;
- `indexOf(String)` ≈ `(seq.indexof (strContent s) (strContent t) 0)` — the P4.3 extension's closure of that read no longer needs the quantified `strfind` ladder in U mode, and the *falsifiable* direction (`stringIndexOfWrong`: the searched needle occurs at 0, the claim says 1) is now served too, because the query is axiom-free by construction and no quantified axiom can poison the instant SAT answer. B mode keeps the quantified `strfind`/`strsub`/`strpre` search ladder (no native sequence sort over bit-vectors): its proven directions stay in the suite, its falsifiable one is not (MBQI must satisfy the axioms while constructing the model, so the check churns);
- string literals bind their whole content natively: `(= (strContent strlit_k) (seq.++ (seq.++ (as seq.empty (Seq Int)) (seq.unit c0)) (seq.unit c1)))`, replacing the per-position `strval` binds.
Two operations keep quantified axioms because the sequence theory cannot express them:
- the single-character search `strindex` keeps its three axioms (result domain / found ⇒ content / absent ⇒ content ≠), with `strval` macroed to `(seq.nth (strContent s) i)` — the native `seq.indexof` cannot derive the content lemmas (`-1 ⇒ content ≠ c` is *satisfiable* to Z3; `⇒ charAt` churns in MBQI);
- `replace(char,char)` stays uninterpreted `strrep` (non-null, same length, position-wise `ite` content map) — `seq.replace_all` has no `(Seq Int)` instance.
Why the pivot (spike-measured on Z3 4.16): **any quantified string axiom poisons instant SAT answers** — MBQI must satisfy them while constructing models, so even trivial satisfiability checks churn. U-mode queries that avoid `strindex`/`strrep` (e.g. `stringIndexOfWrong`: charAt/length/indexOf(String) only) are now axiom-free by construction: the wrong-position claim is falsifiable *and* answers instantly. BOUNDED mode keeps the pre-pivot axiom model untouched (bit-vector arithmetic has no native sequence sort), so none of the B-mode goldens change structurally.
Tests: all P4.3 semantics tests split U/B where the modes diverge (`stringLengthIsModelReadUnderFlag` now asserts the `seq.len` read in U and the `stringLength` read in B); `stringIndexOfWrong` is served in U only (falsifiable, instant — its B-mode run churns in the quantified model and is *not* part of the suite, documented divergence); 5 new U_STR goldens (`stringIndexOf{Len,Empty,FoundSuffix,AbsentSuffix,String}`) + regenerated string goldens; the non-string U goldens changed only cosmetically (the pre-existing eager receiver path blank-declares `strContent` where base declared `stringLength`); whole-project suite + spotless green.
Design direction (recorded, not implemented): boxing compared types into `(Seq (Seq Int))` — `boxIntVal` — would let the substring `indexOf(String)` search stay inside the native theory+this is the follow-up path if case-sensitivity/`regionMatches`-style reasoning is ever needed; out of scope here.

### P4.4 (future) Boxing `boxIntVal`: design direction  📌 RECORDED
The native `strContent` encoding models Java strings as `(Seq Int)` (UTF-16 code units). Boxing the compared type to `(Seq (Seq Int))` (the `boxIntVal` idea) would let a substring search over a *string-valued* view live inside the sequence theory, at the cost of a heavier sort and explicit boxing of every `seq.unit`/`seq.nth`. Not needed for the current P4.3 read set (all reads are single code units or whole content); recorded so the pivot and the axiom models document why `replace` and char-`indexOf` are the only quantified leftovers.

## Execution order
1. Phase 0 baseline → P1.1 → P1.2 → P1.3, one commit each, rerun suite, push to `weigl/vcg`.
2. P2.1 → P2.2, then Phase 3 (diagnostics).
3. P4.1, then decide P4.2/P4.3 (likely separate PRs + documented limitation).

## Open decisions
- P1.3: reject-by-default or `--allow-unrefined` best-effort? **Decided (P1.3): reject-by-default, opt-in `--allow-unrefined-calls`.**
- P2.1: reject unresolvable constructors (P1.3 mirror) or keep the fresh-object approximation? **Decided (P2.1): keep the fresh-object approximation** — unrefined construction is a sound over-approximation, and reads on such objects are already guarded by P1.3 (deviation documented in the P2.1 section).
- P2.2: which entry bindings are sound? **Decided (P2.2): bind only where every legal execution reaches the entry state with the constant** — `static final` everywhere, constant instance initializers in constructors (initializer-before-body), `final` instance constants in methods; mutable static and mutable instance fields are never bound (documented in the P2.2 section).
- P3.3: which runtime checks on by default? **Decided (P3.3): the six exception checks, `checkOverflow` opt-in, `--checks=all|none|default`.**
- P4.2: "arrays never-null" acceptable as a documented approximation, or must it be fixed? **Decided (P4.2): fixed — arrays are ordinary (nullable-by-default) references;** `non_null` opts out, elements are non-null-by-default stores, `nonnullelements` states the default, and a `nullable`-elements modifier set is out of scope (grammar has no such keyword).
