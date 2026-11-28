# Grammar Parsing Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make JMLtk Java/JML parsing measurably faster through `java.jj` changes, with a committed benchmark proving each win.

**Architecture:** First add a repeatable parse benchmark (`tools/bench`) that separates grammar cost from validation cost. Then optimize the grammar in measured steps: an O(1) identifier-FIRST-set guard (P1), removal of redundant numeric lookaheads (P2), optional JML gating (P3), and an optional lexer investigation (P4). Every step is verified by tests, corpus parse parity, and before/after medians.

**Tech Stack:** Java 21, JavaCC/ParserGeneratorCC 3.0.0, Gradle wrapper, Kotlin 2.x (`standard-kotlin` convention), JUnit 5, AssertJ, Gson.

**Spec:** `doc/design/grammar-parsing-performance.md`

## Global Constraints

- Use `./gradlew` (wrapper) only; JDK 21 is the toolchain.
- Never hand-edit generated sources under `javaparser-core/build/`; edit `java.jj` and run `./gradlew :jmlparser-core:compileJavacc`.
- After grammar changes run `./gradlew spotlessCheck checkstyleMain`; before merge run `./scripts/run-generators.sh` and confirm no unexpected diff.
- New files need the license header from `gradle/header`.
- Do not stage or commit the pre-existing local modification to `gradle.properties`.
- Measurement rule: ≥3 warmup rounds, ≥5 measured rounds, compare medians, treat <3% as noise, and never ship a change that regresses either corpus by more than noise.
- No AST shape, API, ambiguity-resolution, or error-recovery changes.
- Every user-visible change gets an entry in `jmltk-changelog.md` under the current `Unreleased` section.

## Review Focus

These are the input classes most likely to bite; each has its owning test below.

1. **Truncated/EOF input during speculative lookahead** — parsing `"class X { Object "` or `"class X { void m() { a."` must return a `ParseResult`, never throw from a new predicate. Test in Task 2.
2. **Keyword-like tokens still accepted as identifiers** — `_`, `enum`, `record`, `yield`, and every JML clause word that `Identifier()` accepted must still parse in identifier position. Exhaustive test in Task 2.
3. **JML markers with `processJml=false`** — must keep the current behavior if gating would change it; full `:jmlparser-jml-tests:test` plus a targeted test in Task 4.
4. **Scoped/generic types after removing lookaheads** — `a.b.C<D>.E<F>` and `Outer<T>.Inner` must parse to the same AST. Test in Task 3.
5. **`--verify` mismatch must fail loudly** — a different ok/fail file set than the baseline must exit nonzero. Test in Task 1.

---

## Task 1: Benchmark harness `tools/bench`

**Files:**
- Create: `tools/bench/build.gradle.kts`
- Create: `tools/bench/src/main/kotlin/io/github/jmltoolkit/bench/Bench.kt`
- Create: `tools/bench/src/main/kotlin/io/github/jmltoolkit/bench/Main.kt`
- Create: `tools/bench/src/test/kotlin/io/github/jmltoolkit/bench/BenchTest.kt`
- Create: `tools/bench/baseline/*.json` (four generated files)
- Modify: `settings.gradle.kts`
- Modify: `tools/README.md`

**Interfaces:**
- Produces:
  - `enum class BenchMode { GRAMMAR, FULL }`
  - `data class BenchConfig(val corpus: String, val mode: BenchMode, val warmup: Int, val rounds: Int, val json: java.nio.file.Path?, val baseline: java.nio.file.Path?, val verify: Boolean)`
  - `fun parseBenchConfig(args: Array<String>): BenchConfig`
  - `fun median(values: List<Double>): Double`
  - `fun percentDelta(baseline: Double, current: Double): Double`
  - `data class RoundSample(val millis: Double, val megabytesPerSecond: Double)`
  - `data class BenchResult(val corpus: String, val mode: BenchMode, val files: Int, val bytes: Long, val ok: Int, val fail: Int, val failures: List<String>, val rounds: List<RoundSample>, val medianMillis: Double)`
  - `fun runBenchmark(config: BenchConfig): BenchResult`
  - `fun writeResult(result: BenchResult, path: java.nio.file.Path)`
  - `fun readResult(path: java.nio.file.Path): BenchResult`
  - `fun verifyParity(baseline: BenchResult, current: BenchResult): List<String>` (empty list means equal)

- [ ] **Step 1: Wire the module**

Add `include(":tools:bench")` to `settings.gradle.kts` after `include(":tools:vcg")`.

Create `tools/bench/build.gradle.kts`:

```kotlin
plugins {
    id("standard-kotlin")
    application
}

dependencies {
    implementation(libs.gson)
}

application {
    mainClass = "io.github.jmltoolkit.bench.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
```

- [ ] **Step 2: Write the failing unit tests**

Create `tools/bench/src/test/kotlin/io/github/jmltoolkit/bench/BenchTest.kt` with tests that assert:

```kotlin
class BenchTest {
    @Test fun `parses corpus and mode`() {
        val c = parseBenchConfig(arrayOf("--corpus", "jml", "--mode", "grammar", "--rounds", "3", "--warmup", "1"))
        assertEquals("jml", c.corpus)
        assertEquals(BenchMode.GRAMMAR, c.mode)
        assertEquals(3, c.rounds)
        assertEquals(1, c.warmup)
    }

    @Test fun `rejects unknown mode`() {
        assertThrows<IllegalArgumentException> { parseBenchConfig(arrayOf("--corpus", "plain", "--mode", "fast")) }
    }

    @Test fun `median handles odd and even sizes`() {
        assertEquals(2.0, median(listOf(1.0, 3.0, 2.0)))
        assertEquals(2.5, median(listOf(1.0, 4.0, 2.0, 3.0)))
    }

    @Test fun `percent delta is signed relative change`() {
        assertEquals(-20.0, percentDelta(100.0, 80.0), 1e-9)
        assertEquals(5.0, percentDelta(100.0, 105.0), 1e-9)
    }

    @Test fun `json round trip`() {
        val result = BenchResult("plain", BenchMode.FULL, 2, 10, 1, 1, listOf("a/B.java"),
            listOf(RoundSample(1.0, 2.0)), 1.0)
        val file = Files.createTempFile("bench", ".json")
        writeResult(result, file)
        assertEquals(result, readResult(file))
    }

    @Test fun `verify reports a changed ok set`() {
        val baseline = BenchResult("plain", BenchMode.FULL, 2, 10, 1, 1, listOf("a/B.java"),
            listOf(RoundSample(1.0, 2.0)), 1.0)
        val current = baseline.copy(fail = 0, ok = 2, failures = emptyList())
        assertFalse(verifyParity(baseline, current).isEmpty())
        assertTrue(verifyParity(baseline, baseline).isEmpty())
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :tools:bench:test`
Expected: compilation failure because `parseBenchConfig`, `median`, `percentDelta`, `BenchResult`, `RoundSample`, `writeResult`, `readResult`, `verifyParity` do not exist.

- [ ] **Step 4: Implement config, statistics, and JSON**

In `Bench.kt`, implement exactly the interfaces above. `parseBenchConfig` accepts `--corpus`, `--mode`, `--warmup`, `--rounds`, `--json`, `--baseline`, `--verify`; unknown option or invalid value throws `IllegalArgumentException`. Use Gson (`com.google.gson.GsonBuilder().setPrettyPrinting()`) for read/write.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :tools:bench:test`
Expected: BUILD SUCCESSFUL, 5 tests passed.

- [ ] **Step 6: Implement the runner and CLI**

`runBenchmark`:
- `plain` corpus roots: `javaparser-core/src/main/java` and `javaparser-symbol-solver-core/src/main/java`.
- `jml` corpus root: `jmlparser-jml-tests/src/test/resources/fullexamples`; configuration has `setProcessJml(true)`.
- `path:<dir>` corpus: the directory after `path:`; JML off.
- Collect `.java` files sorted, `Files.readString` all sources before the first round.
- `GRAMMAR` mode: `ParserConfiguration().setProcessJml(jml).also { it.processors.clear() }`.
- `FULL` mode: default configuration.
- Per round: parse every source once; count ok/fail and record failing relative paths.
- `--verify`: when both `--baseline` and `--verify` are given, call `verifyParity`; if it returns anything, print the differing paths and exit with code 1.
- `Main.kt` calls `runBenchmark`, prints one line per round plus median, writes `--json`, and prints `percentDelta` per field when `--baseline` is given.

- [ ] **Step 7: Add usage docs**

Append to `tools/README.md`:

```markdown
## Parse benchmark

`tools/bench` measures `JavaParser` throughput and separates grammar cost from validation cost.

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --rounds 5"
./gradlew :tools:bench:run --args="--corpus jml --mode full --rounds 5 --json /tmp/jml-full.json"
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
```

Corpora: `plain`, `jml`, or `path:<directory>`. Modes: `grammar` (validators disabled) and `full`.
```

- [ ] **Step 8: Record the baseline**

Run each command and commit the output JSONs:

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --warmup 3 --rounds 5 --json tools/bench/baseline/plain-grammar.json"
./gradlew :tools:bench:run --args="--corpus plain --mode full    --warmup 3 --rounds 5 --json tools/bench/baseline/plain-full.json"
./gradlew :tools:bench:run --args="--corpus jml   --mode grammar --warmup 3 --rounds 5 --json tools/bench/baseline/jml-grammar.json"
./gradlew :tools:bench:run --args="--corpus jml   --mode full    --warmup 3 --rounds 5 --json tools/bench/baseline/jml-full.json"
```

- [ ] **Step 9: Run formatting and commit**

Run: `./gradlew :tools:bench:test spotlessCheck`
Then:

```bash
git add settings.gradle.kts tools/bench tools/README.md
git commit -m "bench: add tools/bench parse benchmark"
```

---

## Task 2: P1 — O(1) identifier rejection in lookaheads

**Files:**
- Modify: `javaparser-core/src/main/java/com/github/javaparser/TokenTypes.java`
- Modify: `javaparser-core/src/main/javacc/java.jj` (`Identifier()`)
- Create: `javaparser-core-testing/src/test/java/com/github/javaparser/IdentifierLookaheadTest.java`

**Interfaces:**
- Produces: `public static boolean TokenTypes.isIdentifierKind(int kind)`
- Consumes: `GeneratedJavaParserConstants.tokenImage`, existing `JavaParser`

- [ ] **Step 1: Write the failing consistency test**

Create `IdentifierLookaheadTest` in package `com.github.javaparser`:

```java
@Test
void identifierPredicateMatchesParserForKeywordTokens() {
    String[] images = GeneratedJavaParserConstants.tokenImage;
    for (int kind = 0; kind < images.length; kind++) {
        String image = images[kind];
        if (!image.startsWith("\"") || !image.endsWith("\"")) continue;
        String text = image.substring(1, image.length() - 1);
        if (!text.matches("[A-Za-z_$][A-Za-z0-9_$]*")) continue;
        boolean expected = TokenTypes.isIdentifierKind(kind);
        boolean parsed = new JavaParser().parse("class X { Object " + text + "; }").isSuccessful();
        assertEquals(expected, parsed, "kind=" + kind + " text=" + text);
    }
}

@Test
void namedIdentifierTokensAreIdentifierKinds() {
    assertTrue(TokenTypes.isIdentifierKind(GeneratedJavaParserConstants.IDENTIFIER));
    assertTrue(TokenTypes.isIdentifierKind(GeneratedJavaParserConstants.JML_IDENTIFIER));
    assertTrue(TokenTypes.isIdentifierKind(GeneratedJavaParserConstants.SVIDENTIFIER));
    assertTrue(TokenTypes.isIdentifierKind(GeneratedJavaParserConstants.KEYIDENTIFIER));
}

@Test
void truncatedInputDoesNotThrow() {
    assertDoesNotThrow(() -> new JavaParser().parse("class X { Object "));
    assertDoesNotThrow(() -> new JavaParser(new ParserConfiguration().setProcessJml(true)).parse("class X { void m() { a."));
}
```

- [ ] **Step 2: Add a false stub and run the test to verify it fails**

Add `public static boolean isIdentifierKind(int kind) { return false; }` to `TokenTypes`, then run:

`./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.IdentifierLookaheadTest"`

Expected: FAIL on keyword tokens such as `invariant`, `enum`, `record`, `_` with `expected=true, parsed=true`.

- [ ] **Step 3: Implement the exact predicate**

Replace the stub with a `switch (kind)` returning `true` for exactly the token kinds listed in `Identifier()` in `java.jj` (the four identifier tokens, the module/older-Java/Key literals, and the JML clause words). Extract the list mechanically:

```bash
sed -n '3559,3604p' javaparser-core/src/main/javacc/java.jj | grep -oE '<[A-Z_0-9]+>' | tr -d '<>'
```

For the quoted literals in that block (`"source"`, `"exec"`, `"ccatch"`, `"ccat"`), use the corresponding constant names found by matching their images in `GeneratedJavaParserConstants.tokenImage`. `default: return false;`

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.IdentifierLookaheadTest"`
Expected: PASS, including truncated-input tests.

- [ ] **Step 5: Add the semantic guard to the grammar**

Change `Identifier()` in `java.jj` to:

```java
String Identifier():
{
    String ret;
}
{
    LOOKAHEAD({ isIdentifierKind(getToken(1).kind) })
    (
        ... existing alternative list unchanged ...
    ) { ret = token().getText(); setTokenKind(IDENTIFIER);}
    { return ret; }
}
```

- [ ] **Step 6: Regenerate and run the parser test suites**

```bash
./gradlew :jmlparser-core:compileJavacc
./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.IdentifierLookaheadTest"
./gradlew :jmlparser-core-testing:test :jmlparser-jml-tests:test
```

Expected: all pass.

- [ ] **Step 7: Measure**

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
./gradlew :tools:bench:run --args="--corpus jml   --mode grammar --baseline tools/bench/baseline/jml-grammar.json --verify"
./gradlew :tools:bench:run --args="--corpus plain --mode full    --baseline tools/bench/baseline/plain-full.json --verify"
./gradlew :tools:bench:run --args="--corpus jml   --mode full    --baseline tools/bench/baseline/jml-full.json --verify"
```

Record the deltas. If no corpus improves beyond noise, revert all Task 2 changes (`java.jj`, the predicate, and the test) and record the negative result in `doc/design/grammar-parsing-performance.md` instead of shipping.

- [ ] **Step 8: Format, verify generators, and commit**

```bash
./gradlew spotlessCheck checkstyleMain
./scripts/run-generators.sh && git diff --exit-code
git add javaparser-core/src/main/java/com/github/javaparser/TokenTypes.java \
        javaparser-core/src/main/javacc/java.jj \
        javaparser-core-testing/src/test/java/com/github/javaparser/IdentifierLookaheadTest.java
git commit -m "perf(grammar): O(1) identifier rejection in lookaheads

plain/grammar: ...% ; jml/grammar: ...% (fill in measured deltas)"
```

---

## Task 3: P2 — remove redundant numeric lookaheads

**Files:**
- Modify: `javaparser-core/src/main/javacc/java.jj`
- Create: `javaparser-core-testing/src/test/java/com/github/javaparser/TypeLookaheadTest.java`

**Interfaces:**
- Consumes: `JavaParser`, existing AST classes.

- [ ] **Step 1: Write the regression tests (they pass before the change)**

```java
@Test
void parsesScopedGenericTypes() {
    var result = new JavaParser().parse("class X { a.b.C<D>.E<F> f; Outer<T>.Inner<U> g; }");
    assertTrue(result.isSuccessful());
    var types = result.getResult().orElseThrow().findAll(ClassOrInterfaceType.class);
    assertTrue(types.stream().anyMatch(t -> t.getNameAsString().equals("E")
            && t.getTypeArguments().orElseThrow().size() == 1
            && t.getScope().orElseThrow().getNameAsString().equals("C")));
    assertTrue(types.stream().anyMatch(t -> t.getNameAsString().equals("Inner")
            && t.getScope().orElseThrow().getNameAsString().equals("Outer")));
}

@Test
void parsesCommaAndSemicolonLists() {
    assertTrue(new JavaParser().parse("enum E { A, B, }").isSuccessful());
    assertTrue(new JavaParser().parse("class X { int[] a = {1, 2,}; }").isSuccessful());
    assertTrue(new JavaParser().parse("import java.util.List;;").isSuccessful());
}
```

Run: `./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.TypeLookaheadTest"`
Expected: PASS.

- [ ] **Step 2: Remove the two redundant type-argument lookaheads**

In `ClassOrInterfaceType()`, replace both occurrences of `[ LOOKAHEAD(2) typeArgs = TypeArguments() ]` with `[ typeArgs = TypeArguments() ]` (they are near `java.jj` lines 3319 and 3326 before earlier tasks shift them). Do **not** remove the `LOOKAHEAD(2)` on the `.` scope-loop (`LOOKAHEAD(2) "." annotations = Annotations() name = SimpleName()`): `.` can also begin `.class`/`.this` in callers, so that lookahead is required.

- [ ] **Step 3: Regenerate and run the regression test**

```bash
./gradlew :jmlparser-core:compileJavacc
./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.TypeLookaheadTest"
```

Expected: PASS.

- [ ] **Step 4: Remove redundant separator lookaheads**

Remove the `LOOKAHEAD(2)` token before a body that starts with a unique separator, at these currently line-numbered sites (locate by content; earlier tasks shift the numbers):

- `( LOOKAHEAD(2) ";" )*`
- `( LOOKAHEAD(2) "," entry = EnumConstantDeclaration() ... )*`
- `( LOOKAHEAD(2) "," val = VariableInitializer() ... )*`
- `(LOOKAHEAD(2) ";")?`

Each body's FIRST set is exactly `;` or `,`, which cannot begin the surrounding follow set; if any site's surrounding rule contradicts that, leave it unchanged and note why in the commit message.

- [ ] **Step 5: Regenerate, test, and measure**

```bash
./gradlew :jmlparser-core:compileJavacc
./gradlew :jmlparser-core-testing:test :jmlparser-jml-tests:test
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
./gradlew :tools:bench:run --args="--corpus jml   --mode grammar --baseline tools/bench/baseline/jml-grammar.json --verify"
```

Keep the change only if at least one corpus improves beyond noise and neither regresses. Otherwise revert.

- [ ] **Step 6: Do not touch `Statement()` unless still hot**

Post-P1, re-run the JFR profile (same command shape as the spec's baseline). If generated-parser samples are below 10% of the total, skip the `LOOKAHEAD(24) StatementExpression()` and `LOOKAHEAD(KeyMethodBodyStatement())` experiments. If still above, prototype one at a time, each as its own commit, with the same gates; revert on any parse difference.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessCheck checkstyleMain
git add javaparser-core/src/main/javacc/java.jj javaparser-core-testing/src/test/java/com/github/javaparser/TypeLookaheadTest.java
git commit -m "perf(grammar): drop redundant numeric lookaheads

plain/grammar: ...% ; jml/grammar: ...%"
```

---

## Task 4: P3 — JML gating, only if it pays

**Files:**
- Modify: `javaparser-core/src/main/javacc-support/com/github/javaparser/GeneratedJavaParserBase.java`
- Modify: `javaparser-core/src/main/java/com/github/javaparser/JavaParser.java`
- Modify: `javaparser-core/src/main/javacc/java.jj` (`JmlPrimaryPrefix()`)
- Create: `javaparser-core-testing/src/test/java/com/github/javaparser/JmlGatingTest.java`

**Interfaces:**
- Produces: `void GeneratedJavaParserBase.setProcessJml(boolean processJml)` and `boolean isProcessJml()` (package-private accessors in the parser base).

- [ ] **Step 1: Write the behavior test**

```java
@Test
void plainJavaWithAndWithoutJmlProcessingParsesEqually() {
    String source = "class X { void m() { int x = (1); } }";
    var off = new JavaParser(new ParserConfiguration().setProcessJml(false)).parse(source);
    var on  = new JavaParser(new ParserConfiguration().setProcessJml(true)).parse(source);
    assertTrue(off.isSuccessful());
    assertTrue(on.isSuccessful());
    assertEquals(on.getResult().orElseThrow().toString(), off.getResult().orElseThrow().toString());
}

@Test
void jmlMarkersWithProcessingOffStayParseable() {
    String source = "class X { //@ invariant true;\n void m() {} }";
    assertDoesNotThrow(() -> new JavaParser(new ParserConfiguration().setProcessJml(false)).parse(source));
}
```

Run: `./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.JmlGatingTest"`
Expected: PASS (these pin current behavior).

- [ ] **Step 2: Add configuration plumbing**

In `GeneratedJavaParserBase`, add:

```java
private boolean processJml = true;
void setProcessJml(boolean processJml) { this.processJml = processJml; }
boolean isProcessJml() { return processJml; }
```

In `JavaParser.getParserForProvider`, after `setKeyMetaConstructNames`, add `astParser.setProcessJml(configuration.isProcessJml());`.

- [ ] **Step 3: Guard the JML primary prefix**

In `JmlPrimaryPrefix()` in `java.jj`, add `LOOKAHEAD({ isProcessJml() })` as the first element of the production body (the same pattern as Task 2's `Identifier()` guard).

- [ ] **Step 4: Regenerate, run the full JML suite**

```bash
./gradlew :jmlparser-core:compileJavacc
./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.JmlGatingTest"
./gradlew :jmlparser-jml-tests:test :jmlparser-core-testing:test
```

Expected: all pass. If any existing test changes behavior, revert this task entirely and record the negative result — do not adjust the tests to fit the gate.

- [ ] **Step 5: Measure and decide**

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
./gradlew :tools:bench:run --args="--corpus plain --mode full    --baseline tools/bench/baseline/plain-full.json --verify"
```

Ship only if `plain` improves beyond noise. If not, `git checkout --` the three modified files and skip the commit.

- [ ] **Step 6: Format and commit (only on a measured win)**

```bash
./gradlew spotlessCheck checkstyleMain
git add javaparser-core/src/main/javacc-support/com/github/javaparser/GeneratedJavaParserBase.java \
        javaparser-core/src/main/java/com/github/javaparser/JavaParser.java \
        javaparser-core/src/main/javacc/java.jj \
        javaparser-core-testing/src/test/java/com/github/javaparser/JmlGatingTest.java
git commit -m "perf(grammar): gate JML primary prefix by processJml

plain/grammar: ...%"
```

---

## Task 5: P4 — lexer investigation (measure, do not guess)

**Files:**
- Modify: `doc/design/grammar-parsing-performance.md` (results section)

- [ ] **Step 1: Re-profile after P1–P3**

Run the plain corpus with JFR (`-XX:StartFlightRecording=...,settings=profile`), aggregate samples, and compute the exclusive shares of validation, generated parser, and token manager as in the spec.

- [ ] **Step 2: Decide**

If token-manager share is below 10% of total samples, or no specific safe lexical change is identified, write a short "No lexer change" result and stop this task. Do not make speculative lexer edits.

- [ ] **Step 3: If a concrete safe candidate exists**

Apply one change at a time, run the full test suite and both corpora with `--verify`, and keep it only on a beyond-noise win with no regression. One commit per kept change.

---

## Task 6: Results, changelog, and final verification

**Files:**
- Modify: `doc/design/grammar-parsing-performance.md`
- Modify: `jmltk-changelog.md`

- [ ] **Step 1: Write measured results into the design doc**

Append a "Results" section with the final median MB/s and percent deltas for all four corpus/mode combinations, plus which of P1–P4 shipped and which were rejected with the reason.

- [ ] **Step 2: Add the changelog entry**

Under the current `Unreleased` → `Changed`, add one bullet describing the parsing optimization, with measured deltas and the commit hashes (mark hashes as feature-branch/pre-merge).

- [ ] **Step 3: Run the full verification**

```bash
./gradlew :tools:bench:test
./gradlew :jmlparser-core:compileJavacc
./gradlew :jmlparser-jml-tests:test :jmlparser-core-testing:test :jmlparser-symbol-solver-testing:test
./scripts/run-generators.sh && git diff --exit-code
./gradlew spotlessCheck checkstyleMain
```

Expected: all pass; `git diff --exit-code` is clean after generators; only intended files are modified (`gradle.properties` remains unstaged).

- [ ] **Step 4: Commit**

```bash
git add doc/design/grammar-parsing-performance.md jmltk-changelog.md
git commit -m "docs(changelog): record parsing performance work"
```

---

## Verification Summary

| Gate | Command |
|---|---|
| Bench unit tests | `./gradlew :tools:bench:test` |
| Grammar regeneration | `./gradlew :jmlparser-core:compileJavacc` |
| Parser tests | `./gradlew :jmlparser-core-testing:test :jmlparser-jml-tests:test` |
| Symbol solver | `./gradlew :jmlparser-symbol-solver-testing:test` |
| Generators clean | `./scripts/run-generators.sh && git diff --exit-code` |
| Style | `./gradlew spotlessCheck checkstyleMain` |
| Performance | `:tools:bench` with `--baseline` and `--verify` on all four corpus/mode pairs |
