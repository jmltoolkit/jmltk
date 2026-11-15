# JMLtk — Agent Instructions

JMLtk (`jmltoolkit/jmltk`) is a JavaParser fork adding Java Modeling Language (JML) support. Multi-module
Gradle build: parser/AST/symbol solver in Java (`javaparser-*` dirs, `:jmlparser-*` project paths), CLI/LSP
tools in Kotlin under `tools/`. JDK 21. See `README.md`, `CONTRIBUTING.md`, `javaparser-core/AGENTS.md`.

## Setup

- JDK 21 is selected by the Gradle toolchain (auto-provisioned via foojay); no external services or database.
- Build and test everything: `./gradlew build`. Always use the wrapper, not a system `gradle`.

## Commands

| Task                 | Command                                                                                    |
|----------------------|--------------------------------------------------------------------------------------------|
| Build                | `./gradlew assemble`                                                                       |
| All tests            | `./gradlew test`                                                                           |
| Core/parser tests    | `./gradlew :jmlparser-core-testing:test`                                                   |
| Symbol-solver tests  | `./gradlew :jmlparser-symbol-solver-testing:test`                                          |
| JML corpus tests     | `./gradlew :jmlparser-jml-tests:test`                                                      |
| One test class       | `./gradlew :jmlparser-core-testing:test --tests "com.github.javaparser.ast.body.AnnotationDeclarationTest"` |
| Shell/CLI tests      | `python3 scripts/shelltester.py tools/cli/shell-tests`                                     |
| Auto-format          | `./gradlew spotlessApply`                                                                  |
| Verify format        | `./gradlew spotlessCheck`                                                                  |
| Checkstyle           | `./gradlew checkstyleMain`                                                                 |
| Regenerate code      | `./scripts/run-generators.sh` (codegen + format + `git diff --exit-code`)                  |
| Regenerate parser    | `./gradlew :jmlparser-core:compileJavacc`                                                  |
| Coverage/test report | `./gradlew test aggregateCoverageReport aggregateTestReport`                               |
| Docs                 | `./gradlew :dokkaGeneratePublicationHtml`                                                  |

CI: `Tests` runs the aggregate test/coverage command on Linux/macOS/Windows and the shell tests on Ubuntu;
`Check codegen and formatting` runs Checkstyle, both generators with `git diff --exit-code`, and `spotlessCheck`.

## Project Layout

- `javaparser-core/` — grammar (`src/main/javacc/java.jj`), AST (`.../ast/`; JML in `ast/jml/{body,clauses,doc,expr,stmt,type}`), `ConcreteSyntaxModel`, printers, generated sources.
- `javaparser-symbol-solver-core/` — name/type resolution.
- Tests: `javaparser-core-testing/`, `javaparser-core-testing-bdd/`, `javaparser-symbol-solver-testing/`, `jmlparser-jml-tests/`, `javaparser-key-testing/`.
- Generators: `javaparser-core-metamodel-generator/`, `javaparser-core-generators/`.
- `tools/` — Kotlin CLI tools (`cli`, `lsp`, `lint`, `smt`, `wd`, `stat`, `jml2java`, `jmlstub`, …).
- `doc/design/` — design/implementation plans; `jmltk-changelog.md` — release notes and version history.

## Conventions

- Tests: JUnit 5 (Jupiter); AssertJ, Google Truth, Mockito, Hamcrest, JBehave available. Prefer `var`.
- Shell e2e tests: `tools/cli/shell-tests/`.
- Style: Spotless (palantir-java-format for Java, ktlint for Kotlin), Checkstyle; new files need the license
  header from `gradle/header`.
- Avoid newlines in Markdown, e.g. in PR descriptions.
- Javadoc/KDoc on AST classes: include an example of the underlying syntax construct.

## Generated Code

- Never hand-edit generated output (visitors, metamodel, pretty printer, `JavaToken` kinds). Change the
  sources, run `./scripts/run-generators.sh`; CI fails if the regenerated diff is non-empty.
- Adding/removing AST classes or attributes: `javaparser-core/AGENTS.md` + `CONTRIBUTING.md` → "Guide to Adding New Nodes and Fields".

## Grammar

- Edit `javaparser-core/src/main/javacc/java.jj`; new tokens need a dedicated lexer rule, and existing rules
  must not be reordered.
- Classify every token kind in `com.github.javaparser.TokenTypes#getCategory(int)` or `TokenTypesTest` fails.
- After changes run `./gradlew :jmlparser-core:compileJavacc`; fix `Warning: Choice conflict` with `LOOKAHEAD`.

## Changelog / Documentation

- Features, bug fixes, and other user-visible changes require an entry in `jmltk-changelog.md`, under the
  current `Unreleased` section's `Added` / `Changed` / `Fixed` / `Removed` / `Breaking` headings.
- Cite commit hashes; mark feature-branch and pre-merge hashes as such (see the changelog's reachability note).
- Docs site: <https://jmltoolkit.github.io>; source at `/home/weigl/work/jmltoolkit.github.io/`
  (Hugo, content under `content/`). Update it for significant core/tool changes.

## Versioning / Releases

- Fork-local SemVer in `gradle.properties` (`version=`), independent of upstream; `upstreamBase=` tracks the
  merged JavaParser release. Both are exposed via `JmlTkBuild`.
- patch = bugfix, minor = feature/upstream merge, major = breaking change. Keep `-SNAPSHOT` between releases.
- Composite J/K history: `jmltk-changelog.md`. Release steps: `CONTRIBUTING.md`.
- `README.md` is generated from `doc/readme.md` (`scripts/generate_readme.py`); never edit it directly.

## PRs

- Use a conventional title prefix, e.g. `feat(core): …`, `fix(symbol-solver): …`, `docs(changelog): …`.
- Include tests; link the issue as `#123`.
- Before pushing: `./scripts/run-generators.sh`, `./gradlew spotlessCheck checkstyleMain`, and the relevant
  module tests.

## Gotchas

- JML processing/validation only runs with `ParserConfiguration#setProcessJml(true)`.
- JML AST nodes live in `ast/jml/...`; plain JavaParser nodes in `ast/...`.
- Both `Tests` and `Check codegen and formatting` workflows must be green.
