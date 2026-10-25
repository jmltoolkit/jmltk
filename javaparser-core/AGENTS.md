# javaparser-core — AST Development

Rules for working on the AST in this module. The metamodel, visitors, pretty printer, and token sources are
generated — never hand-edit them; always run `./scripts/run-generators.sh` from the repo root afterwards.
AST changes also touch the JML grammar (`javaparser-core/src/main/javacc/java.jj`), `ConcreteSyntaxModel`,
`DefaultPrettyPrinterVisitor`, and the language-level validators.

## Adding a new AST class
1. Class must be in the `Node` class hierarchy
2. Add attributes, mark optional ones with `@OptionalProperty`
3. Implement the `NodeWith*` interfaces that fit the use case
4. Stub the minimal requirements: visitor methods, constructor, and an `@AllFieldsConstructor`
5. Add the class to `ALL_NODE_CLASS` in the metamodel generator
6. Run `./scripts/run-generators.sh`

## Removing an AST class
1. Remove the class from `ALL_NODE_CLASS`
2. Run `./scripts/run-generators.sh`
3. Fix compiler errors — mostly wrong overloads (visitors lost the `visit` method)
4. Remove the class and follow along its usages until fixed

## Removing an AST attribute
1. Remove the attribute from the AST class
2. Minimal stubs first: getter `return null`, setter `return this`
3. Run `./scripts/run-generators.sh`
4. Remove the getter and setter

## What an AST change touches
Adding or removing nodes/fields regenerates `metamodel/*MetaModel.java`, the visitors (Clone, Equals, Modifier,
HashCode, Observer), `ObservableProperty`, and `JavaToken` kinds. See `CONTRIBUTING.md` → "Guide to Adding New
Nodes and Fields" for the full checklist.
