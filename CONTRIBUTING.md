# Contributing to JMLtk

## Getting started

Here is some information that's good to know when contributing to JMLtk:

- JMLtk is a fork of JavaParser that adds first-class support for the Java Modeling Language (JML). The parser,
  the AST and the symbol solver from JavaParser are extended with JML constructs, and the repository also contains a
  `tools` directory with command-line utilities (for example `cli`, `stat`, `lint`, `smt`, `prettyprinting`, and
  `lsp`). If you are new to the code base, these are the places you will spend most of your time:
    - The AST nodes live in `javaparser-core/src/main/java/com/github/javaparser/ast/`, with the JML-specific nodes in
      `com.github.javaparser.ast.jml` and its subpackages (`body`, `clauses`, `doc`, `expr`, `stmt`, `type`).
    - The grammar used to generate the parser lives in `javaparser-core/src/main/javacc/java.jj`.
    - The symbol solver lives in `javaparser-symbol-solver-core` and the code generators in
      `javaparser-core-metamodel-generator` and `javaparser-core-generators`.
- Be sure to check the [Coding Guidelines](#coding-guidelines) below. The corresponding formatting rules are in
  `dev-files/` and can be imported into your IDE as described there.
- If you start working on an issue, please say so with a comment in the issue.
- If you know how to fix a problem, please fix it and open a pull request instead of opening an issue.
- If you would like to add new nodes, or new fields to existing nodes, check out the
  [Guide to Adding New Nodes and Fields](#guide-to-adding-new-nodes-and-fields) below.

Thanks for helping!

## Contribution Workflow

Our development workflow is based on pull requests. A pull request is a proposal to merge a branch of commits from
your fork into the main repository. If you are not familiar with pull requests, this is how the workflow works:

1. **Fork** the JMLtk project (the repository is `jmltoolkit/jmltk` on GitHub). If you already have a fork, ensure to
   fetch the latest commits from the main repository first.
2. In your forked project, **create a branch** related to the issue you are working on. This is important to ensure
   that your pull request will not contain unrelated work. A good naming convention is to include the issue number,
   for example `fix/123-description-of-the-fix`.
3. When your work in your branch is done, **push your branch to your forked repository**.
4. Go back to the JMLtk project site and it should have a message offering to **create a pull request** from your
   branch. If it doesn't, you can create one manually using the "Pull requests" -> "New pull request" page, which
   lets you pick the fork and the branch you want to compare against the `main` branch of the main repository.

### Remember:

- A pull request should include tests. You can either use BDD or JUnit.
- Every pull request will automatically be checked by GitHub Actions. Make sure the `Tests` and
  `Check codegen and formatting` workflows are green (both are also available for manual triggering, so you can run
  them on your fork via the "Actions" tab).
- Pull requests often stay open for at least a few days to give people a chance to review it.
- A pull request is merged when all comments on it have been resolved.
- If you create a pull request for an issue, mention the issue in the format #123 to make GitHub link it
  automatically.
- Before creating a commit (or at least before submitting a pull request), please reformat the project with the
  instructions given below to avoid any formatting-related issues during the review.

### Note on formatting the project:

- If you are developing on a machine with bash installed, execute `./scripts/run-generators.sh`. This will re-run
  all of the code generators, reformat the whole project and then check that the resulting diff is empty. This
  ensures that:
    - All the code that needs to be generated has been generated correctly.
    - None of the changes you've added will be overwritten by code generation in the future.
    - All of your changes are correctly formatted (including changes made during code generation, for example
      whitespace changes).

  The script is equivalent to running the following commands by hand:

  ```
  ./gradlew :jmlparser-core-metamodel-generator:run
  ./gradlew :jmlparser-core-generators:run
  ./gradlew spotlessApply
  git diff --exit-code
  ```

  The PR check for style runs these generators and checks that the diff after doing so is empty, so if you've run
  this on your machine, then that check should not fail.

## Coding Guidelines

This project is formatted with Spotless using the palantir-java-format style, and it is enforced in CI by
`./gradlew spotlessCheck`. In addition to the automated checks, please follow the style rules below.

### Coding style

- indent with four spaces
- wrap lines at 120 characters
- type and method annotations go on a separate line
- wrapped arguments are aligned to the opening parenthesis

### License headers for new files

Newly added files must contain the correct copyright and license header comment (this is enforced by Spotless, which
uses the header file `gradle/header`):

```
/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
```

## Guide to Adding New Nodes and Fields

This guide walks through all the places that need to change when adding a new AST node. It is long, but most steps
are small. The same process is used for JML nodes (in the `com.github.javaparser.ast.jml` packages) and for pure
JavaParser nodes.

1. Add the node outline

Add a skeleton for the new node in `javaparser-core/src/main/java/com/github/javaparser/ast/`, with stubs for the
inherited interface/abstract class methods that have to be implemented, along with a constructor annotated with
`@AllFieldsConstructor`. As the annotation suggests, this constructor should have parameters for all the fields of
the class.

There are some field annotations that affect code generation. In particular, `@DerivedProperty` and
`@OptionalProperty` could be useful.

2. Add the new class to the MetaModelGenerator

In
`javaparser-core-metamodel-generator/src/main/java/com/github/javaparser/generator/metamodel/MetaModelGenerator.java`,
there is an `ALL_NODE_CLASSES` list to which the new class must be added. Note that the new class must be added after
any parent classes (for example, `RecordPatternExpr` must be added after `PatternExpr` which, in turn, must be added
after `Expression`).

3. Run the core metamodel generator

```bash 
./scripts/run-generators.sh
```

This will most likely fail due to compilation errors caused by missing visit methods in the
`DefaultPrettyPrinterVisitor`.

4. Fix PrettyPrinting compilation errors

For this step, `visit` methods for the new node type need to be added to the `DefaultPrettyPrinterVisitor` in
`javaparser-core/src/main/java/com/github/javaparser/printer/`. These can either be the final implementations, or just
stubs to fix compilation errors for now.

5. Repeat with (3)

6. Add a placeholder for the new node in `ConcreteSyntaxModel.java`

This means adding a line like `concreteSyntaxModelByClass.put(NewNodeClassName.class, sequence())` in
`javaparser-core/src/main/java/com/github/javaparser/printer/ConcreteSyntaxModel.java`.

This step is technically optional, but without this a lot of unit tests for unrelated components will fail due to
concrete syntax model verification failing. Doing this step makes it easier to verify that smaller changes are
working as expected without having to implement the concrete syntax model for the new class first.

7. Add AST tests for the new node

Add some tests to `javaparser-core-testing/src/test/java/com/github/javaparser/ast/...` that describe what the AST for
the new node should look like. This will help verify that the grammar changes in the next step work, although at this
point these tests will still fail. There are many existing tests to look at for examples of how this is done.

8. Add parser support by adding rules to `java.jj`

The grammar in `javaparser-core/src/main/javacc/java.jj` is used to generate the `GeneratedJavaParser` class used for
all the parsing. It is necessary to do the above steps first since instantiating the new class would lead to
compilation errors if it does not exist yet. This guide won't go into detail about how the grammar productions work,
but looking at how rules for other classes are written is useful, along with reading the JavaCC (or parser-generator-cc)
documentation. In particular, understanding how `LOOKAHEAD` works can help avoid some sneaky bugs later. A `LOOKAHEAD`
is required whenever a choice point in the grammar is ambiguous, for example when two rules can both match the same
input.

Once all the rules are added, regenerate the parser with:

```
./gradlew :javaparser-core:compileJavacc
```

and verify that the command succeeded. In particular, look for `Warning: Choice conflict in ...` messages. This
suggests that a `LOOKAHEAD` is required to disambiguate the grammar. Note that the absence of such a message does not
mean that the lookaheads that are in place are correct. It is still possible for the parser to make an incorrect
decision at these decision points if a lookahead is wrong, but this won't display a warning.

Run the tests from step 9 (you can select them with `./gradlew test --tests "..."`) and, if they succeed, proceed to
the next step.

9. Add symbol solver tests and support for the new node

The details of this step depend a lot on the specific node type being added, so not much can be said here that's
generally useful. The best advice is to look at tests for similar nodes in `javaparser-symbol-solver-testing` and to
use those as a guide for what to test for the new node. Once tests are in place, finding out where changes are needed
in the symbol solver can take some work, but looking at what's done for other nodes could again be useful.

#### Step 12: Add PrettyPrinter support

Add some pretty printer tests to
`javaparser-core-testing/src/test/java/com/github/javaparser/printer/PrettyPrinterTest.java` and, if it wasn't done in
step 5, add the final implementation for the `DefaultPrettyPrinterVisitor`.

#### Step 13: Add concrete syntax model support and `LexicalPreservingPrinter` tests

Start by adding some `LexicalPreservingPrinter` tests to
`javaparser-core-testing/src/test/java/com/github/javaparser/printer/lexicalpreservation/transformations/ast/body/StatementTransformationsTest.java`
(or the most similar test class for the node type you are adding). There are many examples in those files of how to
implement this.

Then, go back to the stub added in step 8 and expand on it. The general structure should be similar to the grammar
added in `java.jj`, but for details see how this was implemented for similar nodes.

#### Step 14: Add validators with tests

Validator tests are in
`javaparser-core-testing/src/test/java/com/github/javaparser/ast/validator/JavaXValidatorTest.java`.

For adding the validators themselves, add a negative validator to
`javaparser-core/src/main/java/com/github/javaparser/ast/validator/language_level_validations/Java1_0Validator.java`
(that is, a validator that checks that the new node type is NOT being used) and then remove that condition in the
validator for the relevant Java version.

#### Step 15: Final checks before PR

As a last verification step, run `./gradlew test` again, along with `./gradlew checkstyleMain`. If both of these run
successfully, then the change is ready for review!
