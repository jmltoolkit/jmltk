# Math-modifier test sources (KeY provenance)

These files are copied verbatim from the KeY project (KeYProject/key) and are
used as **negative** parsing tests: JMLtk must reject them, because they use
KeY-style `spec_*_math` math-mode pragmas that JMLtk deliberately does not
support (it only supports the `code_*_math` modifiers).

| File | KeY source path | KeY commit |
|------|-----------------|------------|
| `TestClass.java` | `key.core/src/test/resources/testcase/speclang/testPackage/TestClass.java` | `3b66e4e` (2026-03-12) |
| `Test.java` | `key.core/src/test/resources/testcase/specMath/bigint/Test.java` | `3b66e4e` (2026-03-12) |

Contents of the math modifiers in these sources:

- `TestClass.java`: class-level inline pragma `public /*@ spec_java_math @*/ class TestClass`.
- `Test.java`: class-level pragma `/*@ spec_bigint_math @*/`, a model-method
  modifier `static spec_java_math model boolean testModel(int[] t)`, and a
  method inline pragma `public /*@ spec_java_math @*/ void testJavaMath()`.

Note: a later KeY handoff commit (`248b69f4`, 2026-10-24) removed these
pragmas, documenting verbatim that "JMLtk does not accept standalone
spec-math-mode comments (it only knows code_*_math modifiers)".

KeY sources are distributed under the terms of the GNU General Public License,
version 2 or later. The files are used here solely as parser regression
fixtures.
