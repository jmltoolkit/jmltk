# JMLtk — Agent Instructions

JMLtk (jmltoolkit/jmltk) is a fork of JavaParser adding Java Modeling Language (JML) support: a Gradle
multi-module build (root `jmlparser-parent`), core libraries in Java, tools in Kotlin.

## Package Manager
Use the Gradle wrapper: `./gradlew`.

## Commands
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

## Conventions

### Tests
- Assertions from AssertJ or Google Truth are available
- Use `var` modifier

### Javadoc / KDoc
- On AST classes, also add examples of the underlying syntax construct

### Grammar
- Grammar: `javaparser-core/src/main/javacc/java.jj`
- New tokens must be declared with a dedicated lexer rule; do not reorder existing rules
- Every token kind must be classified in `com.github.javaparser.TokenTypes#getCategory(int)`
  or `TokenTypesTest` breaks

### Generated code
- Never hand-edit generated output (visitors, metamodel, pretty printer, `JavaToken` kinds). Change sources,
  then run the generators (`:jmlparser-core-metamodel-generator:run`, `:jmlparser-core-generators:run`) and
  `spotlessApply`; CI style check fails otherwise.

## AST changes
Adding/removing AST classes and attributes follows a specific workflow and touches related generator and
printer sources — see `javaparser-core/AGENTS.md`.

## Versioning
- Fork-local SemVer (`MAJOR.MINOR.PATCH`), independent of the upstream JavaParser version. The old composite
  scheme `<JavaParser>-J<JML>-K<KeY>` and its mapping live in `jmltk-changelog.md`.
- Source of truth: `version=` in `gradle.properties`, inherited via `buildlogic.java-conventions` and exposed
  as `JmlTkBuild.PROJECT_VERSION`. The embedded upstream version is tracked via `upstreamBase=` (surfaced as
  `JmlTkBuild.UPSTREAM_JAVAPARSER_VERSION`).
- Bump: patch = bugfix, minor = feature/upstream merge, major = breaking change. Keep `-SNAPSHOT` between releases.
- Cutting a release: follow `CONTRIBUTING.md`. `README.md`'s version string is generated from `doc/readme.md`
  (`$$mvnversion$$` via `scripts/generate_readme.py`) — never edit it by hand.

## Gotchas
- JML AST nodes live in `javaparser-core/src/main/java/com/github/javaparser/ast/jml/` (`body`, `clauses`, `doc`,
  `expr`, `stmt`, `type`); plain JavaParser nodes in `.../ast/`.
- Test modules: `javaparser-core-testing`, `javaparser-symbol-solver-testing`, `jmlparser-jml-tests`; CLI tools
  in `tools/`.
- PR workflow, coding guidelines, and the node/field guide: `CONTRIBUTING.md`.
