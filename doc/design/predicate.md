# Design: JML predicate declarations — `//@ instance predicate Pred(int i) = i > 0;`

Status: proposal (draft).
Branch: `weigl/predicate`.

## 1. Goal

Support **predicate** declarations as a class-body JML construct:

```java
public class P {
    //@ instance predicate Pred(int i) = i > 0;
}
```

A predicate binds a name (`Pred`) to a named assertion formula over a parameter list
(`int i`); here the formula is the predicate `i > 0`. `instance` is the existing
JML instance modifier (`Modifier.DefaultKeyword.JML_INSTANCE`, already lexed and
parsed — `java.jj:7709`).

This is the unit KeY-style proof scripts refer to with `use_lemma Pred(…)`.
The present design scope is **only the declaration**; `use_lemma` statements
(proof commands inside `\by {…}` scripts) are out of scope here — generic
proof commands already parse as `JmlProofCmdStmt` on `main`, and a method-level
`use_lemma` statement is future work.

## 2. Language surface

### 2.1 Grammar

```
lemma_declaration  ::= [ JML modifiers ] 'predicate' SimpleName '(' Parameters ')' '=' Expression ';'
```

A minimal, deterministic shape, modelled on the existing class-level
declarations (`JmlClassExpr` at `java.jj:7943`, `JmlClassRepresents` at
`java.jj:7978`):

- `predicate` is a **dedicated lexer token** `<PREDICATE: "predicate">`, added to the
  `<JML_MULTI_CONTRACT,JML_SINGLE_CONTRACT>` token block only, so plain Java
  identifiers named `predicate` outside JML comments are unaffected.
- The parameter list is the ordinary Java formal parameter list (`Parameters()`,
  `java.jj:2797`).
- Body is any JML expression — intended to be a boolean formula over the
  parameters (type-checking is a symbol-solver concern, not native).

Supported forms (a subset of KeY's `lemma_declaration`):

```java
//@ instance predicate Pred(int i) = i > 0;
//@ model predicate Even(int n) = (\exists int k; n == 2 * k);
//@ predicate NonNegative(int[] a);          // optional? — NO, body is mandatory in this design
```

The body expression is **mandatory** (`SimpleName '(' Parameters ')' '=' Expression ';'`).

### 2.2 Modifiers

`instance`, `model`, `static`, access modifiers, math modes (`code_*_math`,
`spec_*_math`) and `pure` may appear, subject to validation; `instance` is the
canonical modifier of the motivating example.

## 3. AST design

New node: `JmlPredicateDeclaration` in
`javaparser-core/src/main/java/com/github/javaparser/ast/jml/body/`.

```
JmlPredicateDeclaration
  ├─ NodeList<Modifier>   modifiers   (JML + Java modifiers)
  ├─ SimpleName           name        (predicate name, e.g. Pred)
  ├─ NodeList<Parameter>  parameters  (formal parameters, e.g. int i)
  ├─ Expression           body        (the assertion formula, e.g. i > 0)
  └─ NodeList<SimpleName> jmlTags     (JML tag marker list, for statistic selection)
```

- Extends `JmlClassLevelDeclaration<JmlPredicateDeclaration>` (the abstract class
  for *declarations stored directly in a class body*, together with
  `JmlFieldDeclaration`, `JmlMethodDeclaration`, `JmlClassExprDeclaration`, …).
- Implements `NodeWithName`, `NodeWithModifiers`, `NodeWithAccessModifiers`
  (property `NAME`, `MODIFIERS`, `PARAMETERS`, `BODY`, `JML_TAGS` — all already
  exist in the generated `ObservableProperty`).
- No keyword-node: the `predicate` keyword is fixed and needs no kind/position
  storage (unlike `JmlClassExprDeclaration`, which must store which token
  opened the declaration). The token range of the node starts at the first
  modifier (or `predicate`), so lexical position is preserved.

### 3.1 Why not reuse `JmlMethodDeclaration`/`JmlClassExprDeclaration`?

- `JmlMethodDeclaration` wraps a full Java `MethodDeclaration` (return type,
  body, throws) — a predicate has **no return type and no Java body**.
- `JmlClassExprDeclaration` is a *kind-ed* expression clause (invariant /
  axiom / constraint / initially) with an optional name — a predicate is *always
  named* and *always has parameters*, plus a body formula. Reusing it would
  overload `kind` and lose the parameter list.

A dedicated minimal node keeps the model honest and makes the metamodel, the
symbol-solver context, and the stat reporter straightforward.

## 4. Parsing & printing

### 4.1 Grammar changes (`java.jj`)

1. Add `<PREDICATE: "predicate">` to the `<JML_MULTI_CONTRACT,JML_SINGLE_CONTRACT>`
   token block (no reordering of existing rules).
2. New production `JmlClassPredicate(ModifierHolder)`:
   ```
   <PREDICATE> SimpleName() Parameters() "=" Expression() ";"
   ```
   The range begins at the first modifier (if any) via `orIfInvalid(...)`,
   mirroring `JmlClassExpr`/`JmlClassRepresents`.
3. New dispatch branch in `JmlClassLevelInner` (`java.jj:7729`):
   `LOOKAHEAD(1) element = JmlClassPredicate(modifiers)`.
   `JmlClassLevelStart` reaches this production for every class-body JML
   declaration; `JmlProcessor` inserts the resulting `BodyDeclaration` into
   `TypeDeclaration.getMembers()`, so the grammar change alone wires the
   construct into `processJml=true` parsing.

### 4.2 Token classification

Every token kind must be classified in `TokenTypes#getCategory`, otherwise
`TokenTypesTest` breaks — add `case PREDICATE:`. The `JavaToken` kinds and
`GeneratedJavaParserConstants` are regenerated by
`./gradlew :javaparser-core:compileJavacc`.

### 4.3 Printing

- **Concrete Syntax Model**: a `ConcreteSyntaxModel` entry
  `modifiers? 'predicate' name '(' params ')' '=' body ';'` so `genericPrettyPrint`
  round-trips (used by `ConcreteSyntaxModelTest`-style tests and the LSP).
- **DefaultPrettyPrinterVisitor**: `visit(JmlPredicateDeclaration)` inside
  `wrapInJmlIfNeeded(...)` — prints modifiers, `predicate`, name, parenthesised
  parameter list, `" = "`, body, `;`. This is what `toString()` / the
  lex-preserving printer fall back on for the new node.

## 5. Semantics & validation

- **Modifier validation** (`ModifierValidator.visit(JmlPredicateDeclaration)`):
  allowed set = access modifiers, `static`, `final`, `model`, `instance`,
  math modes, `pure`. Reuses the existing `validateModifiers` helper and the
  parameter-modifier check.
- **Structural validation**: body and name are mandatory (`@NonEmpty`/non-null
  properties enforced by the generated code and the `CommonValidators`
  tree visitor); formal parameters must have distinct names (checked in the
  validator visit, like `MethodDeclaration` checks live in `ModifierValidator`).
- The body is *not* type-checked by the parser (boolean-ness is a
  symbol-solver concern, out of scope).

## 6. Symbol resolution (symbol solver)

- New context `JmlPredicateDeclarationContext extends
  AbstractJavaParserContext<JmlPredicateDeclaration>` resolving the formal
  parameters of the predicate inside its own body (`solveSymbol` over
  `getParameters()`, each via `JavaParserSymbolDeclaration.parameter`), then
  falling back to the surrounding class scope — the same pattern as
  `JmlContractContext` / `JmlQuantifiedExprContext`.
- Registered in `JavaParserFactory` (like the other JML contexts).
- `DefaultVisitorAdapter` gains the `visit(JmlPredicateDeclaration)` forwarding
  stub (throwing `UnsupportedOperationException` like the sibling class-level
  declarations) so the name solver never silently mis-handles the node.

Effect: `i` inside `i > 0` resolves to the predicate's parameter `i` (type `int`),
and `//?`-style resolution tests can assert the binding.

## 7. Tool backends

| Tool | Behaviour |
|------|-----------|
| **stat** | New `StatisticKey.totalPredicates` counter (`JmlStatisticsReporter`); the XML `StatVisitor` emits a `<predicate name="…">` element with modifier attributes and body-expression stats (mirroring the `JmlClassExprDeclaration` handling). |
| **wd** | `visit(JmlPredicateDeclaration)` computes well-definedness of the body expression (a predicate's formula must itself be well-defined; parameters are bound, `NameExpr` wd is trivially `true`). |
| **smt** | No change: `JmlExpr2Smt` is expression-only; predicate declarations are not visited. |
| **jml2java** | No change: `Jml2JavaTranslator` translates expressions only; declarations are left untouched (they round-trip through the pretty printer as JML comments). |
| **lsp** | New `DocumentSymbol` (kind `Method`) for the predicate in `JmlCatchSymbols`; `CodeActionCollector` offers the "Well-definedness Check" on the predicate body. |

## 8. Compatibility notes

- `predicate` becomes a token **only inside JML comments**; Java source using
  `predicate` as an identifier keeps parsing (DEFAULT lexer state unchanged).
- `JmlClassLevelStart`/`JmlProcessor` insertion means `FragmentTest`,
  `FullExamplesTest` and the lexical-preserving printer pick up the new
  construct with no extra wiring.
- No new lexer modes, no reordering of existing token rules.

## 9. Out of scope (future work)

- `use_lemma <call>;` method-level / proof-command statement (KeY PR #3971's
  second construct) — track separately.
- Predicate bodies with proof scripts (`\by {…}`) or `assert`-style keyword forms.
- Delegating predicate-*application* semantics to provers; this branch only
  *declares* predicates in the AST.
- Type-checking the predicate body (boolean-ness) via the symbol solver.
