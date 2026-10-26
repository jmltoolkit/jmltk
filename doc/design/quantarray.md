# Design: Quantifiers over arrays and collections (`weigl/quantarray`)

Status: proposal, not yet implemented.
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
- integer index ranges `lo..hi` (bonus: the fork already has `RangeExpr`, see §5),
- uniformly for all generalized binders `\forall \exists \num_of \sum \product
  \min \max \choose` (and, for free, `\bsum`, `\infinite_union`, `\seq_def`).

## 2. Syntax options

### Option A — for-each binder with `:` (recommended)

The colon starts the *domain* of quantification; everything after it is the
element-domain expression:

```java
(\forall int e : a; e > 0)                       // all elements positive
(\exists T e : c; e.isEmpty())                   // some element of collection c
(\sum int e : a; e * e)                          // sum of squares
(\num_of String s : words; s.length() > 3)       // count words longer than 3
(\min int e : a; e)                              // minimum element
(\forall var e : a; e != null)                   // 'var' = inferred element type
(\forall int e : a; e != null; e.id > 0)         // filter clauses before the body
(\forall int i : 0..a.length-1; a[i] <= a[i+1])  // relational: index via range domain
(\forall int[] row : m; row.length == n)         // 2-D array: rows are elements
(\forall int e : m; \forall int f : m; e != f ==> a[e] != a[f])   // nested
```

Properties:

- **Java for-each analogy** (`for (T x : a)`) — the bound variable denotes the
  *element*, never the index (see §4, "element vs. index").
- **No new tokens.** `:` (COLON) already exists; no lexer rule, no `TokenTypes`
  change. The parse decision after the declarator list is `;` (classic) vs `:`
  (element) — LL(1), verified empirically (§5).
- **Uniform** over all binders; comes as one grammar alternative.
- **Range domains for free:** `lo..hi` (half-open, end-exclusive) covers the
  relational cases (sortedness, monotonicity) that pure element binding cannot
  express, reusing the existing `RangeExpr`.

### Option B — membership binder `\in`

```java
(\forall T e \in a; P(e))
```

Mathematical style. Costs: new token `\\in` (lexer rule + `TokenTypes`
classification, must not reorder existing rules), and it may collide with a future
heap/set membership operator (KeY uses `\in`-like symbols). No benefit over `:`.

### Option C — wildcard element `a[*]`

```java
(\forall a[*]; a[*] != null)     // or even bare: a[*] != null
```

Each occurrence of `a[*]` would be an implicit binder. Rejected:

- relational properties need *two different* elements (`a[i] <= a[i+1]`) — fresh
  binders per occurrence cannot correlate them;
- existential polarity is inexpressible without a quantifier anyway;
- needs a new AST node + grammar special case for `LBRACKET * RBRACKET`, and
  muddies write/LHS semantics.

### Option D — implicit index domain

```java
(\forall int i; a[i] != null)    // range implicitly "valid indices of a"
```

The binder's domain is *inferred* from the body. Rejected: multiple arrays give an
intersection domain, no array gives an infinite domain, and silent inference is
fragile for tooling (bounds extraction, SMT encoding).

### Option E — implicit element binder

```java
(\forall a; a[*] > 0)     // or (\forall a; \elem(a) > 0)
```

Ultra-compact, but a single implicit binder cannot express two distinct elements
(monotonicity, distinctness) and repeated `\elem(a)` is ambiguous. Could later be
added as *sugar* on top of Option A (desugars to A with a generated fresh name);
not part of the core proposal.

### Prior art

| Language | Syntax |
|---|---|
| Dafny | `forall x in s :: P(x)` (sets/seqs); `forall i \| 0 <= i < a.Length :: a[i] >= 0` |
| Spec# / Code Contracts | `forall int i in 0..a.Length: P(i)`; `Contract.ForAll<T>(c, pred)` |
| Eiffel | `across a as c all P(c.item) end` |
| SPARK Ada | `for all I in A'Range => P(A(I))` |
| ACSL | index-style only (`\forall integer i; 0 <= i < n => …`) |
| JML RefMan | variable binders only (status quo) |

## 3. AST & grammar design

**Decision: extend `JmlQuantifiedExpr` with an optional domain — no new node class.**

The element form *is* a quantified expression (binder + variables + parts); only the
way the variables are bound differs. A separate class would duplicate semantics and
double the churn in every visitor and in the seven tools that switch on the binder.

```java
public class JmlQuantifiedExpr extends JmlExpression implements NodeWithVariables<JmlQuantifiedExpr> {
    private JmlBinder binder;
    private NodeList<VariableDeclarator> variables;
    @OptionalProperty
    private Expression domain;          // NEW: null = classic form
    private NodeList<Expression> expressions;   // filters + body/value, unchanged
}
```

- Classic form: `domain == null` — parses and prints exactly as today.
- Element form: `variables` holds the element declarator(s) (type `VarType` for the
  inferred variants), `domain` the domain expression.

### Grammar (`javaparser-core/src/main/javacc/java.jj`, `JmlPrimaryPrefix()` ~line 4725)

```java
// today
"(" {begin=token();}
 (<FORALLQ>|<EXISTS>|<NUM_OF>|<SUM>|<MIN>|<PRODUCT>|<MAX>|<JML_IDENTIFIER>)
 {quant=token();}
 vars=QuantifiedVariables()
 ";" expr=Expression() {exprs.add(expr);} (";" expr=Expression() {exprs.add(expr);})*
")"

// extended (LL(1) on ':' vs ';' after the declarators)
"(" {begin=token();}
 binder-token {quant=token();}
 vars=QuantifiedVariables()
 [ ":" domain=Expression() ]          // NEW — element/domain form
 ( ";" expr=Expression() {exprs.add(expr);} )*
")"
```

Optional stretch (untyped binder `(\forall e : a; P(e))`, represented with a
`VarType` declarator; needs syntactic LOOKAHEAD `IDENTIFIER ":"`):

```java
( LOOKAHEAD(<IDENTIFIER> ":") nm=<IDENTIFIER> ":" domain=Expression()
| vars=QuantifiedVariables() [ ":" domain=Expression() ] )
```

No new tokens → no `TokenTypes` changes, no lexer-rule reordering (per `AGENTS.md`).

### Regenerated / printing surfaces (run `./scripts/run-generators.sh`)

- metamodel `JmlQuantifiedExprMetaModel` + property registration in
  `JavaParserMetaModel`, `ObservableProperty.DOMAIN`, all generated visitors.
- `printer/DefaultPrettyPrinterVisitor.visit(JmlQuantifiedExpr)` (~line 914): print
  `" : " + domain` between the declarators and the first `;` part.
- `printer/ConcreteSyntaxModel` entry for `JmlQuantifiedExpr` (~line 1318): add a
  `conditional(DOMAIN, IS_NOT_EMPTY, sequence(token(COLON), space(), child(DOMAIN)))`
  between the variable list and the `;` list.

### Validators

- element form requires exactly one declarator (until multi-declarator semantics is
  decided, §7); declarator must not have an initializer;
- the domain expression must not reference the bound variables;
- `var`/inferred type only resolvable if the domain type is known.

### Symbol solver

`JmlQuantifiedExprContext` (javaparser-symbol-solver-core, `contexts/jml/`) already
binds `getVariables()` as `JavaParserSymbolDeclaration.quantifiedVar`; with a domain
present it additionally *infers the element type*:

| domain kind | element type |
|---|---|
| `ArrayType` `T[]` (any dim) | component type (element of `T[][]` is `T[]`) |
| `BinaryExpr(Operator.RANGE)` `lo..hi` | `int` |
| reference type assignable to `Iterable<T>` (`SequencedCollection`, `List`, …) | first type argument `T` |
| unresolved | leave unbound, report validation problem |

`JmlJavaParserQuantifiedVariableDeclaration` stays as-is (it wraps the declarator).

## 4. Semantics

**Element vs. index.** The bound variable denotes the **element** (Java for-each
consistency, `for (T x : a)`). The Stack Overflow anecdote shows some users expect
`i` to range over *indices*; that need is served by range domains
(`(\forall int i : 0..a.length-1; a[i] <= a[i+1])`) — possibly with future sugar
like `a.indices`.

**Definitional equivalences** (all binders; parts before the last remain conjunctive
filters, the last part is the body/value):

- array domain `a` of type `T[]`, fresh `_i`:
  `(\binder T e : a; P1; …; Pn)` ≡ `(\binder int _i; 0 <= _i && _i < a.length; P1[e:=a[_i]]; …; Pn[e:=a[_i]])`
  (pure substitution; alternatively bind via the existing `JmlLetExpr`).
- range domain: `(\binder int e : lo..hi; P1; …; Pn)` ≡
  `(\binder int e; lo <= e && e < hi; P1; …; Pn)` — half-open, end-exclusive.
- `Iterable` domain: quantification over the (finite) element sequence in iteration
  order. Specifications should not depend on the order, with the exception of
  `\seq_def`/`\bsum` whose value is order-dependent (documented, defined over the
  iterator order).

**Well-definedness:**

- the domain expression is evaluated **exactly once**, must be pure (side-effect
  free) and **non-null**; `a == null` makes the quantifier undefined (WD violation)
  — consistent with the classic form, whose range would dereference `a.length`.
  (Vacuous-true was considered and rejected: it hides null bugs.)
- empty domain: `\forall`→true, `\exists`→false, `\num_of`→0, `\sum`→0,
  `\product`→1, `\min`/`\max`/`\choose`→undefined (same as classic JML on an empty
  range).

**Scope rules:** the domain may not mention the bound variables; nested element
quantifiers may shadow; the domain is not re-evaluated per element.

## 5. Facts verified against the current build (Oct 2026)

- `(\forall int i; 0 <= i < 5; a[i])` parses via `JavaParser.parseJmlExpression`
  (note `JmlMultiCompareExpr` chained comparison already works).
- `(\forall int e : a; e > 0)` fails with `Encountered unexpected token: ":"` right
  after the declarator list — the extension point is clean and LL(1).
- `0..5`, `n..m`, `a[0..5]` all parse as `BinaryExpr(Operator.RANGE)` and print as
  `0 .. 5` etc. The feared float-literal tie (`0.` is a legal Java float) does not
  bite in this grammar; existing fragments already use `students[0..n]`
  (`jmlparser-jml-tests/.../fragments/stmt_1132627598.txt`).
- `RangeExpr` sits below `RelationalExpression` (java.jj:4202), so ranges are valid
  in any `Expression()` position, including a quantifier domain.

## 6. Implementation plan

### M1 — core: parse, AST, print, round-trip

1. `JmlQuantifiedExpr`: add `@OptionalProperty Expression domain` (+ accessors,
   constructor overloads, update `@AllFieldsConstructor`); run
   `./scripts/run-generators.sh`; fix tool compile errors.
2. Grammar: extend the quantifier alternative in `JmlPrimaryPrefix()`; no new
   tokens; `./gradlew :javaparser-core:compileJavacc`; resolve LOOKAHEAD warnings.
3. Printing: `DefaultPrettyPrinterVisitor` + `ConcreteSyntaxModel`.
4. Validators (structural ones above).
5. Tests: round-trip cases in `jmlparser-jml-tests`
   `resources/jml-exprs.txt` + `resources/csm_jml_expression.txt`
   (`JmlExpressionTest`, `ConcreteSyntaxModelTest`), LexicalPreservingPrinter
   round-trip, negative parses (initializer in declarator, domain referencing the
   bound var, two declarators with domain until decided).

### M2 — resolution & type inference

- `JmlQuantifiedExprContext`: element-type inference per §3 table.
- `var`/untyped binder support (`VarType` declarator type).
- Resolution expectation tests: extend
  `jmlparser-jml-tests/.../resolution/JmlQuantifiedExprResolutionTest.java` style
  (`//?` expectations) for array, `Iterable`, `SequencedCollection`, range, and 2-D
  domains; shadowing tests.

### M3 — backends

- **Desugarer utility** (core, e.g. `com.github.javaparser.jml.JmlQuantifierDesugarer`):
  element/range forms → classic form (§4 equivalences, array + range only). Lets the
  KeY path and any consumer that only knows classic quantifiers keep working.
- **`tools/jml2java`** (`Jml2JavaTranslator.visitForall`): element form maps directly
  to Java enhanced-for (`for (T e : a) { if (!(P)) fail; }`) — *simpler* than the
  current `findUpperBound`/`findLowerBound` pattern matching; range domain → classic
  index loop; `\sum`/`\num_of`/`\product` accumulate.
- **`tools/smt`** (`JmlExpr2Smt`): array + range domains → `forall ((i Int)) (=> (and (<= 0 i) (< i len)) P[a[i]])`;
  Iterable domains raise a clear "not supported yet" (needs a `\seq` model of
  collections — future work, cf. the commented-out KeY `\seq` machinery in java.jj
  ~8002).
- **`tools/wd`** (`WDVisitor`): domain WD conditions (non-null, purity), reusing the
  existing binder switch.
- **`tools/stat`**, **`tools/lsp`**: handle the new field (statistics: count element
  quantifiers; LSP: no crash, optionally a quick-fix to expand into classic form via
  the desugarer).

### M4 — docs & release

- `jmltk-changelog.md` entry (fork-local SemVer; feature ⇒ minor bump, e.g.
  `1.2.0-SNAPSHOT`, keep `-SNAPSHOT`).
- Short section in `doc/readme.md` if it showcases JML syntax.
- Full-corpus re-run: `FragmentTest`, `FullExamplesTest` (openjml/key/tested).

## 7. Open questions

1. Untyped binder `(\forall e : a; …)` — grammar-ready in M1, inference in M2; keep?
2. Multiple declarators with a domain (`(\forall T e, f : a; …)`, cartesian
   semantics) — recommend restricting to one declarator initially.
3. Range-domain element type: always `int`, or promote to `long` when operands are
   `long`?
4. Iterable domains in SMT/KeY: defer with a clear error — acceptable?
5. Optional future sugar: `a.indices` (range over valid indices), implicit element
   binder (Option E), `Map` key domains, `String` → `char` domains, `a.first`/`a.last`
   for `SequencedCollection`.

## 8. Rejected alternatives (summary)

- Option B `\in` — new token for no benefit.
- Option C `a[*]` wildcards — no correlated binders, no existential polarity.
- Option D implicit index inference — silent magic, fragile tooling.
- Option E implicit element binder — cannot express distinct-element relations.
- Separate `JmlElementQuantifiedExpr` AST node — duplicated semantics, doubled churn.
- Changing the float lexer to "fix" `0.` — not needed (verified, §5) and would break
  Java conformance (`1.` is a legal Java float literal).
