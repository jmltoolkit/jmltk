# Design: JML lemmas and `use_lemma` proof commands (port of KeY PR #3971) — `weigl/lemmata`

Status: proposal (draft), not yet implemented.
Source: [KeYProject/key#3971 "JML Lemmas and proof scripts in model methods"](https://github.com/KeYProject/key/pull/3971)
(draft, mattulbrich, milestone v3.1.0, branch `mattulbrich:jmlLemmas`).
Branch: `weigl/lemmata`.

## 1. What the KeY PR adds — JML language surface

The PR's parser changes (KeY's ANTLR4 grammars) introduce exactly two new JML
constructs plus one relaxation:

| KeY grammar change | New/changed rule |
|---|---|
| `JmlLexer.g4` | `LEMMA: 'lemma' -> pushMode(expr);`<br>`USE_LEMMA: 'use_lemma' -> pushMode(expr);` |
| `JmlParser.g4` `classlevel_element` | `\| lemma_declaration` |
| `JmlParser.g4` `methodlevel_element` | `\| use_lemma_statement` |
| `JmlParser.g4` | `lemma_declaration: LEMMA IDENT param_list assertionProof SEMI_TOPLEVEL;`<br>`use_lemma_statement: USE_LEMMA postfixexpr SEMI_TOPLEVEL;` |
| `JmlParser.g4` `mbody_block` | `LBRACE (mbody_var \| assert_statement)* mbody_statement RBRACE` (asserts in model-method bodies) |

Everything else in the PR is KeY-internal machinery (the `UseLemmaStatement` KeY
AST node, `UseLemmaStatementBuiltInRuleApp`/`UseLemmaStatementRule`, the script
macros `ApplyScriptsMacro`/`LemmaMethodScriptMacro`/`ModelMethodScriptMacro`,
`ModifierKind.JML_LEMMA`, `ProgramMethod.isLemma()`, the `JMLTransformer`
method-declaration refactor). That machinery consumes a parsed JML AST; the
JML-side grammar is the interface that JMLtk must provide.

### The intended usage (from the PR description)

```java
/*@ static no_state lemma nospace(\seq s) \by {
  @   oss; macro "nosplit-prop";
  @   obtain \bigint N \from_goal;
  @   cut s.length == 0 \by { case "true": auto; case "false": …; }
  @   …
  @   use_lemma nospace(t);          // ← a proof command inside the script
  @ };
  @*/
static int mex0(int[] a) {
    …
    //@ use_lemma nospace(\array2seq(a));     // ← KeY ALSO uses it as a method-level statement
    …
}
```

## 2. What JMLtk already has — the porting baseline

JMLtk already implements the entire *proof-script language* that the PR's lemma
bodies use:

- Token `<BY: "\\by">` (`java.jj:281`).
- `JmlProofBlock()` / `JmlProofStmt()` / `JmlProofCmdStmt()` / `JmlProofObtainStmt()`
  / `JmlProofArgList()` / `JmlProofCmdSuffix()` (`;` or `\by {…}`) /
  `JmlProofCaseStmt()` (`case "label": …` / `default: …`) — `java.jj:7135-7243`,
  explicitly commented as "mirroring KeY's JmlParser.g4:
  assertionProof / proofCmd / proofCmdSuffix / proofArg / proofCmdCase".
- `obtain` is parsed **contextually** (not a reserved word): semantic lookahead
  on the token image, then consumed as `<IDENTIFIER>` (`java.jj:7162-7172`).
- Generic proof commands: `name=SimpleName() JmlProofArgList() JmlProofCmdSuffix()`
  → `JmlProofCmdStmt` (`java.jj:7173-7177`). Because script commands are lexed
  as ordinary identifiers, `use_lemma nospace(t);` inside a `\by {…}` script
  **already parses as `JmlProofCmdStmt(name="use_lemma", args=[nospace(t)])`**
  with today's grammar — provided `use_lemma` stays an identifier.
- AST nodes in `ast/jml/proof/`: `JmlProofArg`, `JmlProofCaseStmt`,
  `JmlProofCmdStmt`, `JmlProofObtainStmt`, `JmlProofStmt`.
- JML statements with optional script: `JmlStatement()` (`java.jj:7087`) handles
  `assert / assume / hence_by / hence_by_redundantly / set / assert_redundantly
  <expr> [\by script] ;` → `JmlExpressionStmt`.
- The `instance` modifier already exists: `<INSTANCE: "instance">`
  (`java.jj:320`), `jmlModifiersInner` → `Modifier.DefaultKeyword.JML_INSTANCE`
  (`java.jj:7727`, `Modifier.java:151`).
- JML logic types `\seq`, `\bigint`, … — `JmlLogicType` (`ast/jml/type`).
- Class-level JML declaration machinery: `JmlClassLevelStart`/`JmlClassLevelInner`
  (`java.jj:7747-7815`) dispatching to `JmlClassExprDeclaration`
  (`axiom/invariant/…` — the existing `//@ axiom evenSumLemma();` corpus pattern),
  `JmlClassAccessibleDeclaration`, `JmlRepresentsDeclaration`,
  `JmlFieldDeclaration`, `JmlMethodDeclaration`.
- Model methods: parsed as `JmlMethodDeclaration` wrapping a `MethodDeclaration`
  with a normal `Block` body; JML comments inside bodies are turned into
  statements by `JmlProcessor` (including `assert … \by {…}`), so the PR's
  `mbody_block` relaxation needs **no parser work** — only a corpus test.
- Keyword-with-position precedent: `Modifier` is an AST **node** wrapping the
  `DefaultKeyword` enum (`ast/Modifier.java`), so each keyword is searchable /
  positionable. `ObservableProperty.KEYWORD` exists.

## 3. Port scope

In scope for JMLtk:

1. `lemma` class-body declarations (`JmlLemmaDeclaration`) in the KeY proof-script form
   `lemma Name(params) \by {…}`. Note: KeY's grammar is
   `lemma_declaration: LEMMA IDENT param_list assertionProof SEMI_TOPLEVEL` with
   `assertionProof: BY (proofCmd | LBRACE (proofCmd)+ RBRACE)` — the `\by` script
   is **mandatory**. The definitional `lemma Pred(int i) = i > 0` and asserted
   `lemma … assert e` forms are deliberately **not** ported in this PR.
2. `use_lemma` **proof command** inside `\by {…}` scripts, parsed the way
   `obtain` is parsed (contextual keyword, no new lexer token), producing a
   `JmlProofCmdStmt`.

Out of scope *by decision*:

- `use_lemma` as a **method-level JML statement** (KeY
  `use_lemma_statement`/`methodlevel_element`). JMLtk's port treats `use_lemma`
  solely as a proof-script command (`JmlProofCmdStmt`), never as a
  `JmlExpressionStmt`/method-body statement. Consequence: the PR's
  `//@ use_lemma nospace(\array2seq(a));` inside a normal method body does **not**
  parse in this port — it must sit inside a `\by {…}` script. (Model-method
  bodies with `assert … \by { use_lemma …; }` work, since those scripts survive.)
- The KeY-internal machinery (use_lemma proof *rule*, script macros, KeY's own
  `UseLemmaStatement` AST, `ProgramMethod.isLemma()`, KeY pretty-printing).

## 4. Syntax (KeY `lemma_declaration` — proof-script form only)

```java
//@ static no_state lemma nospace(\seq s) \by {
//@   oss; use_lemma nospace(t); …                           // use_lemma proof command
//@ };
```

Grammar sketch (javacc):

```
// JmlClassLevelInner (java.jj:7747): add alternative
| LOOKAHEAD(1) element = JmlLemmaDeclaration(modifiers) { ret.add(element); }

JmlClassLevelDeclaration JmlLemmaDeclaration(ModifierHolder modifier):
{
    SimpleName name; NodeList<Parameter> params;
    NodeList<JmlProofStmt> script;
}
{
    <LEMMA>
    name = SimpleName()
    params = Parameters()
    <BY> script = JmlProofBlock()     // mandatory (KeY assertionProof: BY (...))
    ";"
    { return new JmlLemmaDeclaration(range(modifier.begin, token()),
          modifier.modifiers, name, params, script); }
}
```

```
// Proof-command production (java.jj:7156, obtain-style contextual keyword):
// use_lemma is NOT a lexer keyword here — recognized by semantic lookahead on
// the token image, exactly like "obtain" (java.jj:7162), and produces the
// generic JmlProofCmdStmt.
LOOKAHEAD({ getToken(1).kind == IDENTIFIER && "use_lemma".equals(getToken(1).image) })
<IDENTIFIER> { begin = token(); }
args = JmlProofArgList()
suffix = JmlProofCmdSuffix()
{ return new JmlProofCmdStmt(range(begin, token()),
      new SimpleName("use_lemma"), args, suffix); }
```

Note: no `USE_LEMMA` lexer token, no `JmlStmtKind.USE_LEMMA`, no entry in
`JmlStatement()` — otherwise the generic command branch would stop matching
`use_lemma` inside scripts.

## 5. AST design

### 5.1 No body-form keyword node

Unlike `Modifier`/`DefaultKeyword`, no dedicated keyword node exists for the
lemma body form: the grammar accepts only the Mandatory `\by {…}` script, so
there is nothing to discriminate. (An earlier draft had a `JmlLemmaKeyword`
node wrapping a `DEFINITION`/`ASSERTION`/`SCRIPT` enum — removed when the
`= expr` and `assert e` forms were dropped.)

### 5.2 `JmlLemmaDeclaration` — `ast/jml/body`

```
public class JmlLemmaDeclaration extends JmlClassLevelDeclaration<JmlLemmaDeclaration>
        implements NodeWithModifiers<JmlLemmaDeclaration>,
                   NodeWithAccessModifiers<JmlLemmaDeclaration> {

    private NodeList<Modifier> modifiers;
    private SimpleName name;
    private NodeList<Parameter> parameters;
    private NodeList<JmlProofStmt> script;       // \by {…} — mandatory
}
```

- Mirrors `JmlClassExprDeclaration` (modifiers + name) plus the `JmlExpressionStmt`
  shape; the `script` list is the sole body property.
- All observable properties already exist (`NAME`, `MODIFIERS`, `PARAMETERS`,
  `SCRIPT`) — none are new.
- As learned in the quantarray port: no `TypeCastingGenerator` methods appear on
  `JmlClassLevelDeclaration` (only the six base nodes get casts) — consumers use
  `instanceof`.

### 5.3 `use_lemma` — a `JmlProofCmdStmt`

No new node. Inside a `\by {…}` proof script, `use_lemma nospace(\array2seq(a));`
parses (via the contextual branch, §4) into

```
JmlProofCmdStmt(
    name   = SimpleName("use_lemma"),
    args   = [ JmlProofArg(label=null, expr=nospace(\array2seq(a))) ],
    suffix = [] )
```

Exactly the same shape as `obtain` produces `JmlProofObtainStmt` — plugging into
existing proof-script machinery, printing, LPP, and the LSP/stat/wd visitors
unchanged.

## 6. Semantics

- `lemma Name(params) \by {s};` — the script is the lemma's proof; the lemma
  becomes available as an assumption when expanded (`use_lemma …` inside a
  script cuts on the lemma call and runs its script).
- `use_lemma f(args);` (proof command) expands to an assumption `f(args)`.
  Operand is a call expression (`MethodCallExpr`); validated at
  resolution/lint time, not parse time.

## 7. Port mapping

| KeY PR change | JMLtk port |
|---|---|
| `LEMMA` token | new `<LEMMA: "lemma">` lexer rule (JML token region) + `TokenTypes` KEYWORD classification |
| `lemma_declaration` | `JmlLemmaDeclaration` (Phases 1) + `JmlLemmaDeclaration(ModifierHolder)` production with mandatory `\by` (Phase 2) + CSM/printer (Phase 3) + validation (Phase 4) |
| `USE_LEMMA` token | **none** — `use_lemma` stays an identifier, parsed contextually like `obtain` (§4) |
| `use_lemma_statement` (method-level) | **deferred by decision** (§3); only the proof-command form is ported |
| `use_lemma` proof command | contextual branch in the proof-command production → `JmlProofCmdStmt` (Phase 2) |
| `mbody_block` asserts | no parser change — corpus regression test only (Phase 5) |
| `ModifierKind.JML_LEMMA` (KeY AST) | out of scope — `lemma` is a grammar keyword in JMLtk, not a `Modifier` |
| `UseLemmaStatement` node/rule/macros (KeY) | out of scope (KeY-side consumers) |
| `TextualJMLMethodOrLemmaDecl` rename (KeY) | out of scope; JMLtk keeps `JmlMethodDeclaration` |

## 8. Verification approach

- parse + CSM round-trip + `LexicalPreservingPrinter` round-trip tests;
- corpus fixture `fullexamples/tested/Lemmas.java` (a miniature of the
  VerifyThis 2025.1 `MinExcludant0` example): `\by`-script lemmas (one whose
  script contains `use_lemma`), an `assert … \by {…}` with a proof script,
  inside declared methods (lemma + normal method);
- negative parse/validation cases (missing `\by`, `lemma … = expr;` /
  `lemma … assert e;` rejected, `lemma P(int i) == i > 0;`);
- `TokenTypesTest` (new `LEMMA` token classified), `ConcreteSyntaxModelTest`,
  full corpus (`FragmentTest`, `FullExamplesTest`) green;
- backends: no-crash + minimal semantics (stat/LSP/wd), jml2java/smt no-op.
