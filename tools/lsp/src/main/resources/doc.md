%keywords invariant invariant_free invariant_redundantly

## invariant clause

The `invariant` clause specifies a condition that must hold in every visible state for instances of a class. It is evaluated after construction and before and after each method call. The `_free` suffix indicates the invariant is not inherited by subclasses. The `_redundantly` suffix suppresses warnings when the tool detects the invariant is redundant. See [Reference Manual Section 4.2](https://www.jmlspecs.org/JMLreference.pdf) for detailed semantics.

----- 
%keywords accessible accessible_redundantly

## accessible clause

The `accessible` clause restricts which memory locations may be accessed during method execution. It defines a footprint constraint ensuring the method only reads from specified locations. Use `_redundantly` to suppress redundancy warnings. Refer to [Reference Manual Section 5.3](https://www.jmlspecs.org/JMLreference.pdf) for access control specifications.

-----
%keywords abrupt_behavior abrupt_behaviour model_behavior model_behaviour

## Behavior specification modifiers

`abrupt_behavior` (or `abrupt_behaviour`) specifies behavior for abnormal method termination. `model_behavior` defines abstract behavioral specifications without concrete implementation details. These modifiers structure case analysis in method specifications. See [Reference Manual Section 4.1](https://www.jmlspecs.org/JMLreference.pdf) for behavioral specification patterns.

-----
%keywords also

## also clause

The `also` keyword combines multiple behavior cases for the same method specification. It enables disjunctive reasoning where each behavior describes a distinct execution path. Multiple behaviors are conjoined using `also`. [Reference Manual Section 4.1.3](https://www.jmlspecs.org/JMLreference.pdf) describes behavioral composition.

-----
%keywords <=!=> <==> ==> <== <- <: <# <#=

## Logical operators

JML provides logical connectives: `<==>` (biconditional), `==>` (implication), `<==` (reverse implication), `<=!=>` (exclusive or). Additional operators include `<-` (left arrow), `<:` (subtype), `<#` (disjoint), `<#=` (subset disjoint). These operators form specification expressions. See [Reference Manual Section 3.4](https://www.jmlspecs.org/JMLreference.pdf) for expression syntax.

-----
%keywords assert assert_redundantly

## assert statement

The `assert` statement checks a boolean condition at runtime during verification or assertion checking. It has syntax `//@ assert <expression>;`. The `_redundantly` suffix suppresses warnings for provably true assertions. [Reference Manual Section 11.1](https://www.jmlspecs.org/JMLreference.pdf) covers assertion statements.

-----
%keywords assignable assignable_redundantly

## assignable clause

The `assignable` clause (equivalent to `modifies`) declares which memory locations a method may modify. It forms part of the method's frame condition. Use `_redundantly` to suppress redundancy warnings. See [Reference Manual Section 5.2](https://www.jmlspecs.org/JMLreference.pdf) for modification specifications.

-----
%keywords assume assume_redundantly

## assume statement

The `assume` statement introduces an assumption into the verification context without runtime checking. Syntax: `//@ assume <expression>;`. Assumptions are trusted by verifiers. The `_redundantly` variant suppresses warnings. [Reference Manual Section 11.2](https://www.jmlspecs.org/JMLreference.pdf) details assumption semantics.

-----
%keywords axiom

## axiom clause

An `axiom` clause introduces a state-independent boolean assumption into proofs. Syntax: `axiom [name:] predicate;`. Axioms express mathematical properties too complex for automatic proof. They carry soundness risk unless separately verified. [Reference Manual Section 4.6](https://www.jmlspecs.org/JMLreference.pdf) covers axiomatic specifications.

-----
%keywords behavior behaviour

## behavior case

A `behavior` (or `behaviour`) case defines one possible execution scenario for a method specification. Multiple behaviors are combined with `also`. Each behavior contains its own preconditions, postconditions, and frame conditions. See [Reference Manual Section 4.1](https://www.jmlspecs.org/JMLreference.pdf) for behavioral specification structure.

-----
%keywords \BIGINT \bigint \bigint_math

## BIGINT type and arithmetic

`\BIGINT` and `\bigint` denote arbitrary-precision integer types in specifications. `\bigint_math` enables big integer arithmetic semantics in specification expressions. These types avoid overflow in mathematical reasoning. [Reference Manual Section 3.2.1](https://www.jmlspecs.org/JMLreference.pdf) describes numeric types.

-----
%keywords breaks breaks_redundantly break_behavior break_behaviour

## breaks clause

The `breaks` clause specifies postconditions when a loop terminates via a break statement. Syntax: `breaks <expression>;`. Use `break_behavior` to define dedicated break termination cases. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 6.3](https://www.jmlspecs.org/JMLreference.pdf) covers loop termination specifications.

-----
%keywords \by

## by clause

The `\by` clause introduces a proof hint or justification for verification tools. It suggests a reasoning strategy or lemma application. Syntax varies by tool. See [Reference Manual Section 9.2](https://www.jmlspecs.org/JMLreference.pdf) for proof annotations.

-----
%keywords callable callable_redundantly

## callable clause

The `callable` clause specifies which methods may be invoked during execution of a code block. It restricts the call graph for verification purposes. Use `_redundantly` to suppress redundancy warnings. [Reference Manual Section 5.4](https://www.jmlspecs.org/JMLreference.pdf) describes call restrictions.

-----
%keywords captures captures_redundantly

## captures clause

The `captures` clause specifies which heap locations are captured by a lambda or closure. It defines the footprint of functional abstractions. Use `_redundantly` to suppress warnings. See [Reference Manual Section 10.9](https://www.jmlspecs.org/JMLreference.pdf) for lambda specifications.

-----
%keywords choose choose_if

## choose statement

The `choose` statement introduces a specification variable with a chosen value satisfying a predicate. Syntax: `choose <type> <var> such_that <predicate>;`. The `choose_if` variant adds conditional selection. [Reference Manual Section 11.8](https://www.jmlspecs.org/JMLreference.pdf) covers choice statements.

-----
%keywords code code_bigint_math code_java_math code_safe_math

## code annotation

The `code` annotation marks Java code blocks within specifications. Variants `code_bigint_math`, `code_java_math`, and `code_safe_math` specify arithmetic semantics for the enclosed code. These control overflow behavior. [Reference Manual Section 10.3](https://www.jmlspecs.org/JMLreference.pdf) discusses code annotations.

-----
%keywords immutable

## immutable modifier

The `immutable` modifier declares that an object's state cannot change after construction. All fields become effectively final. Immutable objects simplify reasoning about aliasing. See [Reference Manual Section 4.4](https://www.jmlspecs.org/JMLreference.pdf) for immutability specifications.

-----
%keywords constraint constraint_redundantly

## constraint clause

A `constraint` clause adds a global postcondition to all non-constructor methods of a class. Syntax: `constraint <predicate>;`. It ensures certain properties hold across method calls. Use `_redundantly` to suppress warnings. [Reference Manual Section 4.3](https://www.jmlspecs.org/JMLreference.pdf) covers type-level constraints.

-----
%keywords constructor

## constructor specification

The `constructor` keyword introduces specifications specific to constructors. It enables preconditions and postconditions for object initialization. Constructor specs use standard JML clauses. [Reference Manual Section 10.6](https://www.jmlspecs.org/JMLreference.pdf) details constructor specifications.

-----
%keywords continues continues_redundantly continue_behavior continue_behaviour

## continues clause

The `continues` clause specifies behavior when a loop iteration ends via continue. Syntax: `continues <expression>;`. Use `continue_behavior` for dedicated continue cases. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 6.3](https://www.jmlspecs.org/JMLreference.pdf) covers loop control specifications.

-----
%keywords \declassifies

## declassifies clause

The `\declassifies` clause specifies information flow declassification in security policies. It permits controlled release of otherwise protected information. Used in information flow verification. See [Reference Manual Section 8.4](https://www.jmlspecs.org/JMLreference.pdf) for security specifications.

-----
%keywords decreases decreases_redundantly decreasing decreasing_redundantly

## decreases clause

The `decreases` clause provides a termination measure for loops or recursive methods. Syntax: `decreases <expression>;`. The measure must strictly decrease with each iteration. Use `_redundantly` to suppress warnings. [Reference Manual Section 6.4](https://www.jmlspecs.org/JMLreference.pdf) covers termination specifications.

-----
%keywords determines loop_determines

## determines clause

The `determines` clause specifies which variables determine the result of a pure function. `loop_determines` applies to loop iterations. These clauses support deterministic reasoning. See [Reference Manual Section 5.5](https://www.jmlspecs.org/JMLreference.pdf) for determinacy specifications.

-----
%keywords separates loop_separates

## separates clause

The `separates` clause asserts that specified memory regions do not overlap. `loop_separates` applies this to loop iterations. Separation supports modular reasoning. [Reference Manual Section 5.6](https://www.jmlspecs.org/JMLreference.pdf) covers separation logic.

-----
%keywords \new_objects

## new_objects expression

The `\new_objects` expression denotes the set of objects allocated during method execution. It tracks fresh allocations in postconditions. Used in frame specifications. See [Reference Manual Section 3.5.2](https://www.jmlspecs.org/JMLreference.pdf) for allocation expressions.

-----
%keywords diverges diverges_redundantly

## diverges clause

The `diverges` clause specifies conditions under which a method may not terminate normally (e.g., loops forever). Syntax: `diverges <predicate>;`. Use `_redundantly` to suppress warnings. [Reference Manual Section 4.1.5](https://www.jmlspecs.org/JMLreference.pdf) covers divergence specifications.

-----
%keywords \duration duration_redundantly

## duration clause

The `\duration` clause specifies timing or resource consumption bounds for method execution. Syntax: `duration <expression>;`. Used in real-time or resource-aware verification. Suffix `_redundantly` suppresses warnings. See [Reference Manual Section 7.3](https://www.jmlspecs.org/JMLreference.pdf) for temporal specifications.

-----
%keywords ensures ensures_free ensures_redundantly

## ensures clause

The `ensures` clause specifies normal postconditions that hold after method execution. Syntax: `ensures <predicate>;`. The `_free` suffix prevents inheritance. The `_redundantly` suffix suppresses warnings. [Reference Manual Section 4.1.2](https://www.jmlspecs.org/JMLreference.pdf) details postcondition semantics.

-----
%keywords example exceptional_example normal_example for_example

## example clauses

Example clauses (`example`, `normal_example`, `exceptional_example`, `for_example`) provide concrete test cases within specifications. They illustrate expected behavior with specific inputs and outputs. Used for documentation and test generation. See [Reference Manual Section 4.7](https://www.jmlspecs.org/JMLreference.pdf) for example specifications.

-----
%keywords \exists

## exists quantifier

The `\exists` quantifier expresses existential quantification in specification expressions. Syntax: `\exists <type> <var>; <predicate>`. It asserts existence of values satisfying the predicate. [Reference Manual Section 3.4.2](https://www.jmlspecs.org/JMLreference.pdf) covers quantifiers.

-----
%keywords exsures exsures_redundantly

## exsures clause

The `exsures` clause combines `ensures` and `signals` to specify both normal postconditions and exception postconditions. Syntax: `exsures <exceptionType> <predicate>;`. Use `_redundantly` to suppress warnings. [Reference Manual Section 4.1.4](https://www.jmlspecs.org/JMLreference.pdf) covers exception postconditions.

-----
%keywords extract

## extract annotation

The `extract` annotation directs tools to extract specifications into separate files or modules. It supports modular specification organization. Tool-specific usage varies. See [Reference Manual Section 9.4](https://www.jmlspecs.org/JMLreference.pdf) for extraction directives.

-----
%keywords field

## field specification

The `field` keyword introduces specifications for individual fields, including visibility and mutability constraints. Field specs use clauses like `readonly`, `writable`, `maps`. [Reference Manual Section 4.5](https://www.jmlspecs.org/JMLreference.pdf) covers field specifications.

-----
%keywords \forall forall

## forall quantifier

The `\forall` (or `forall`) quantifier expresses universal quantification. Syntax: `\forall <type> <var>; <predicate>`. It asserts the predicate holds for all values of the given type. [Reference Manual Section 3.4.2](https://www.jmlspecs.org/JMLreference.pdf) details quantifier semantics.

-----
%keywords \let

## let expression

The `\let` expression introduces local definitions within specification expressions. Syntax: `\let <var> == <expr> in <body>`. It improves readability and avoids repetition. See [Reference Manual Section 3.4.5](https://www.jmlspecs.org/JMLreference.pdf) for let bindings.

-----
%keywords ghost

## ghost modifier

The `ghost` modifier declares variables or fields used only in specifications, not at runtime. Ghost code is erased during compilation to executable code. Ghost entities support verification reasoning. [Reference Manual Section 3.3.1](https://www.jmlspecs.org/JMLreference.pdf) covers ghost declarations.

-----
%keywords BEGIN END

## BEGIN/END blocks

`BEGIN` and `END` mark specification blocks or delimit scope in certain JML constructs. They group related specifications or define regions. Usage is context-dependent. See [Reference Manual Section 9.1](https://www.jmlspecs.org/JMLreference.pdf) for block annotations.

-----
%keywords helper

## helper modifier

The `helper` modifier marks methods as auxiliary functions used only within specifications or proofs. Helper methods are typically pure and side-effect free. They support modular verification. [Reference Manual Section 10.2](https://www.jmlspecs.org/JMLreference.pdf) discusses helper methods.

-----
%keywords hence_by hence_by_redundantly

## hence_by clause

The `hence_by` clause provides proof justification for verification conditions. It suggests reasoning steps or lemma applications. Syntax: `hence_by <justification>;`. Use `_redundantly` to suppress warnings. See [Reference Manual Section 9.2](https://www.jmlspecs.org/JMLreference.pdf) for proof hints.

-----
%keywords implies_that

## implies_that clause

The `implies_that` clause chains implications in behavioral specifications. It enables structured reasoning: if precondition holds, then postcondition follows. Used in refined specifications. [Reference Manual Section 4.1.6](https://www.jmlspecs.org/JMLreference.pdf) covers implication patterns.

-----
%keywords in in_redundantly

## in clause

The `in` clause maps model fields to concrete memory locations (datagroups). Syntax: `in <field> \in <location>;`. It defines the footprint of abstract fields. Use `_redundantly` to suppress warnings. [Reference Manual Section 5.1.2](https://www.jmlspecs.org/JMLreference.pdf) covers location mappings.

-----
%keywords initializer static_initializer

## initializer specification

The `initializer` keyword specifies behavior for instance initializers. `static_initializer` applies to static initialization blocks. Both use standard JML clauses. [Reference Manual Section 10.5](https://www.jmlspecs.org/JMLreference.pdf) covers initializer specifications.

-----
%keywords initially

## initially clause

The `initially` clause specifies a postcondition that holds after every constructor completes. Syntax: `initially <predicate>;`. It establishes class invariants at creation time. See [Reference Manual Section 4.3.2](https://www.jmlspecs.org/JMLreference.pdf) for initialization constraints.

-----
%keywords instance two_state no_state uninitialized

## Instance and state modifiers

`instance` marks instance-level specifications. `two_state` enables reference to both pre- and post-states in method specs. `no_state` declares state independence. `uninitialized` refers to pre-construction state. [Reference Manual Section 4.8](https://www.jmlspecs.org/JMLreference.pdf) covers state modifiers.

-----
%keywords loop_contract loop_invariant loop_invariant_free loop_invariant_redundantly

## Loop specifications

`loop_invariant` specifies conditions preserved by each loop iteration. `loop_contract` combines invariant, modifies, and decreases clauses. Suffixes `_free` and `_redundantly` control inheritance and warnings. [Reference Manual Section 6.2](https://www.jmlspecs.org/JMLreference.pdf) details loop invariants.

-----
%keywords maintaining maintaining_redundantly

## maintaining clause

The `maintaining` clause specifies conditions maintained across loop iterations (synonym for loop_invariant in some contexts). Syntax: `maintaining <predicate>;`. Use `_redundantly` to suppress warnings. See [Reference Manual Section 6.2.1](https://www.jmlspecs.org/JMLreference.pdf) for maintenance clauses.

-----
%keywords maps maps_redundantly

## maps clause

The `maps` clause associates a model field with a set of concrete memory locations (datagroup). Syntax: `maps <field> \into \set(<locations>);`. It defines abstraction relationships. Use `_redundantly` to suppress warnings. [Reference Manual Section 5.1.3](https://www.jmlspecs.org/JMLreference.pdf) covers mapping specifications.

-----
%keywords \max \min

## max and min binding terms

The `\max` and `\min` binding terms compute maximum and minimum values over quantified ranges. Syntax: `\max <type> <var>; <predicate>` or `\min <type> <var>; <predicate>`. They bind variables and evaluate the predicate to find extremal values. Used in quantitative specifications. See [Reference Manual Section 3.4.4](https://www.jmlspecs.org/JMLreference.pdf) for binding term syntax.

-----
%keywords measured_by \measured_by measured_by_redundantly

## measured_by clause

The `measured_by` clause specifies a termination measure for recursive methods or loops. Syntax: `measured_by <expression>;`. The measure must decrease with each recursive call. Use `_redundantly` to suppress warnings. [Reference Manual Section 6.4.2](https://www.jmlspecs.org/JMLreference.pdf) covers measurement clauses.

-----
%keywords method

## method specification

The `method` keyword introduces specifications for individual methods. Method specs combine requires, ensures, assigns, and other clauses. [Reference Manual Section 4.1](https://www.jmlspecs.org/JMLreference.pdf) covers method specification structure.

-----
%keywords model

## model modifier

The `model` modifier declares abstract fields or classes used only in specifications. Model entities represent conceptual state not present in concrete implementation. They support abstraction. [Reference Manual Section 3.3.2](https://www.jmlspecs.org/JMLreference.pdf) covers model declarations.

-----
%keywords modifiable modifiable_redundantly modifies modifies_redundantly loop_modifies

## modifies clause

The `modifies` (or `modifiable`) clause declares which memory locations a method may change. Syntax: `modifies <location-list>;`. `loop_modifies` applies to loops. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 5.2](https://www.jmlspecs.org/JMLreference.pdf) details frame conditions.

-----
%keywords monitored monitors_for

## monitors_for clause

The `monitors_for` clause specifies lock requirements for accessing fields. Syntax: `monitors_for <field> = <lock-expression>;`. It enforces synchronization discipline. [Reference Manual Section 7.2](https://www.jmlspecs.org/JMLreference.pdf) covers concurrency specifications.

-----
%keywords } {

## Specification delimiters

Braces `{` and `}` delimit specification blocks, particularly in multi-clause specifications or behavioral cases. They group related clauses. Usage follows Java block conventions. See [Reference Manual Section 2.3](https://www.jmlspecs.org/JMLreference.pdf) for syntactic conventions.

-----
%keywords nonnullelements

## nonnullelements clause

The `nonnullelements` clause specifies that all elements of an array or collection are non-null. Syntax: `nonnullelements <array-field>;`. It strengthens type guarantees. [Reference Manual Section 4.5.3](https://www.jmlspecs.org/JMLreference.pdf) covers element nullity.

-----
%keywords non_null

## non_null modifier

The `non_null` modifier asserts that a reference type value is never null. It applies to fields, parameters, and return types. Non-null types prevent null pointer exceptions. See [Reference Manual Section 3.2.3](https://www.jmlspecs.org/JMLreference.pdf) for nullity annotations.

-----
%keywords normal_behavior normal_behaviour feasible_behavior feasible_behaviour

## Behavior classification

`normal_behavior` specifies the case where a method terminates normally without exceptions. `feasible_behavior` asserts that a behavior case is achievable. British spelling `behaviour` variants are accepted. [Reference Manual Section 4.1.1](https://www.jmlspecs.org/JMLreference.pdf) covers behavior categories.

-----
%keywords old

## old expression

The `\old` expression refers to the value of an expression in the pre-state. Syntax: `\old(<expression>)`. It enables comparison between pre- and post-states. Essential for postconditions. [Reference Manual Section 3.5.1](https://www.jmlspecs.org/JMLreference.pdf) covers state expressions.

-----
%keywords or

## or clause

The `or` keyword combines alternative postconditions or behavioral cases disjunctively. It enables case analysis in specifications. Used within behavior blocks. See [Reference Manual Section 4.1.3](https://www.jmlspecs.org/JMLreference.pdf) for disjunctive specifications.

-----
%keywords peer

## peer modifier

The `peer` modifier declares classes or methods as peers, allowing access to private members. It relaxes encapsulation for closely coupled components. Used in module-level specifications. [Reference Manual Section 10.7](https://www.jmlspecs.org/JMLreference.pdf) covers peer relationships.

-----
%keywords rep

## rep modifier

The `rep` (representation) modifier marks fields as part of the object's representation. Rep fields are subject to representation exposure analysis. They support abstraction barriers. See [Reference Manual Section 4.4.2](https://www.jmlspecs.org/JMLreference.pdf) for representation specifications.

-----
%keywords read_only

## read_only modifier

The `read_only` modifier declares that a field or parameter cannot be modified after initialization. It enforces immutability constraints. Read-only entities support safer reasoning. [Reference Manual Section 4.5.1](https://www.jmlspecs.org/JMLreference.pdf) covers read-only specifications.

-----
%keywords refining

## refining clause

The `refining` clause indicates that a specification refines or strengthens an inherited specification. It supports stepwise refinement in subclass specifications. Used in inheritance hierarchies. See [Reference Manual Section 3.2.4](https://www.jmlspecs.org/JMLreference.pdf) for refinement specifications.

-----
%keywords represents represents_redundantly

## represents clause

The `represents` clause defines the concrete representation of a model field. Syntax: `represents <field> == <expression>;` or `represents <field> such_that <predicate>;`. It establishes abstraction functions. Use `_redundantly` to suppress warnings. [Reference Manual Section 5.1.1](https://www.jmlspecs.org/JMLreference.pdf) covers representation clauses.

-----
%keywords \result

## result expression

The `\result` expression refers to the return value of a method in postconditions. Syntax: `\result`. It enables specification of return value properties. Used in ensures clauses. [Reference Manual Section 3.5.3](https://www.jmlspecs.org/JMLreference.pdf) covers result references.

-----
%keywords returns returns_redundantly return_behavior return_behaviour

## returns clause

The `returns` clause specifies the return value of a method (synonym for ensures in some contexts). `return_behavior` defines dedicated return cases. Suffix `_redundantly` suppresses warnings. See [Reference Manual Section 4.1.2](https://www.jmlspecs.org/JMLreference.pdf) for return specifications.

-----
%keywords \safe_math spec_safe_math code_safe_math

## safe_math mode

The `\safe_math`, `spec_safe_math`, and `code_safe_math` modes enable overflow-checked arithmetic in specifications and code. They prevent silent integer overflow. Safety modes apply to expressions. [Reference Manual Section 3.2.2](https://www.jmlspecs.org/JMLreference.pdf) covers arithmetic modes.

-----
%keywords \java_math spec_java_math code_java_math

## java_math mode

The `\java_math`, `spec_java_math`, and `code_java_math` modes specify Java semantics for arithmetic, including overflow wrapping. They match JVM behavior. Used when Java semantics are required. See [Reference Manual Section 3.2.2](https://www.jmlspecs.org/JMLreference.pdf) for Java math mode.

-----
%keywords spec_bigint_math code_bigint_math

## bigint_math mode

The `spec_bigint_math` and `code_bigint_math` modes enable arbitrary-precision integer arithmetic. They prevent overflow by using mathematical integers. Preferred for verification. [Reference Manual Section 3.2.1](https://www.jmlspecs.org/JMLreference.pdf) covers big integer semantics.

-----
%keywords spec_package spec_private spec_protected spec_public

## Visibility modifiers

JML visibility modifiers (`spec_private`, `spec_protected`, `spec_public`, `spec_package`) control specification visibility independently of Java visibility. They support information hiding. `spec_package` denotes package-private visibility. [Reference Manual Section 3.3.3](https://www.jmlspecs.org/JMLreference.pdf) covers specification visibility.

-----
%keywords set

## set expression

The `\set` expression constructs finite sets in specifications. Syntax: `\set({<elements>})` or `\set(<type> x; <predicate>)`. Sets support mathematical reasoning. See [Reference Manual Section 3.5.5](https://www.jmlspecs.org/JMLreference.pdf) for set expressions.

-----
%keywords signals signals_only signals_only_redundantly signals_redundantly

## signals clause

The `signals` clause specifies postconditions when a method throws an exception. Syntax: `signals <ExceptionType> <predicate>;`. `signals_only` restricts which exceptions may be thrown. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 4.1.4](https://www.jmlspecs.org/JMLreference.pdf) covers exception specifications.

-----
%keywords space

## space clause

The `space` clause specifies memory or storage requirements for data structures. It constrains resource usage. Used in space-bounded verification. See [Reference Manual Section 7.4](https://www.jmlspecs.org/JMLreference.pdf) for space specifications.

-----
%keywords strictly_pure

## strictly_pure modifier

The `strictly_pure` modifier declares methods that are pure and have no observable side effects, including no exceptions. It strengthens the `pure` guarantee. Strictly pure methods are total functions. [Reference Manual Section 10.2.2](https://www.jmlspecs.org/JMLreference.pdf) covers strict purity.

-----
%keywords \such_that

## such_that expression

The `\such_that` construct filters quantified variables or constrains model fields. Syntax: `\forall T x; \such_that(P(x)); Q(x)` or `represents f such_that P;`. It adds predicates to quantifiers. See [Reference Manual Section 3.4.3](https://www.jmlspecs.org/JMLreference.pdf) for constrained quantification.

-----
%keywords \sum \product

## sum and product expressions

The `\sum` and `\product` expressions compute summation and product over sequences or sets. Syntax: `\sum(\seq(...))` or `\product(\set(...))`. Used in quantitative specifications. [Reference Manual Section 3.5.4](https://www.jmlspecs.org/JMLreference.pdf) covers aggregation.

-----
%keywords \TYPE \type

## TYPE expression

The `\TYPE` (or `\type`) expression denotes the runtime type of an object in specifications. Syntax: `\TYPE(<expression>)`. It supports dynamic type reasoning. Used in type-safe specifications. See [Reference Manual Section 3.5.6](https://www.jmlspecs.org/JMLreference.pdf) for type expressions.

-----
%keywords unreachable

## unreachable statement

The `unreachable` statement marks code paths that should never execute. Syntax: `//@ unreachable;`. Violations indicate specification or implementation errors. Used for dead code marking. [Reference Manual Section 11.6](https://www.jmlspecs.org/JMLreference.pdf) covers unreachable statements.

-----
%keywords \warn \warn_op

## warn annotations

The `\warn` and `\warn_op` annotations generate warnings when certain conditions hold. They flag potential issues without failing verification. `warn_op` applies to operators. Used for lint-like checks. See [Reference Manual Section 9.5](https://www.jmlspecs.org/JMLreference.pdf) for warning annotations.

-----
%keywords \working_space working_space working_space_redundantly

## working_space clause

The `\working_space` or `working_space` clause specifies temporary memory locations used during method execution. It distinguishes working memory from persistent state. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 5.2.3](https://www.jmlspecs.org/JMLreference.pdf) covers working space specifications.

-----
%keywords writable readable

## writable and readable clauses

The `writable` and `readable` clauses specify conditions under which fields may be written or read. Syntax: `writable <field> if <condition>;`. They enforce access control policies. See [Reference Manual Section 4.5.2](https://www.jmlspecs.org/JMLreference.pdf) for access clauses.

-----
%keywords nullable nullable_by_default

## nullable modifiers

The `nullable` modifier allows null values for reference types. `nullable_by_default` sets null as the default for a class. These complement `non_null` annotations. [Reference Manual Section 3.2.3](https://www.jmlspecs.org/JMLreference.pdf) covers nullity specifications.

-----
%keywords \nowarn \nowarn_op

## nowarn annotations

The `\nowarn` and `\nowarn_op` annotations suppress specific warnings from verification tools. They acknowledge known issues without fixing them. `nowarn_op` applies to operators. Use sparingly. See [Reference Manual Section 9.5.2](https://www.jmlspecs.org/JMLreference.pdf) for warning suppression.

-----
%keywords \only_assigned \only_accessed \only_called \only_captured

## only_ clauses

Clauses like `\only_assigned`, `\only_accessed`, `\only_called`, and `\only_captured` restrict modifications, accesses, calls, or captures to specified locations or methods. They enforce strict framing. [Reference Manual Section 5.7](https://www.jmlspecs.org/JMLreference.pdf) covers restriction clauses.

-----
%keywords \fresh

## fresh expression

The `\fresh` expression tests whether an object was newly allocated. Syntax: `\fresh(<expression>)`. It identifies fresh objects in postconditions. Used in allocation specifications. See [Reference Manual Section 3.5.2](https://www.jmlspecs.org/JMLreference.pdf) for freshness predicates.

-----
%keywords \reach

## reach expression

The `\reach` expression computes the set of objects reachable from a given reference. Syntax: `\reach(<expression>)`. It supports reachability analysis. Used in ownership specifications. [Reference Manual Section 3.5.7](https://www.jmlspecs.org/JMLreference.pdf) covers reachability.

-----
%keywords \same

## same expression

The `\same` expression tests whether two references point to the same object. Syntax: `\same(<expr1>, <expr2>)`. It asserts reference equality. Distinguished from structural equality. See [Reference Manual Section 3.4.4](https://www.jmlspecs.org/JMLreference.pdf) for equality expressions.

-----
%keywords \not_assigned \not_modified \not_specified \strictly_nothing \nothing

## nothing clauses

Clauses like `\nothing`, `\strictly_nothing`, `\not_assigned`, `\not_modified`, and `\not_specified` indicate absence of modifications, assignments, or specifications. They declare empty frame conditions. [Reference Manual Section 5.2.4](https://www.jmlspecs.org/JMLreference.pdf) covers empty specifications.

-----
%keywords \is_initialized

## is_initialized expression

The `\is_initialized` expression tests whether an object has completed construction. Syntax: `\is_initialized(<expression>)`. It distinguishes initialized from uninitialized objects. Used in construction specifications. See [Reference Manual Section 10.6.4](https://www.jmlspecs.org/JMLreference.pdf) for initialization predicates.

-----
%keywords \into

## into clause

The `\into` clause maps model fields to datagroups or specifies target locations. Syntax: `\into <location-set>`. It defines abstraction mappings. Used with `maps` clauses. [Reference Manual Section 5.1.3](https://www.jmlspecs.org/JMLreference.pdf) covers mapping targets.

-----
%keywords \lockset

## lockset expression

The `\lockset` expression denotes the set of locks held at a program point. Syntax: `\lockset`. It supports concurrency verification. Used in thread-safety specifications. See [Reference Manual Section 7.2.2](https://www.jmlspecs.org/JMLreference.pdf) for lockset expressions.

-----
%keywords \invariant_for

## invariant_for expression

The `\invariant_for` expression applies class invariants to specific objects. Syntax: `\invariant_for(<expression>)`. It enables selective invariant enforcement. Used in modular reasoning. [Reference Manual Section 4.2.3](https://www.jmlspecs.org/JMLreference.pdf) covers targeted invariants.

-----
%keywords \num_of

## num_of expression

The `\num_of` expression counts elements satisfying a predicate in a collection. Syntax: `\num_of(<type> x; <collection>; <predicate>)`. It supports cardinality reasoning. Used in collection specifications. See [Reference Manual Section 3.5.8](https://www.jmlspecs.org/JMLreference.pdf) for counting expressions.

-----
%keywords \pre

## pre expression

The `\pre` expression refers to precondition values in postcondition context. Syntax: `\pre(<expression>)`. It enables precondition-postcondition comparisons. Alternative to requires in some contexts. [Reference Manual Section 3.5.1](https://www.jmlspecs.org/JMLreference.pdf) covers pre-state references.

-----
%keywords post post_redundantly

## post clause

The `post` keyword introduces postcondition specifications (synonym for ensures in some contexts). Syntax: `post <predicate>;`. Suffix `_redundantly` suppresses warnings. See [Reference Manual Section 4.1.2](https://www.jmlspecs.org/JMLreference.pdf) for postcondition syntax.

-----
%keywords pre pre_redundantly

## pre clause

The `pre` keyword introduces precondition specifications (synonym for requires). Syntax: `pre <predicate>;`. Suffix `_redundantly` suppresses warnings. [Reference Manual Section 4.1.1](https://www.jmlspecs.org/JMLreference.pdf) covers precondition syntax.

-----
%keywords \erases

## erases annotation

The `\erases` annotation marks code or specifications to be erased during compilation. Erased content affects verification but not runtime. Used for ghost code. See [Reference Manual Section 3.3.4](https://www.jmlspecs.org/JMLreference.pdf) for erasure annotations.

-----
%keywords non_null_by_default nullable_by_default

## Null defaults

`non_null_by_default` and `nullable_by_default` specify the default nullity for reference types in a class. They apply recursively to nested classes without their own defaults. Not inherited by subclasses. [Reference Manual Section 3.2.3](https://www.jmlspecs.org/JMLreference.pdf) covers nullity defaults.

-----
%keywords pure @Pure

## Purity of functions

Specifying that a class is `pure` means that each method and nested class within the class is specified as pure. The `pure` modifier on a class is not inherited by derived classes, though `pure` modifiers on methods are. There is no modifier to disable an enclosing `pure` specification. [Reference Manual Section 10.2](https://www.jmlspecs.org/JMLreference.pdf) covers purity.

-----
%keywords requires requires_free requires_redundantly

## requires clause

The `requires` clause specifies preconditions that must hold before method execution. Syntax: `requires <predicate>;`. The `_free` suffix prevents inheritance. The `_redundantly` suffix suppresses warnings. [Reference Manual Section 4.1.1](https://www.jmlspecs.org/JMLreference.pdf) details precondition semantics.

-----
%keywords continue_behavior continue_behaviour exceptional_behavior return_behavior normal_behavior model_behavior behavior

## Behavior types

Behavior keywords classify method execution outcomes: `normal_behavior` for normal termination, `exceptional_behavior` for exception cases, `return_behavior` for return specifications, `continue_behavior` for loop continues, `model_behavior` for abstract specs. British spellings with 'behaviour' are accepted. [Reference Manual Section 4.1](https://www.jmlspecs.org/JMLreference.pdf) covers behavior classification.

-----
%keywords \lbl \lblneg \lblpos

## Label expressions

The `\lbl`, `\lblneg`, and `\lblpos` expressions attach labels to subexpressions for error reporting and debugging. `\lblneg` marks negative occurrences, `\lblpos` marks positive occurrences. They improve counterexample readability. See [Reference Manual Section 3.5.7](https://www.jmlspecs.org/JMLreference.pdf) for labeling expressions.

-----
%keywords model_program

## model_program annotation

The `model_program` annotation declares a Java program fragment as a model implementation used for verification. It provides executable semantics for abstract specifications. Model programs bridge abstraction and implementation. [Reference Manual Section 10.8](https://www.jmlspecs.org/JMLreference.pdf) covers model program annotations.

-----
%keywords when_redundantly

## when_redundantly suffix

The `_when_redundantly` suffix (or `when_redundantly`) suppresses warnings when a condition is detected as redundant by the verification tool. It applies to various conditional clauses. Used to silence false positives. See [Reference Manual Section 9.6](https://www.jmlspecs.org/JMLreference.pdf) for redundancy control.

-----
%keywords typeof \typeof

## typeof expression

The `typeof` or `\typeof` expression returns the runtime type of an object in specification context. Syntax: `typeof(<expression>)` or `\typeof(<expression>)`. It supports dynamic type reasoning. Similar to `\TYPE`. [Reference Manual Section 3.5.6](https://www.jmlspecs.org/JMLreference.pdf) covers type expressions.

-----
%keywords \real

## real type

The `\real` keyword denotes the mathematical real number type in specifications. It enables reasoning about continuous quantities. Real numbers have infinite precision. Used in quantitative specifications. See [Reference Manual Section 3.1.3](https://www.jmlspecs.org/JMLreference.pdf) for real number semantics.
