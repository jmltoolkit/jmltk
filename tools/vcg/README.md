# Verification Condition Generator (VCG)

This module implements a verification condition generator for Java programs annotated
with JML. It translates a method or constructor and its contract into a set of SMT
formulas that can be checked by an SMT solver (e.g. Z3, via `:tools:smt`).

## Capabilities

* `Vcg` — entry point. Given a `VcgContext` (a `MethodDeclaration` /
  `ConstructorDeclaration` plus its merged `JmlContract`) and a `VcgOptions`, it
  produces a `VcgResult`: an `SmtQuery` with one check-sat block per verification
  condition, annotated with `:named` attributes and source `Range`s.
* `VcgResult.check()` / `failedConditions()` — run a solver and classify every
  condition as `PROVEN` / `FAILED` / `UNKNOWN`.
* Bounded (`VerificationMode.BOUNDED`, Java overflow via bit-vectors) and unbounded
  (`VerificationMode.UNBOUNDED`, mathematical integers) data types.
* Loops: unrolling (with a global default depth and per-loop overrides) or abstraction
  by invariant / loop contract (including `decreases`, `breaks`, `continues`).
* Method calls: inlining or replacement by the callee's JML contract (assert
  precondition, havoc assignable locations, assume postcondition).
* Exceptions: `try/catch/finally` and `signals` are encoded via abrupt-completion
  flags.
* Java type hierarchy: `T`/`U` sorts with `subtype`/`instanceof`/`cast`/`typeof`;
  instance fields read/written on arbitrary receivers via per-field heap selectors.
* Optional runtime checks (`VcgOptions`): division-by-zero, array bounds, and signed
  arithmetic overflow.

## CLI

The `jmltk` CLI exposes a `vcg` subcommand:

```
jmltk vcg --unbounded [--loop-invariant|--loop-unroll N] [--check-division] \
          [--check-index] File.java ...
```

Parses the files with symbol resolution, runs the VCG on every JML-annotated
callable, and prints a per-condition line (`ok` / `FAIL` / `????`) followed by a
summary.

## LSP

The language server exposes a `Verify method (VCG)` code-lens on JML-annotated
methods (`jml.verify.vcg`). Activating it runs the generator and reports the
`proven / failed / unknown` counts (and failing condition descriptions) to the
client.

## Module layout

| File | Purpose |
| ---- | ------- |
| `Normalizer.kt` | Normalises a statement block into three-address form (simple assignments, desugared loops/switch, JML assert/assume) |
| `ir/NfStmt.kt` | The normal-form IR nodes |
| `Vcg.kt` | The SP-based condition generator over the normal form |
| `ExprTranslator.kt` | (JML) expression to SMT, against the current SSA environment |
| `VcgOptions.kt` / `VcgContext` | Configuration and task context |
| `VcgResult.kt` | `VerificationCondition` + solver checking |
