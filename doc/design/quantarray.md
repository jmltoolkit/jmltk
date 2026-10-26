# Design: Quantifiers over arrays and collections (`weigl/quantarray`)

Status: proposal **v4** (scope reduced), not yet implemented.
History: v1 single `Expression domain` field (no per-variable domains) → v2 abstract
parent + reparenting of `VariableDeclarator` (too much core ripple) → v3
`JmlQuantifiedVariable extends VariableDeclarator` with per-variable iterables →
**v4 defers the index+element multi-binding part** (kinds 5/6: `int i : a[i]`,
`T e : a[i]`, zip) by decision. The AST keeps the per-binding node, so re-enabling
them later is grammar/validator work only — no AST migration.
Branch: `weigl/quantarray` (from `main`).

## 1. Problem

JML quantifiers only bind free variables (JML RefMan §12.4.24.2). To talk about "all
elements of an array" one must invent an index, restate the index bounds, and index
manually:

```java
//@ ensures (\forall int i; 0 <= i && i < a.length; a[i] != null);
```

This is verbose, off-by-one-prone, and does not even work for collections (a
`SequencedCollection` has no `length`; one would have to model its elements as a
`\seq` first). Yet a large share of array/collection specifications are of the form
"every element satisfies P" / "some element satisfies P" / "sum of elements is v".

Empirical evidence that users reach for a different syntax: on Stack Overflow
(3946545, "How to specify a requirement of an array with crescent elements") the
asker intuitively wrote `(\forall int i : array[i] < array[i+1])` — a for-each style
binder that JML does not support.

**Goal:** element-oriented quantification over

- arrays `T[]` (any dimension; the element of `T[][]` is a `T[]`),
- `java.lang.Iterable` / `Collection` / `java.util.SequencedCollection`
  (`List`, `Set`, `Deque`, …) — anything with an iterator,
- integer index ranges `lo..hi` as single-binding domains (kept, §2.1),
- uniformly for all generalized binders `\forall \exists \num_of \sum \product
  \min \max \choose` (and, for free, `\bsum`, `\infinite_union`, `\seq_def`).

## 2. Syntax

Accepted: the **for-each binder with `:`**. One quantifier binds **one** variable to
an iterable domain:

```java
(\forall int e : a; e > 0)                        // elements of an array
(\exists T e : c; e.isEmpty())                    // elements of an Iterable/SequencedCollection
(\sum int e : a; e * e)                           // sum of squares
(\num_of String s : words; s.length() > 3)        // count words longer than 3
(\forall var e : a; e != null)                    // 'var' = inferred element type
(\forall int e : a; e != null; e.id > 0)          // filter clauses before the body
(\forall int i : 0..a.length-1; a[i] <= a[i+1])   // single range domain (kept, §2.1)
```

**Deferred (dropped for now):** index+element bindings —
`(\forall int i : 0..a.length-1, T e : a[i]; P(i, e))`, self-index
`(\forall int i : a[i], T e : a[i]; …)`, 2-D compositional forms, zip over several
arrays, value bindings. These fail cleanly at parse today (§5) and later need only
grammar + validator work; the AST is already prepared (§3.1).

### 2.1 Range domains (kept — not the dropped index+element pair)

A single binding whose iterable is `lo..hi` (the existing `RangeExpr`) iterates the
half-open integer interval `[lo, hi)`. It is the fallback for relational properties
(sortedness, monotonicity) after the index+element deferral, costs no extra
machinery (the domain is just an `Expression`), and is trivially droppable if
unwanted — one kind branch (§7.1).

## 3. AST & grammar design

### 3.1 Node

One new JML node; no changes to any core node:

```java
package com.github.javaparser.ast.jml.expr;

/** A quantifier binding: a VariableDeclarator, optionally bound to an iterable domain. */
public class JmlQuantifiedVariable extends VariableDeclarator {
    @OptionalProperty
    private Expression iterable;    // null = classic quantified variable
}

public class JmlQuantifiedExpr extends JmlExpression {
    private JmlBinder binder;
    private NodeList<VariableDeclarator> variables;   // UNCHANGED type — may now
                                                      // contain JmlQuantifiedVariable
    private NodeList<Expression> expressions;         // filters + body/value, unchanged
}
```

- The parser produces a `JmlQuantifiedVariable` **only when an iterable is present**;
  classic bindings stay plain `VariableDeclarator`s. The classic form is
  AST-identical to today.
- The per-binding node is deliberately kept although v4 binds only one variable: the
  deferred multi-binding forms (§2) then require only grammar + validator work, no
  AST migration.
- The is-a is defensible: a bound binding is declarator-shaped (type + name) and
  lives only inside `JmlQuantifiedExpr`. Precedent for generated JML casting on core
  classes exists (`Expression.isJmlQuantifiedExpr()` today; the generators will add
  `VariableDeclarator.isJmlQuantifiedVariable()` the same way).
- `initializer` is inherited but forbidden on bound bindings (validator); classic
  behavior (where an initializer parses today) is untouched.
- `NodeWithVariables` **stays implemented** on `JmlQuantifiedExpr` (it is hard-typed
  to `NodeList<VariableDeclarator>`, `nodeTypes/NodeWithVariables.java:21–25`), so
  the `MAXIMUM_COMMON_TYPE` printing machinery survives for classic lists.

### 3.2 Metamodel / generated code

- Add `JmlQuantifiedVariable` to `ALL_NODE_CLASSES` in `MetaModelGenerator` (after
  `VariableDeclarator`; the generator requires parents before children and derives
  the metamodel parent from the superclass, `NodeMetaModelGenerator.java:72–74`) →
  `JmlQuantifiedVariableMetaModel extends VariableDeclaratorMetaModel` with the new
  `iterable` property. `JmlQuantifiedExprMetaModel` and `ObservableProperty.VARIABLES`
  are unchanged; a new `ObservableProperty.ITERABLE` is generated.
- Run `./scripts/run-generators.sh`: regenerates metamodels, `ObservableProperty`,
  and `visit(JmlQuantifiedVariable)` in all visitors.

### 3.3 Grammar (`javaparser-core/src/main/javacc/java.jj`, `JmlPrimaryPrefix()` ~4725)

With the multi-binding part deferred, the grammar simplifies back to a single
optional iterable after the declarator list:

```java
// today
"(" binder vars=QuantifiedVariables() ";" exprs… ")"

// v4
"(" binder vars=QuantifiedVariables() [ ":" iterable=Expression() ] ";" exprs… ")"
```

- LL(1): `;` (classic) vs `:` (element) after the declarator list — verified, §5.
- The grammar action requires exactly one declarator when an iterable is present
  (`ParseException` with a fix-it message: "an iterable domain binds exactly one
  variable; nest quantifiers for more") and wraps that declarator in a
  `JmlQuantifiedVariable`. So `(\forall int i, j : 0..n; …)` fails at parse, never
  producing an ambiguous AST.
- Classic comma lists (`(\forall int i, j; …)`) and the shared leading-type behavior
  (`VariableDeclarator(Type partialType)`, java.jj:2581–2590) are untouched.
- `var` parses as a `ClassOrInterfaceType("var")`; the grammar action normalizes it
  to `VarType` ("infer from the binding kind").
- No new tokens → no `TokenTypes` changes, no lexer-rule reordering (per
  `AGENTS.md`). Optional stretch (untyped first binding `(\forall e : a; …)`) needs
  syntactic LOOKAHEAD `IDENTIFIER ":"` — deferred.

### 3.4 Why this shape — design comparison (ripple analysis)

| | v2: abstract parent + reparent | mimic class | two parallel lists | **v3/v4: extend `VariableDeclarator`** |
|---|---|---|---|---|
| `variables` field type | changes | changes | unchanged | **unchanged** |
| `NodeWithVariables` | must be dropped | must be dropped | stays | **stays** |
| Classic path (e.g. `JmlQuantifiedExprContext.solveSymbol` → `quantifiedVar(VariableDeclarator, …)`, JmlQuantifiedExprContext.java:34–40) | breaks, switch needed | breaks for **all** forms | untouched | **untouched** |
| Metamodel | parent of `VariableDeclaratorMetaModel` flips globally | one new metamodel | one new metamodel | **one new child metamodel, no flips** |
| Core-node impact | reparents the most-used node | none | none | **none** |
| Consumer logic | `instanceof` over 2 classes | null-check everywhere incl. classic | second list + exclusivity validator | **`instanceof` only where iterables matter** |
| Boilerplate | abstract + concrete node | duplicates type/name/initializer accessors | extra list plumbing | **one small class, accessors inherited** |

The mimic variant (a standalone `JmlQuantifiedVariable` duplicating `VariableDeclarator`'s
shape plus the iterable) is the purest JML placement, but it breaks the classic
representation: `quantifiedVar(VariableDeclarator, …)` (JavaParserSymbolDeclaration.java:36),
the context loop above, every tool iterating `getVariables()`, and the round-trip tests
all need rework — churn concentrated on the classic path we want to keep stable. The
two-lists variant is the conservative fallback if the is-a relationship is rejected.

### 3.5 Printing

- `DefaultPrettyPrinterVisitor.visit(JmlQuantifiedExpr)` (~914) and
  `ConcreteSyntaxModel` (~1318): if the list contains a `JmlQuantifiedVariable`,
  print it as `Type name : iterable`; otherwise keep the current classic printing
  (common type + names) unchanged.
- New CSM entry for `JmlQuantifiedVariable` itself (type, name, optional
  `: iterable`).

### 3.6 Validators

- an iterable binds exactly one declarator (defensive; the parse already enforces
  it);
- **the iterable must not reference the bound variable** (`(\forall int e : a[e]; …)`
  is an error — the self-index sugar is deferred, §2);
- **binding kinds** are decided by the iterable (§4) and re-checked:
  - range iterable → declarator must be of integer type (`var` → `int`);
  - array generator → declarator type must match the component type (or `var`);
  - Iterable generator → declarator type must match the element type (or `var`);
- a bound binding must not have an initializer;
- the iterable must be pure (side-effect free) — WD, §4.

### 3.7 Symbol solver

`JmlQuantifiedExprContext.solveSymbol` (javaparser-symbol-solver-core,
`contexts/jml/JmlQuantifiedExprContext.java:34–40`) keeps working unchanged — bound
entries are `VariableDeclarator`s. Addition: infer the static type per binding kind:

| binding kind (§4) | static type of the bound variable |
|---|---|
| 1 classic | declared type (as today) |
| 2 range `lo..hi` | `int` |
| 3 array generator `A` | component type (element of `T[][]` is `T[]`) |
| 4 Iterable generator | first type argument of `Iterable<T>` |

`JmlJavaParserQuantifiedVariableDeclaration` is reused as-is (it wraps a
`VariableDeclarator`).

## 4. Semantics

A quantifier `(\binder b; F1; …; Fn)` is interpreted as today; the binding `Type x
[: E]` gets one of four kinds:

1. **classic** (no `E`): unconstrained variable, restricted only by the filter
   clauses `F1..Fn-1` — unchanged classic JML.
2. **range generator** (`E` is `lo..hi`, `BinaryExpr(Operator.RANGE)`): `x` iterates
   the half-open integer interval `[lo, hi)`.
3. **array generator** (`E` array-typed, not an array access): `x` iterates the
   elements of `E` (for `T[][]`, elements are `T[]`).
4. **Iterable generator** (`E` assignable to `Iterable<T>`): `x` iterates the
   elements in iteration order (specifications should be order-independent; the
   order-dependent binders `\seq_def`/`\bsum` are defined over the iterator order).

Deferred kinds (index+element): self-index generator (`int i : a[i]`) and value
binding (`T e : a[i]`) — see §2 and §7.2.

Definitional equivalences (all binders; parts before the last remain conjunctive
filters, the last part is the body/value):

- kind 2: `(\binder int x : lo..hi; P…)` ≡ `(\binder int x; lo <= x && x < hi; P…)`
- kind 3 (array), fresh `_i`:
  `(\binder T x : a; P…)` ≡ `(\binder int _i; 0 <= _i && _i < a.length; P[x := a[_i]])`
  (pure substitution; alternatively bind via the existing `JmlLetExpr`)
- kind 4: quantification over the (finite) element sequence of the iterator;
  formal model deferred until the KeY `\seq` machinery is revived (java.jj ~8002)

**Well-definedness:**

- the domain expression is evaluated **exactly once**, must be pure (side-effect
  free) and, for arrays/Iterables, **non-null**; a null domain makes the quantifier
  undefined (WD violation — consistent with the classic form whose range would
  dereference `a.length`; vacuous-true rejected: it hides null bugs);
- empty iteration domain: `\forall`→true, `\exists`→false, `\num_of`→0, `\sum`→0,
  `\product`→1, `\min`/`\max`/`\choose`→undefined (same as classic JML on an empty
  range).

## 5. Facts verified against the current build (Oct 2026)

- `(\forall int i; 0 <= i < 5; a[i])` parses via `JavaParser.parseJmlExpression`
  (note `JmlMultiCompareExpr` chained comparison already works).
- `(\forall int e : a; e > 0)` fails with `Encountered unexpected token: ":"` right
  after the declarator list — the extension point is clean and LL(1).
- The deferred forms fail today as well: `(\forall int i, T e : a[i]; …)` fails on
  the second declarator type (`QuantifiedVariables`, java.jj:4743–4754, binds one
  `Type` to the whole list); per-binding types will be needed again when
  re-enabling multi-binding (§7.2), not in v4 scope.
- `0..5`, `n..m`, `a[0..5]` all parse as `BinaryExpr(Operator.RANGE)` and print as
  `0 .. 5` etc. The feared float-literal tie (`0.` is a legal Java float) does not
  bite in this grammar; existing fragments already use `students[0..n]`
  (`jmlparser-jml-tests/.../fragments/stmt_1132627598.txt`). `RangeExpr` sits below
  `RelationalExpression` (java.jj:4202), so ranges are valid in any `Expression()`
  position, including a binding's iterable.
- `VariableDeclarator extends Node` (ast/body/VariableDeclarator.java:48);
  `VariableDeclaratorMetaModel extends NodeMetaModel`, parent derived from the
  superclass by the generator — a new *child* metamodel needs no flips.
- `JmlQuantifiedExprContext.solveSymbol` iterates `getVariables()` as
  `VariableDeclarator` and calls `JavaParserSymbolDeclaration.quantifiedVar(VariableDeclarator, …)`
  (JmlQuantifiedExprContext.java:34–40, JavaParserSymbolDeclaration.java:36) — with
  the subclass design this classic path survives unchanged.

## 6. Implementation plan

### M1 — core: AST, grammar, print, round-trip

1. `JmlQuantifiedVariable extends VariableDeclarator` with `@OptionalProperty
   Expression iterable` in `ast.jml.expr`; register in `ALL_NODE_CLASSES`; run
   `./scripts/run-generators.sh`.
2. Grammar: single optional iterable in the quantifier alternative of
   `JmlPrimaryPrefix()` (§3.3); no new tokens;
   `./gradlew :javaparser-core:compileJavacc`; resolve LOOKAHEAD warnings.
3. Printing: `DefaultPrettyPrinterVisitor` + `ConcreteSyntaxModel` (§3.5) + new CSM
   entry for `JmlQuantifiedVariable`.
4. Validators (§3.6).
5. Tests: round-trips in `jmlparser-jml-tests` `resources/jml-exprs.txt` +
   `resources/csm_jml_expression.txt` (classic unchanged; single bindings over
   array/`Iterable`/range; `var`; filter clauses), LexicalPreservingPrinter
   round-trip, negative parses (multi declarator + iterable, self-referencing
   iterable, initializer on a bound binding).

### M2 — resolution & type inference

- `JmlQuantifiedExprContext`: kind detection + type inference (§3.7 table);
  `JmlJavaParserQuantifiedVariableDeclaration` reused.
- `var` normalization (`VarType`) and inference.
- Resolution expectation tests (extending
  `jmlparser-jml-tests/.../resolution/JmlQuantifiedExprResolutionTest.java` style)
  for array, `Iterable`, `SequencedCollection`, and range domains; shadowing tests.

### M3 — backends

- **Desugarer utility** (core, e.g. `com.github.javaparser.jml.JmlQuantifierDesugarer`):
  rewrite kinds 2/3 to classic quantifiers (§4 equivalences), so KeY and any
  classic-only consumer keep working.
- **`tools/jml2java`** (`Jml2JavaTranslator.visitForall`): kinds 3/4 map directly to
  Java enhanced-for — build `VariableDeclarationExpr` from the binding's declarator
  plus the iterable (`ForStmt` init pair); kind 2 → index loop;
  `\sum`/`\num_of`/`\product` accumulate.
- **`tools/smt`** (`JmlExpr2Smt`): kinds 2/3 → `forall ((i Int)) (=> (and (<= 0 i)
  (< i len)) P[a[i]])`; kind 4 raises a clear "not supported yet" (needs a `\seq`
  model of collections).
- **`tools/wd`** (`WDVisitor`): domain WD conditions (§4), reusing the binder
  switch.
- **`tools/stat`**, **`tools/lsp`**: handle the new binding node (statistics: count
  element quantifiers; LSP: no crash, optionally a quick-fix expanding into classic
  form via the desugarer).

### M4 — docs & release

- `jmltk-changelog.md` entry (fork-local SemVer; feature ⇒ minor bump, e.g.
  `1.2.0-SNAPSHOT`, keep `-SNAPSHOT`).
- Short section in `doc/readme.md` if it showcases JML syntax.
- Full-corpus re-run: `FragmentTest`, `FullExamplesTest` (openjml/key/tested).

## 7. Open questions

1. **Range domains `lo..hi`**: kept in v4 as the relational fallback (§2.1) — keep,
   or drop too?
2. **Re-enabling index+element bindings** (`int i : 0..n, T e : a[i]`, self-index,
   zip): AST is prepared; needs per-binding types in `QuantifiedVariables`
   (verified missing today, §5) + validator relaxation. When?
3. Untyped first binding `(\forall e : a; …)` — grammar-ready, inference in M2; or
   require `var` only?
4. Range generator element type: always `int`, or promote to `long` when operands
   are `long`?
5. Iterable generator domains in SMT/KeY: defer with a clear error — acceptable?
6. Optional future sugar: `a.indices`, implicit element binder, `Map` key domains,
   `String` → `char` domains, `a.first`/`a.last` for `SequencedCollection`.

## 8. Rejected / deferred alternatives

- **Deferred by decision (this revision): index+element multi-binding** — self-index
  (`int i : a[i]`), value bindings (`T e : a[i]`), 2-D compositional forms, zip.
  AST prepared (§3.1); grammar + validator work later (§7.2).
- **v1's single `Expression domain` field** — AST-level rejection stood for the
  multi-binding goal; with multi-binding deferred it would even suffice, but the
  per-binding node is kept to avoid re-migrating the AST later.
- **v2's abstract parent + reparenting of `VariableDeclarator`** — flips the
  metamodel parent of the most-used core node, drops `NodeWithVariables`, forces
  heterogeneous-list switches in every consumer; superseded by the subclass.
- **Standalone mimic class** (`JmlQuantifiedVariable` duplicating `VariableDeclarator`
  + iterable) — purest JML placement, but breaks the classic representation and
  reworks `quantifiedVar(VariableDeclarator, …)` and every consumer including the
  classic path; duplicated declarator boilerplate.
- **Two parallel lists** (`variables` + `foreachVariables`) — classic untouched, but
  splits one binding sequence into two mutually-exclusive containers (API asymmetry,
  exclusivity validator, printing branches); fallback only.
- Option B `\in` membership binder — new token for no benefit.
- Option C `a[*]` wildcards — no correlated binders, no existential polarity.
- Option D implicit index inference — silent magic, fragile tooling.
- Option E implicit element binder — cannot express distinct-element relations.
- Separate `JmlElementQuantifiedExpr` quantifier node — duplicated semantics,
  doubled churn; the new *variable* node is the right granularity.
- Extending the *core* `VariableDeclarator` in place with an optional iterable
  field — leaks a JML-only property onto a node used by every Java declaration.
- Changing the float lexer to "fix" `0.` — not needed (verified, §5) and would break
  Java conformance (`1.` is a legal Java float literal).
