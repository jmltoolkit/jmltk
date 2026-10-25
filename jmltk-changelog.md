# JMLtk — J/K Release Changelog

Complementary to [`changelog.md`](./changelog.md), which documents the upstream JavaParser releases.

This file documents the fork releases that carry the **J** (JML) and **K** (KeY) feature sets, reconstructed
from the git history of the two forks that were merged into this repository:

- **KeY fork** — [wadoon/key-javaparser](https://github.com/wadoon/key-javaparser): releases `K1` … `K13.6`.
- **JML fork** — [jmltoolkit/jmltk](https://github.com/jmltoolkit/jmltk) (formerly `wadoon/jmlparser`): releases
  `-b2` … `-b7` before the merge, then the combined `-J<JML>-K<KeY>` scheme.

Commit hashes are given for every entry so that each claim can be verified with `git show <hash>` / `git log`.

## Version scheme

```
<JavaParser>-J<JML>-K<KeY>      e.g. 3.28.2-J8.0-K13.6  (next release, prepared)
```

- **J** counts the JML (Java Modeling Language) feature level — JML AST nodes (`ast/jml/**`), JML grammar, JMLDoc,
  contracts, stipulations (`\by` scripts), etc. Built in the JML fork since 2022.
- **K** counts the KeY feature level — KeY-specific AST nodes (`KeyCatchBranch`, `KeyMarkerStatement`,
  `ArrayLengthValueDeclaration`, `LogicalType`, …), ghost variables/modifiers, method-frame resolution, KeY-style
  semantics. Developed in the KeY fork (key-javaparser) since 2021.
- The combined scheme starts with `3.28.0-J7.0-K10.1-SNAPSHOT` (2026-02-03), when `key/master` was merged into
  the JML fork; the J count continued as J7.0 → J8.0, while the K count continued from K10.1 (KeY fork) through
  K13.6.

Release markers found in the history (tags): `k1`, `k2`, `k3`, `v3.25.3-K3`, `v3.25.3-K5`, `K11.1`,
`KEY-JAVAPARSER-3.28.0-K13.6`. Old releases were published from `pom.xml` (Maven); since the July 2026 Gradle
migration the version lives in `gradle.properties`.

---

## Combined releases (JML + KeY, this repository)

### Version 3.28.2-J8.0-K13.6  (2026-10-25, prepared)

Second combined release. Completes the K13.6 feature level on this lineage: the KeY fork published K13.6 on
2026-07-20 (`c26f62ea3`, tagged `KEY-JAVAPARSER-3.28.0-K13.6`), but that fix never crossed back into the JML
fork's release line — this release cherry-picks it. Version commit: `0b2a7a359` "prepare release
3.28.2-J8.0-K13.6"; cherry-pick commit: `183af569e`.

**Fixed**

- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive
  integral type — cherry-picked from the KeY fork (`c26f62ea3` → `183af569e`): during the recursive descend
  of an array type the primitive check now uses `equals` instead of `isAssignableBy`.
- The other three K13.6 fixes from the KeY fork were already present in `3.28.2-J8.0-K13.5`: weak
  `JavaParserFacade` cache (`febe5a8a9`), call-context threading through `solveMethodAsUsage` (`f6b2260d4`),
  and the missing `ConcreteSyntaxModel` classes (`a8bb0355b`).

### Version 3.28.2-J8.0-K13.5  (2026-10-25)

First combined release, and the first release built with Gradle instead of Maven. Base
JavaParser is 3.28.2. Release commit: `82b6d7869`/`b66825037` "prepare release".

**Added**

- JML contracts on `LambdaExpression` (PR #60, `82b57d207`)
- Contracts on `CompactConstructorDeclaration` (PR #59, `9bfabd52e`)
- KeY integration updates — "changes for KeY" (PR #54, `cbb924bbd`): grammar/`JavaToken` handling for KeY
  ghost syntax, `GhostTest`
- LSP distribution revived (PR #55, `0a68a74e2`)
- JRE stub generation restored — "Stubby is back" (PR #35, `5106df9a3`)
- `Stat` tool (PR #36, `f3cb5f207`)
- `JmlQuantifiedExpr` now uses `VariableDeclarator` (PR #48, `418503588`); `JmlLabeledClause` renamed (PR #49, `caadb91ac`)
- Test suite re-activated (PR #21, `570aec9d9`)

**Changed**

- Build migrated to Gradle; Maven build files removed (`d13918922`, `a384db3bc`); Spotless/Checkstyle alignment
  (`e0ee2bab4`, `1fb8c8376`); default branch `master` → `main` (`b5eb7957b`)
- CodeQL workflow added; SonarQube removed (`4a724e15e`, `3913e89c2`)
- Upstream JavaParser 3.28.1 and 3.28.2 merged (includes phase-2 poly-expression inference for method/constructor
  references, multi-pattern case-label rules, OSGi support)

**Fixed**

- Grammar: precise error positions for malformed variable declarations (PRs #71/#72, `6782cb6fc`, `3da7c7825`)
- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive integral
  type (`7883cd9d0`)
- CodeQL findings: unused imports, statements with no effect, unclosed files (`14327cd25`, `5194c58f1`,
  `a3c1c9d5f`, `7795d099d`)

---

## KeY releases (wadoon/key-javaparser)

### Version 3.28.0-K13.6  (2026-07-20)

Tagged `KEY-JAVAPARSER-3.28.0-K13.6`. Release commit: `056533f92` "release 3.28.0-K13.6".

**Fixed**

- `StackOverflowError`: thread the call context through `solveMethodAsUsage` (`f6b2260d4`)
- Memory leak: `JavaParserFacade` cache now holds its values weakly (`febe5a8a9`)
- Method resolution for overloaded methods with array-typed arguments when the basetype is a primitive integral
  type (`c26f62ea3`)
- Missing `ConcreteSyntaxModel` classes (`a8bb0355b`)

### Version 3.28.0-K13.5  (2026-07-04)

Release commit: `7ecfed818` "the version 3.28.0-K13.5".

**Fixed**

- Method-frame resolution (`c863d4768`)

### Version 3.28.0-K13.4  (2026-03-05)

Release commit: `dd2ad4fba` "Fix protected visibility rules and version bump".

**Fixed**

- Protected-visibility rules in the symbol resolver

### Version 3.28.0-K13.3  (2026-03-05)

`b5a850cbf` "Fix internal types test and bump version" — internal-types handling.

### Version 3.28.0-K13.2  (2026-03-05)

`0d48924ca` "inner classes can see anything" — inner classes can resolve members of their enclosing scopes.

### Version 3.28.0-K13.1  (2026-03-04)

`6a841a2e3` "Bump to 13.1" — minor release after K13.0.

### Version 3.28.0-K13.0  (2026-03-04)

**Added**

- `KeyMarkerStatement` AST node (`e92599acb`)

### Version 3.28.0-K12.10  (2026-03-04)

**Changed**

- Method resolution now considers the call context (`71f74fb40` + bump `566636ea1`)

### Version 3.28.0-K12.9  (2026-03-04)

`4fbdf1492` "K12.9" — minor release.

### Version 3.28.0-K12.8  (2026-03-04)

`08c4721bc` "K12.8" — minor release.

### Version 3.28.0-K12.7  (2026-03-04)

`3ad29b010` "set version to K12.7" — minor release.

### Version 3.28.0-K12.5  (2026-03-03)

Release commit: `4564ffe65` "set version to K12.5". Covers the K11.2 → K12.5 iterations.

**Added**

- JML commentary handling in the parser (`a944396b3`)
- JMLDoc support for KeY AST nodes — "K12.2 now with JMLDoc" (`6a260468d`)
- Setters and pretty printing for the JML/KeY nodes (`e8da511eb`); missing meta classes (`12a358398`)

**Fixed**

- `AssignableToPrimitive` interface (`7ac76ac42`, K11.2)
- Resolution inside `KeyCatchBranch` now respects its parameters (`de35187e1`)
- Error in `JmlDocsStatements` (`d5b953858`)

### Version 3.28.0-K11.1  (2026-03-02)

Tagged `K11.1`. Release commit: `e09ddd7b0` "AssignableToPrimitive interface". This entry also covers the
development iterations since K7, which the fork versioned as K8…K11-pre and K10.1:

**Added**

- `AssignableToPrimitive` interface (`e09ddd7b0`)
- `ArrayLengthValueDeclaration` opened up (K10, `bd3e650ac`)
- Non-`Optional` getters using nullable (JSpecify) annotations (K11-pre, `6125dfb99`); JSpecify annotations on
  setters and return values (K10.1, `bcce6b70c`)
- New annotation to avoid lexical-preserving-printer errors caused by dynamic method resolution (`3004dfd69`)
- Catch-clause groundwork — "catch is required" (`3ee9eba0f`, K8 iteration)

**Fixed**

- `KeyCatchClauseSV` (K9, `d325b863d`)
- Static `JavaParser` disabled in the tooling path (`c2507bfdd`)

**Changed**

- Base rebased from 3.26.3 via 3.27.0/3.27.1 to 3.28.0 (upstream merges `71dcd5c56`, `55348354e`, `6dbb2be00`)
- Grammar rule rename (`aaba2f83f`)

### Version 3.26.3-K7  (2024-12-23)

"Fix test cases, release complete" (`ca6d9dc97`, `86115cb03`). Base JavaParser 3.26.3; the fork tracked upstream
3.25.4 → 3.25.8 → 3.26.2 → 3.26.3 in this cycle and prepared the KeY catch-clause support released in the
following iterations.

**Changed**

- Upstream JavaParser 3.25.4 → 3.26.3 tracked (`67dcf3c50`, `afaffe24e`, `c37b0d5b0`, …); base rebased to 3.25.8
  (`d38558571`) and finally 3.26.3 (`874efbc1f`, `24dd80a0a`)

### Version 3.25.3-K6  (2023-06-29)

"New release" (`dd2a13d15`).

**Changed**

- Ghost-modifier pretty printing (`424e7e5b6`)

### Version 3.25.3-K5  (2023-06-23)

Tagged `v3.25.3-K5`. Release commit: `d0eb777bc` "relese K5: KeY as class types".

**Added**

- "KeY as class types": new `LogicalType` AST node and `LogicalTypes` utility, grammar updates (`d0eb777bc`)
- `javaparser-key-testing` module

**Fixed**

- Pretty-printer holes for the KeY constructs — issues #2/#3/#4 (`89724d7f0`, `c8223e169`, `e65ab3f05`)
- `setTransactionType` in constructor (issue #5, `c92594cd1`)

### Version 3.25.2-K3  (2023-04-17)

Tagged `k3`. Release commit: `af7303951` "version K3".

**Changed**

- Java 17 sealed/non-sealed classes support (`25b12b3ed`)
- Empty `BlockStmt` without range no longer errors (`9f925d5a7`)

(Previous iteration K2, `3.25.2-K2-SNAPSHOT`/tag `k2`, added method-frame name resolution — `490278748`.)

### Version 3.24.1-K1  (2022-03-04)

Tagged `k1`. Release commit: `211d31f46` "set non-snapshot" (merge of the `weigl/keyadditions` branch,
`027c37505`). Initial KeY fork release.

**Added**

- KeY syntax / proof parser (`9810b8ded`, `f8ecacf87`) — the basis of `\by` proof scripts
- KeY's modifiers and implicit identifiers (`b650a92fe`); `KEYIDENTIFIER` changed to the `$<id>` token rule
  (`e945cc169`)
- `Key`-prefixed AST classes (`KeyTypeSV` et al.; KeY-prefix classes renamed to a `Key` prefix, `2c07a5c50`;
  `KeyTypeSV` attribute renamed `name` → `text`, `7e8783227`)

---

## JML releases (this repository, before the KeY merge)

### Version 3.26.2-b7  (2025-01-23)

"Release 3.26.2-b7" (`54a11df57`).

**Added**

- Generation of annotated getters without `Optional` (`870b5ce70`)
- Javadoc-error-ignorance fix and token-types test fix (`ccdd9901a`, `a6decfba8`)

**Fixed**

- Regression in the printer (`82e3e91b2`)

### Version 3.26.2-b6  (2024-11-15)

"Set version to 3.26.2-b6-SNAPSHOT" (`b89f51c48`) after merging upstream 3.26.2 — "fix merge errors" (`cb46f5b57`).

### Version 3.25.10-b6  (2024-05-23)

**Fixed**

- Missing JML delimiters in pretty printing (`72507343c`)

### Version 3.25.10-b3  (2024-05-04)

**Changed**

- Clean-up of deprecated JML AST classes (`5a1852c82`)
- Renaming of AST nodes (`bfaa6b1c2`)
- Docs moved to the GitHub wiki (`4ea8d929f`)

### Version 3.25.10-b2  (2024-05-03)

**Added**

- `JmlClauseIfClause` and `JmlCallableClause` (`f2ef7f2f0`)
- JML clauses in `TypeCastingGenerator` (`0f20de0d6`); contract lists in methods/constructors are mandatory
  (`ba3385f58`); meta-model regeneration (`30ce07988`)

---

## Version history (all distinct J/K version states in the git history)

| Version state | Date | Commit / tag | Fork |
|---|---|---|---|
| `3.24.1-K1-SNAPSHOT` | 2022-03-04 | `49afba28d` | KeY |
| `3.24.1-K1` | 2022-03-04 | `211d31f46`, tag `k1` | KeY |
| `3.24.1-K2-SNAPSHOT` | 2022-03-04 | `2ea2789e0` | KeY |
| `3.25.2-K2-SNAPSHOT` | 2023-04-15 | `e5d29e4a7`, tag `k2` | KeY |
| `3.25.2-K3-SNAPSHOT` | 2023-04-17 | `af7303951`, tag `k3` | KeY |
| `3.25.2-K4-SNAPSHOT` | 2023-05-24 | `575a36ac1` | KeY |
| `3.25.3-K4-SNAPSHOT` | 2023-05-24 | `185b6a8a5`, tag `v3.25.3-K3` | KeY |
| `3.25.3-K5-SNAPSHOT` | 2023-05-24 | `bd8db3116` | KeY |
| `3.25.3-K5` | 2023-06-23 | `d0eb777bc`, tag `v3.25.3-K5` | KeY |
| `3.25.3-K6-SNAPSHOT` | 2023-06-23 | `14997a791` | KeY |
| `3.25.3-K7-SNAPSHOT` | 2023-06-29 | `dd2a13d15` (release of K6) | KeY |
| `3.25.8-K7-SNAPSHOT` | 2024-02-03 | `d38558571` | KeY |
| `3.26.2-K7-SNAPSHOT` | 2024-12-09/21 | `4739d5750`, `874efbc1f` | KeY |
| `3.26.3-K7-SNAPSHOT` | 2024-12-21/23 | `24dd80a0a`, `ca6d9dc97` (release of K7) | KeY |
| `3.26.3-K8-SNAPSHOT` | 2024-12-26 | `3ee9eba0f` | KeY |
| `3.26.3-K9-SNAPSHOT` | 2025-01-03 | `d325b863d` | KeY |
| `3.26.3-K10-SNAPSHOT` | 2025-01-08/11 | `bd3e650ac`, `6125dfb99` (K11-pre) | KeY |
| `3.27.0-K10-SNAPSHOT` | 2025-06-19 | `63f374098` | KeY |
| `3.28.0-K10-SNAPSHOT` | 2026-02-01/02 | `6dbb2be00`, `86a694b8d` | KeY |
| `3.28.0-K10.1-SNAPSHOT` | 2026-02-02 | `2d516a68e` | KeY |
| `3.28.0-J7.0-K10.1-SNAPSHOT` | 2026-02-03 | `a096d093f` (first combined) | JML (jmltk) |
| `3.28.0-J8.0-K10.1-SNAPSHOT` | 2026-02-03 | `02012f2ca` | JML (jmltk) |
| `3.28.0-K11.1-SNAPSHOT` | 2026-03-02 | `e09ddd7b0`, tag `K11.1` | KeY |
| `3.28.0-K12.0/12.2/12.3-SNAPSHOT` | 2026-03-03 | `a944396b3`, `6a260468d`, `de35187e1` | KeY |
| `3.28.0-K12.5` | 2026-03-03 | `4564ffe65` | KeY |
| `3.28.0-K12.7` | 2026-03-04 | `3ad29b010` | KeY |
| `3.28.0-K12.8` | 2026-03-04 | `08c4721bc` | KeY |
| `3.28.0-K12.9` | 2026-03-04 | `4fbdf1492` | KeY |
| `3.28.0-K12.10` | 2026-03-04 | `566636ea1` | KeY |
| `3.28.0-K13.0` | 2026-03-04 | `e92599acb` | KeY |
| `3.28.0-K13.1` | 2026-03-04 | `6a841a2e3` | KeY |
| `3.28.0-K13.2` | 2026-03-05 | `0d48924ca` | KeY |
| `3.28.0-K13.3` | 2026-03-05 | `b5a850cbf` | KeY |
| `3.28.0-K13.4` | 2026-03-05 | `dd2ad4fba` | KeY |
| `3.28.0-J8.0-K13.4` | 2026-03-07 | `de600d8e9` | JML (jmltk) |
| `3.28.1-J8.0-K13.4` | 2026-05-10 | `1d5f30f1b` | JML (jmltk) |
| `3.28.1-J8.0-K13.6-SNAPSHOT` | 2026-07-04 | `085061d4a` | JML (jmltk) |
| `3.28.0-K13.5` | 2026-07-04 | `7ecfed818` | KeY |
| `3.28.1-J8.0-K13.5-SNAPSHOT` | 2026-07-06 | `a8238e94a` | JML (jmltk) |
| `3.28.2-J8.0-K13.5-SNAPSHOT` | 2026-07-06 | `acf8dc398` | JML (jmltk) |
| `3.28.0-K13.6` | 2026-07-20 | `056533f92`, tag `KEY-JAVAPARSER-3.28.0-K13.6` | KeY |
| `3.28.0-K13.7-SNAPSHOT` | 2026-07-20 | `0336c93d6` | KeY |
| `3.28.2-J8.0-K13.5` | 2026-10-25 | `82b6d7869` (release) | JML (jmltk) |
| `3.28.2-J8.0-K13.6` | 2026-10-25 | `0b2a7a359` (prepare release) | JML (jmltk) |

JML `-b` iterations: `3.25.10-b2` (`f2ef7f2f0`, 2024-05-03), `3.25.10-b3` (`6cca21e71`, 2024-05-04),
`3.25.10-b6` (`72507343c`, 2024-05-23), `3.26.2-b6` (`b89f51c48`, 2024-11-15), `3.26.2-b7`
(`54a11df57`, 2025-01-23), then `3.27.0-b7-SNAPSHOT` / `3.27.1-b7-SNAPSHOT` until the KeY merge
(`71dcd5c56`, 2025-06-19 / `55348354e`, 2025-12-31).
