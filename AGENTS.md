# JMLtk — Agent Instructions

JMLtk (jmltoolkit/jmltk) is a fork of JavaParser that adds Java Modeling Language (JML) support. It is a Gradle
multi-module build (root project `jmlparser-parent`). Core libraries in Java, tools in Kotlin.

## Package Manager
Use the Gradle wrapper: `./gradlew`. Full guidance in `CONTRIBUTING.md`.

## File-Scoped Commands
| Task              | Command                                                                   |
|-------------------|---------------------------------------------------------------------------|
| Test one class    | `./gradlew test --tests "com.github.javaparser.ast.stmt.ForStmtTest"`     |
| Test a module     | `./gradlew :javaparser-core:test`                                         |
| Compile           | `./gradlew compileJava`                                                   |
| Auto-format       | `./gradlew spotlessApply`                                                 |
| Verify format     | `./gradlew spotlessCheck`                                                 |
| Checkstyle        | `./gradlew checkstyleMain`                                                |
| Regenerate code   | `./scripts/run-generators.sh` (codegen + format + `git diff --exit-code`) |
| Regenerate parser | `./gradlew :javaparser-core:compileJavacc`                                |

## Versioning
- **Fork-local SemVer.** JMLtk versions are `MAJOR.MINOR.PATCH` (e.g. `1.0.0`) and are independent of the upstream
  JavaParser version. The old composite scheme `<JavaParser>-J<JML>-K<KeY>` (e.g. `3.28.2-J8.0-K13.6`) is retired;
  its history and the J/K → SemVer mapping live in `jmltk-changelog.md`.
- **Single source of truth:** `gradle.properties` (`version=`). Module versions inherit it via
  `buildlogic.java-conventions`; the generated `JmlTkBuild` (`javaparser-core/src/main/java-templates/.../JmlTkBuild.java`)
  exposes it at runtime.
- **Track the embedded upstream JavaParser** via `upstreamBase=` in `gradle.properties`. Bump it in the same change
  that merges an upstream release. It is surfaced as `JmlTkBuild.UPSTREAM_JAVAPARSER_VERSION` and as the
  `jmltk.upstreamJavaParser` Maven POM property.
- **Bump rules:** patch = bugfix, minor = feature/upstream merge, major = breaking change. Add `-SNAPSHOT` between
  releases; drop it in the release commit.
- **Cutting a release:** follow `CONTRIBUTING.md` (tests, generators, `spotlessApply`), set `version=` in
  `gradle.properties`, and add a `jmltk-changelog.md` entry.
- `README.md`'s version string is generated from `doc/readme.md` (`$$mvnversion$$` via `scripts/generate_readme.py`)
  — never edit it by hand.

## Key Conventions
- JML AST nodes live in `javaparser-core/src/main/java/com/github/javaparser/ast/jml/` (subpackages `body`,
  `clauses`, `doc`, `expr`, `stmt`, `type`); plain JavaParser nodes in `.../ast/`.
- Parser grammar: `javaparser-core/src/main/javacc/java.jj`.
- Do NOT hand-edit generated output (visitors, metamodel, pretty printer). Change sources, then run the generators
  (`:jmlparser-core-metamodel-generator:run` and `:jmlparser-core-generators:run`); the CI style check fails
  otherwise.
- Adding AST nodes/fields also touches `MetaModelGenerator.ALL_NODE_CLASSES`, `ConcreteSyntaxModel`,
  `DefaultPrettyPrinterVisitor`, and the language-level validators. Follow `CONTRIBUTING.md`.
- Edit `doc/readme.md` for README changes — `README.md` is generated and gets overwritten.
- Test modules: `javaparser-core-testing`, `javaparser-symbol-solver-testing`, `jmlparser-jml-tests`; CLI tools in
  `tools/`.
- PR workflow, coding guidelines, and the node/field guide: see `CONTRIBUTING.md`.
