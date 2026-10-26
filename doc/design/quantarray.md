# Design: Quantifiers over arrays and collections (`weigl/quantarray`)

Status: proposal **v3** (`JmlQuantifiedVariable extends VariableDeclarator`), not yet implemented.
v1 modeled the domain as a single `Expression domain` field (cannot express per-variable
domains); v2 reparented `VariableDeclarator` under an abstract parent (too much ripple for
core nodes). Both superseded — see §3.4 for the design comparison.
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
- integer index ranges `lo..hi` (the fork already has `RangeExpr`, see §5),
- uniformly for all generalized binders `\forall \exists \num_of \sum \product
  \min \max \choose` (and, for free, `\bsum`, `\infinite_union`, `\seq_def`).

## 2. Syntax

Accepted: the **for-each binder with `:`**. One quantifier takes a comma-separated
list of *bindings*; each binding may carry its own iterable after `:`:

```java
(\forall int e : a; e > 0)                        // elements of an array
(\exists T e : c; e.isEmpty())                    // elements of an Iterable/SequencedCollection
(\sum int e : a; e * e)                           // sum of squares
(\num_of String s : words; s.length() > 3)        // count words longer than 3
(\forall int i : 0..a.length-1; a[i] <= a[i+1])   // index iteration via range
(\forall var e : a; e != null)                    // 'var' = inferred element type
(\forall int e : a; e != null; e.id > 0)          // filter clauses before the body
```

### Index + element: per-variable bindings

Because every variable carries its own iterable, "both the value and the index" is
just two bindings — no magic, fully compositional:

```java
(\forall int i : 0..a.length-1, T e : a[i]; P(i, e))    // index + value binding
(\forall int i : a[i], T e : a[i]; P(i, e))             // self-index sugar: i over indices of a
(\forall int i : m[i], int j : m[i][j], T e : m[i][j]; P(i, j, e))  // 2-D, compositional
(\forall int k : 0..n-1, T x : a[k], U y : b[k]; x <= y)            // zip over two arrays
```

The *loose* mixed form `(\forall int i, T e : a[i]; P(i, e))` — `i` without an
iterable, referenced by a later binding — is a **validator error** ("`i` is not
bound to a domain; write `int i : a[i]` or `int i : 0..a.length-1`"). The AST must
stay honest: an unbound declarator is unbound.

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
  classic bindings stay plain `VariableDeclarator`s. Homogeneous lists are therefore
  structural (see validators), and the classic form is AST-identical to today.
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

### 3.3 Grammar (`javaparser-core/src/main/javacc/java.jj`, `QuantifiedVariables()` ~4743)

Per-binding types and iterables; classic shared-type lists preserved:

```java
// today: ONE type shared by the whole comma list
Type partialType; val=VariableDeclarator(partialType) ("," val=VariableDeclarator(partialType))*

// v3: each binding may carry its own type and its own iterable
NodeList<VariableDeclarator> QuantifiedVariables():
{
    t = Type(...)
    d = VariableDeclarator(t)                       // first binding (classic)
    [ LOOKAHEAD / colon handling: ":" iterable=Expression()
        -> rebuild d as JmlQuantifiedVariable(d, iterable) ]
    ( "," ( LOOKAHEAD(2) t2=Type(...) d=VariableDeclarator(t2)     // own type:  ", T e : a[i]"
         | d = VariableDeclarator(t) )                             // inherit:   ", j"
      [ ":" iterable=Expression() -> JmlQuantifiedVariable ] )*
}
```

- `(\forall int i, j; …)` parses exactly as today (inherited type, no iterables);
  the leading type instance is shared across inherited declarators exactly as the
  current production does (`VariableDeclarator(Type partialType)`, java.jj:2581–2590).
- The `:` after a declarator always belongs to that declarator; the parser wraps the
  declarator in a `JmlQuantifiedVariable` (or builds it directly) when an iterable
  follows.
- `(\forall int i, T e : a[i]; …)` — verified to fail today (§5) — becomes the typed
  binding form.
- `var` parses as a `ClassOrInterfaceType("var")`; the grammar action normalizes it
  to `VarType` ("infer from the binding kind").
- No new tokens → no `TokenTypes` changes, no lexer-rule reordering (per
  `AGENTS.md`). Optional stretch (untyped first binding `(\forall e : a; …)`) needs
  syntactic LOOKAHEAD `IDENTIFIER ":"` — deferred.

### 3.4 Why this shape — design comparison (ripple analysis)

| | v2: abstract parent + reparent | mimic class | two parallel lists | **v3: extend `VariableDeclarator`** |
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
  print **each binding** as `Type name : iterable` (comma-separated; the shared
  `MAXIMUM_COMMON_TYPE` scheme is wrong for per-binding types); otherwise keep the
  current classic printing unchanged.
- New CSM entry for `JmlQuantifiedVariable` itself (type, name, optional
  `: iterable`).

### 3.6 Validators

- **homogeneous bindings** within one quantifier: either all entries are plain
  declarators (classic form) or all are bound (`JmlQuantifiedVariable`); the loose
  `(\forall int i, T e : a[i]; …)` is an error with a fix-it;
- a bound binding must not have an initializer;
- **binding kinds** are decided by the iterable (§4) and re-checked:
  - range iterable → declarator must be integer type (`var` → `int`);
  - array/Iterable generator → declarator type must match the component/element
    type (or `var`);
  - self-index iterable `A[…x…]` (§4 kind 5) → the bound variable must occur as
    exactly one index position of the access chain, be of integer type, and the
    other index positions must reference *already bound* variables;
  - value binding (§4 kind 6) → declared type must match the iterable's type (or
    `var`);
- earlier bindings cannot reference later ones (no forward references).

### 3.7 Symbol solver

`JmlQuantifiedExprContext.solveSymbol` (javaparser-symbol-solver-core,
`contexts/jml/JmlQuantifiedExprContext.java:34–40`) keeps working unchanged — bound
entries are `VariableDeclarator`s. Additions:

- infer the static type per binding kind (§3 table below) via the iterable;
- `JmlJavaParserQuantifiedVariableDeclaration` is reused as-is (it wraps a
  `VariableDeclarator`).

| binding kind (§4) | static type of the bound variable |
|---|---|
| 1 classic | declared type (as today) |
| 2 range `lo..hi` | `int` |
| 3 array generator `A` | component type (element of `T[][]` is `T[]`) |
| 4 Iterable generator | first type argument of `Iterable<T>` |
| 5 self-index `A[…x…]` | `int` |
| 6 value binding `x : E` | declared type; must equal type of `E` |

## 4. Semantics

A quantifier `(\binder b1, …, bk; F1; …; Fn)` is interpreted left to right; each
binding `Type x [: E]` gets one of six kinds:

1. **classic** (no `E`): unconstrained variable, restricted only by the filter
   clauses `F1..Fn-1` — unchanged classic JML.
2. **range generator** (`E` is `lo..hi`, `BinaryExpr(Operator.RANGE)`): `x` iterates
   the half-open integer interval `[lo, hi)`.
3. **array generator** (`E` array-typed, not an array access): `x` iterates the
   elements of `E` (for `T[][]`, elements are `T[]`).
4. **Iterable generator** (`E` assignable to `Iterable<T>`): `x` iterates the
   elements in iteration order (specifications should be order-independent; the
   order-dependent binders `\seq_def`/`\bsum` are defined over the iterator order).
5. **self-index generator** (`E` is an array access `A[…x…]` mentioning `x` in an
   index position): `x` iterates the valid indices `0..len-1` of the dimension where
   `x` occurs (e.g. `int i : a[i]` → `[0, a.length)`; `int j : m[i][j]` →
   `[0, m[i].length)` with `i` already bound). This preserves the intuition behind
   the Stack Overflow anecdote in §1.
6. **value binding** (otherwise, `E` scalar): `x = E` within each iteration — a
   let-like binding (`T e : a[i]` binds `e` to the element at index `i`).

Definitional equivalences (all binders; parts before the last remain conjunctive
filters, the last part is the body/value):

- kind 2: `(\binder int x : lo..hi; P…)` ≡ `(\binder int x; lo <= x && x < hi; P…)`
- kind 3 (array), fresh `_i`:
  `(\binder T x : a; P…)` ≡ `(\binder int _i; 0 <= _i && _i < a.length; P[x := a[_i]])`
  (pure substitution; alternatively bind via the existing `JmlLetExpr`)
- kind 4: quantification over the (finite) element sequence of the iterator;
  formal model deferred until the KeY `\seq` machinery is revived (java.jj ~8002)
- kind 5: `int x : A[…x…]` ≡ range generator over that dimension's length
- kind 6: `T x : E` ≡ `(\let T x = E; …)` — no iteration, no quantifier over `x`
- bindings compose: later bindings may reference earlier variables (dependent
  domains, zip); the domain of an iteration kind is evaluated **once** per enclosing
  iteration.

**Well-definedness:**

- generator domains (kinds 2–5): the iterable must be pure (side-effect free) and,
  for arrays/Iterables, non-null; a null domain makes the quantifier undefined (WD
  violation — consistent with the classic form whose range would dereference
  `a.length`; vacuous-true rejected: it hides null bugs);
- value bindings (kind 6): `E` must be well-defined and pure;
- empty iteration domain: `\forall`→true, `\exists`→false, `\num_of`→0, `\sum`→0,
  `\product`→1, `\min`/`\max`/`\choose`→undefined (same as classic JML on an empty
  range).

## 5. Facts verified against the current build (Oct 2026)

- `(\forall int i; 0 <= i < 5; a[i])` parses via `JavaParser.parseJmlExpression`
  (note `JmlMultiCompareExpr` chained comparison already works).
- `(\forall int e : a; e > 0)` fails with `Encountered unexpected token: ":"` right
  after the declarator list — the extension point is clean and LL(1).
- `(\forall int i, j; true)` (shared type over a comma list) parses, but
  `(\forall int i, T e : a[i]; …)` and `(\forall int i, var e : a[i]; …)` fail:
  `QuantifiedVariables` (java.jj:4743–4754) binds one `Type` to the whole list —
  per-binding types are required (§3.3).
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
- `VariableDeclarator(Type partialType)` (java.jj:2581–2590) shares the leading type
  instance across the comma list — the per-binding grammar keeps that behavior for
  inherited declarators.

## 6. Implementation plan

### M1 — core: AST, grammar, print, round-trip

1. `JmlQuantifiedVariable extends VariableDeclarator` with `@OptionalProperty
   Expression iterable` in `ast.jml.expr`; register in `ALL_NODE_CLASSES`; run
   `./scripts/run-generators.sh`.
2. Grammar: per-binding production in `QuantifiedVariables()` (§3.3); no new tokens;
   `./gradlew :javaparser-core:compileJavacc`; resolve LOOKAHEAD warnings.
3. Printing: `DefaultPrettyPrinterVisitor` + `ConcreteSyntaxModel` (§3.5) + new CSM
   entry for `JmlQuantifiedVariable`.
4. Validators (§3.6).
5. Tests: round-trips in `jmlparser-jml-tests` `resources/jml-exprs.txt` +
   `resources/csm_jml_expression.txt` (classic unchanged, single binding, multi
   binding, zip, self-index), LexicalPreservingPrinter round-trip, negative parses
   (mixed bound/unbound bindings, initializer in bound binding, forward references).

### M2 — resolution & type inference

- `JmlQuantifiedExprContext`: per-binding kind detection + type inference (§3.7
  table); `JmlJavaParserQuantifiedVariableDeclaration` reused.
- `var` normalization (`VarType`) and inference.
- Resolution expectation tests (extending
  `jmlparser-jml-tests/.../resolution/JmlQuantifiedExprResolutionTest.java` style)
  for array, `Iterable`, `SequencedCollection`, range, self-index, value binding,
  zip; shadowing tests.

### M3 — backends

- **Desugarer utility** (core, e.g. `com.github.javaparser.jml.JmlQuantifierDesugarer`):
  rewrite kinds 2/3/5 to classic quantifiers (§4 equivalences) and kind 6 to
  substitution/`JmlLetExpr`, so KeY and any classic-only consumer keep working.
- **`tools/jml2java`** (`Jml2JavaTranslator.visitForall`): kinds 3/4 map directly to
  Java enhanced-for — build `VariableDeclarationExpr` from the binding's declarator
  plus the iterable (`ForStmt` init pair); kind 2 → index loop; kind 5 → index loop
  over the dimension; kind 6 → local variable; `\sum`/`\num_of`/`\product`
  accumulate.
- **`tools/smt`** (`JmlExpr2Smt`): kinds 2/3/5 → `forall ((i Int)) (=> (and (<= 0 i)
  (< i len)) P[a[i]])`; kind 6 → substitution; kind 4 raises a clear "not supported
  yet" (needs a `\seq` model of collections).
- **`tools/wd`** (`WDVisitor`): per-kind WD conditions (§4), reusing the binder
  switch.
- **`tools/stat`**, **`tools/lsp`**: handle the new binding node (statistics: count
  binding kinds; LSP: no crash, optionally a quick-fix expanding into classic form
  via the desugarer).

### M4 — docs & release

- `jmltk-changelog.md` entry (fork-local SemVer; feature ⇒ minor bump, e.g.
  `1.2.0-SNAPSHOT`, keep `-SNAPSHOT`).
- Short section in `doc/readme.md` if it showcases JML syntax.
- Full-corpus re-run: `FragmentTest`, `FullExamplesTest` (openjml/key/tested).

## 7. Open questions

1. Confirm the is-a relationship (`JmlQuantifiedVariable extends VariableDeclarator`);
   fallback if rejected: standalone mimic class or two parallel lists (§3.4) — both
   with the documented extra churn.
2. Kind 5 (self-index sugar `int i : a[i]`) — include in v1 or defer in favour of
   `0..a.length-1` (and later `a.indices` sugar)?
3. Untyped first binding `(\forall e : a; …)` — grammar-ready, inference in M2; or
   require `var` only?
4. Range generator element type: always `int`, or promote to `long` when operands
   are `long`?
5. Iterable generator domains in SMT/KeY: defer with a clear error — acceptable?
6. Optional future sugar: `a.indices`, implicit element binder, `Map` key domains,
   `String` → `char` domains, `a.first`/`a.last` for `SequencedCollection`.

## 8. Rejected alternatives (summary)

- **v1's single `Expression domain` field** — cannot express per-variable domains
  (`(\forall int i : 0..n, T e : a[i]; …)`, zip).
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
