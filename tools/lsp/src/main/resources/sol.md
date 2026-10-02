

-----

%keywords behavior behaviour normal_behavior normal_behaviour exceptional_behavior exceptional_behaviour abrupt_behavior abrupt_behaviour continue_behavior continue_behaviour break_behavior break_behaviour return_behavior return_behaviour model_behavior model_behaviour feasible_behavior feasible_behaviour

## Behavior specification cases
A behavior introduces one case of a method or block contract. `normal_behavior` forbids exceptional termination, while `exceptional_behavior` forbids normal termination; `behaviour` spellings are equivalent. The `abrupt_*`, `continue_*`, `break_*`, `return_*`, `model_*`, and `feasible_*` forms are tool-specific or legacy extensions rather than current standard JML. See Reference Manual §§8.1.1 and 11.11.

-----



-----
%keywords post ensures ensures_free ensures_redundantly

## Postconditions
An `ensures` clause gives a predicate that must hold when the method or specified block terminates normally.
```java
//@ ensures result >= 0;
```

post is a legacy synonym, _redundantly marks a logically redundant specification, and ensures_free is a KeY extension. See Reference Manual §8.5.2.

-----

%keywords pre requires requires_free requires_redundantly
## Preconditions
A requires clause states a condition that callers must establish before invoking a method or entering a specified block.
java //@ requires index >= 0;
pre is a legacy synonym, _redundantly marks a redundant clause, and requires_free is a KeY extension. See Reference Manual §8.5.1.
-----
 invariant invariant_free invariant_redundantly
## Type invariants
An invariant clause states a property expected to hold for the relevant class or object states.
java //@ public invariant size >= 0;
invariant_redundantly states a redundant invariant, while invariant_free is a tool-specific extension. See Reference Manual §§3.13 and 7.2.
-----
 accessible accessible_redundantly
## Read frame
An accessible clause states which memory locations a method may read; reads is the preferred current spelling. The _redundantly form indicates a redundant read-frame specification. See Reference Manual §8.6.3.


-----

%keywords also
## Specification-case composition
also separates specification cases and combines a method's own cases with inherited cases. It is required at the beginning of a specification when an overriding method adds cases to inherited specifications. See Reference Manual §§8.1 and 8.1.4.


-----

%keywords <==> <=!=> ==> <==
## Boolean implication and equivalence
==> is short-circuiting logical implication, <==> is equivalence, and <=!=> is inequivalence. The reverse-implication operator <== is deprecated; reverse its operands and use ==> instead. See Reference Manual §§12.5.6–12.5.7 and Appendix C.8.


-----

%keywords assert assert_redundantly
## Assertions
A JML assert requires its Boolean expression to be true at that program point.
java //@ assert index < values.length;
Static verification proves it and runtime assertion checking may test it; _redundantly marks a redundant assertion. See Reference Manual §11.1.


-----

%keywords assume assume_redundantly
## Assumptions
An assume statement lets subsequent reasoning presume that its Boolean expression is true. Because an assumption need not be proved, an incorrect assumption can make verification unsound. The _redundantly form is a redundant assumption. See Reference Manual §11.2.


-----

%keywords assignable assignable_redundantly modifiable modifiable_redundantly modifies modifies_redundantly
## Write frame
An assignable clause states the memory locations that a method or block is permitted to modify. modifiable and modifies are legacy synonyms, and _redundantly forms mark redundant frame specifications. The current synonyms are assignable, assigns, and writes. See Reference Manual §8.5.3.


-----

%keywords loop_modifies
## Loop write frame
loop_modifies states which locations may be modified during all executions of a loop. It is a legacy synonym for the preferred loop_writes or loop_assigns clause. See Reference Manual §11.10.3.


-----

%keywords axiom
## Axioms
An axiom introduces a state-independent Boolean formula as an assumption for verification. Axioms have public visibility and can make verification unsound if they are inconsistent. See Reference Manual §7.12.


-----

%keywords \BIGINT
## Legacy mathematical-integer token
\BIGINT is a legacy or tool-specific spelling for mathematical-integer arithmetic. Current JML uses the type \bigint and the arithmetic mode \bigint_math. See Reference Manual §§5.4 and 13.1.


-----

%keywords \bigint_math \java_math \safe_math
## Local integer-arithmetic modes
These expressions select bigint, Java, or safe arithmetic for the enclosed specification expression. Bigint mode uses mathematical integers, Java mode uses Java overflow semantics, and safe mode reports potential overflow or underflow. See Reference Manual §§12.5.18 and 13.1.


-----

%keywords breaks breaks_redundantly
## Break postconditions
A breaks clause specifies a condition that must hold if a block contract terminates by executing break, optionally naming its target label. It is valid only in block specifications. The _redundantly form is tool-specific or legacy. See Reference Manual §8.6.15.


-----

%keywords continues continues_redundantly
## Continue postconditions
A continues clause specifies a condition that must hold if a block contract terminates by executing continue, optionally naming its target label. It is valid only in block specifications. The _redundantly form is tool-specific or legacy. See Reference Manual §8.6.14.


-----

%keywords returns returns_redundantly
## Return postconditions
A returns clause specifies a condition that must hold if the statement governed by a block contract executes return. It is valid only in block specifications and may use \result where appropriate. The _redundantly form is tool-specific or legacy. See Reference Manual §8.6.12.


-----

%keywords \by
## Proof justification
\by is a KeY or other tool-specific proof-justification construct and is not part of current standard JML. Its exact semantics depend on the verification tool. Consult the tool's documentation in addition to the Reference Manual's discussion of core JML in §3.21.


-----

%keywords callable callable_redundantly
## Callable methods
A callable clause restricts the methods that may be called directly by the specified method or block. callable \nothing; permits no calls. The _redundantly form marks a redundant restriction. See Reference Manual §8.6.10.


-----

%keywords captures captures_redundantly
## Reference capture
A captures clause identifies references that a method is allowed to retain after returning, for example by storing them in a field. It supports alias-control and representation-hiding specifications. The _redundantly form marks a redundant clause. See Reference Manual §8.6.11.


-----

%keywords choose choose_if or
## Nondeterministic choice
A choose statement executes one enabled guarded alternative, with alternatives separated by or. The selected alternative is nondeterministic; choose_if is an older form replaced by the current unified choose syntax. See Reference Manual §11.8.


-----

%keywords code
## Non-inherited specification case
The code modifier marks a behavior that applies only to the implementation declared in that class. Unlike ordinary visible method behaviors, it is not inherited by overriding methods. See Reference Manual §8.1.4.


-----

%keywords code_bigint_math code_java_math code_safe_math
## Arithmetic mode for Java code
These modifiers select bigint, Java, or safe integer arithmetic when analyzing the associated Java implementation. Safe arithmetic is the recommended default because it reports possible overflows. See Reference Manual §§8.7.7 and 13.1.


-----

%keywords spec_bigint_math spec_java_math spec_safe_math
## Arithmetic mode for specifications
These modifiers select bigint, Java, or safe integer arithmetic for JML specification expressions in the associated class or method. Bigint arithmetic is the standard default for specifications. See Reference Manual §§8.7.7 and 13.1.


-----

%keywords immutable
## Immutable types
immutable is a tool-specific extension indicating that instances of a type do not observably change after construction. It is not currently defined as a standard JML modifier. Consult the relevant tool documentation and Reference Manual §3.21.


-----

%keywords constraint constraint_redundantly
## History constraints
A constraint clause specifies a two-state property that methods of a class must preserve between their pre- and post-states.
java //@ public constraint count >= \old(count);
The _redundantly form marks a redundant constraint. See Reference Manual §7.3.


-----

%keywords constructor method field
## Obsolete declaration keywords
The constructor, method, and field keywords were formerly used to assist parsing JML declarations. They have been removed because ordinary Java declaration syntax is sufficient. See Reference Manual Appendix C.17.


-----

%keywords \declassifies determines loop_determines separates loop_separates
## Information-flow specifications
These tokens are KeY information-flow extensions rather than current standard JML constructs. They describe permitted information dependencies, declassification, or separation properties for methods and loops. Consult the KeY documentation; the Reference Manual's feature classification is in Appendix B.


-----

%keywords \new_objects
## Newly allocated objects
\new_objects is a tool-specific information-flow or dynamic-frame expression denoting objects allocated since an earlier state. It is not currently defined by standard JML. For the standard object-allocation predicate, see \fresh in Reference Manual §12.5.15.


-----

%keywords decreases decreases_redundantly decreasing decreasing_redundantly
## Loop variants
A decreases clause gives a non-negative integer expression that must strictly decrease on every loop iteration, thereby proving termination. decreasing is a synonym, and _redundantly forms mark redundant variants. See Reference Manual §§11.10 and 11.10.2.


-----

%keywords measured_by \measured_by measured_by_redundantly
## Recursive termination measure
A measured_by clause supplies a non-negative integer measure that must decrease across recursive calls. \measured_by is a legacy or tool-specific spelling, while _redundantly marks a redundant measure. See Reference Manual §8.6.5 and the limitations discussed in §19.2.


-----

%keywords diverges diverges_redundantly
## Divergence
A diverges clause states when a method is permitted not to return to its caller. The default is diverges false;, requiring termination whenever the behavior applies. The _redundantly form marks a redundant divergence condition. See Reference Manual §8.6.4.


-----

%keywords \duration duration_redundantly
## Execution duration
\duration(expression) denotes the estimated maximum virtual-machine cycles needed to evaluate an expression. A duration method clause places a corresponding resource bound on a method; duration_redundantly marks a redundant bound. Resource specifications are experimental. See Reference Manual §§8.6.8 and 12.6.1.


-----

%keywords working_space working_space_redundantly \working_space space
## Memory-resource specifications
A working_space clause bounds the additional heap space used by a method, while \working_space(expression) estimates the space needed to evaluate an expression. \space(object) denotes the shallow heap space occupied by an object, and _redundantly marks a redundant bound. These features are experimental. See Reference Manual §§8.6.9 and 12.6.2–12.6.3.


-----

%keywords example normal_example exceptional_example
## Example specification cases
Example cases illustrate consequences of a primary contract rather than adding new required behavior. normal_example describes normal termination and exceptional_example describes exceptional termination. They normally occur after for_example. See Reference Manual §8.3.


-----

%keywords \erases
## Erasure
\erases is a legacy or tool-specific token. Current JML uses \erasure(typeExpression) to obtain the Java Class value corresponding to the erasure of a JML \TYPE. See Reference Manual §§5.7.2 and 12.5.25.


-----

%keywords \exists \forall \num_of \sum \product \max \min
## Quantified expressions
\forall and \exists express universal and existential quantification. \num_of, \sum, \product, \max, and \min aggregate values over the range selected by a quantified expression. See Reference Manual §12.5.1.


-----

%keywords exsures exsures_redundantly
## Legacy exceptional postconditions
exsures is a legacy synonym for the signals exceptional postcondition clause. Use signals in new specifications. The _redundantly form denotes a redundant exceptional postcondition.


-----

%keywords extract
## Extracted model programs
The experimental extract modifier asks a tool to derive a model program or specification from a method implementation. Its support and precise behavior are tool-dependent. See Reference Manual §8.8.3 and Chapter 16.


-----

%keywords \let
## Local expression binding
\let introduces a local immutable binding within a JML expression, allowing a subexpression to be named and reused. Its initializer must be well-defined, and a non-null declared type requires a provably non-null initializer. See Reference Manual §3.4.3.15 and the expression grammar in Chapter 12.


-----

%keywords forall
## Deprecated forall specification clause
The forall method-specification clause is deprecated and should not be confused with the \forall expression. Its former uses can generally be expressed with an old declaration initialized by \choose. See Reference Manual Appendix C.16.


-----

%keywords for_example
## Example section
for_example introduces behavior cases intended as examples of consequences of the main method specification. Such examples must follow from the primary behaviors. See Reference Manual §8.3.


-----

%keywords \fresh
## Freshly allocated objects
\fresh(expression) is true when the referenced object was not allocated in the designated earlier program state. Without an explicit label, it compares against the contract's old state. See Reference Manual §12.5.15.


-----

%keywords peer rep read_only
## Universe ownership modifiers
peer, rep, and read_only are experimental Universe-type modifiers used to express ownership and aliasing relationships. Their integration with the rest of JML is not yet fully standardized. See Reference Manual Chapter 15 and §§8.8.4 and 9.1.9.


-----

%keywords ghost
## Ghost declarations
A ghost declaration introduces an executable specification-only field, local variable, method, or class. Ghost state is visible only to JML and may be updated with set; runtime assertion checking may compile it into instrumented classes. See Reference Manual §§3.5, 7.5, 9.2, and 11.3.


-----

%keywords BEGIN END
## Specification statement groups
BEGIN and END are legacy or case-variant spellings of JML's begin and end statements. These delimit a group of statements for a block contract without introducing a Java scope. See Reference Manual §11.12.


-----

%keywords helper
## Helper methods
A helper method is exempt from the usual assumptions and obligations concerning the containing class's invariants. Helper methods are commonly private utilities and are required for methods called from invariants. See Reference Manual §8.7.6 and §3.13.


-----

%keywords hence_by hence_by_redundantly
## Deprecated proof hints
A hence_by statement supplied an intermediate proof justification. It is deprecated; use assume or tool-specific proof facilities instead. See Reference Manual Appendix C.14.


-----

%keywords implies_that
## Implied behaviors
implies_that introduces redundant behaviors that must be logical consequences of the primary method behaviors. It can provide a clearer alternative formulation or useful lemmas for verification. See Reference Manual §8.2.


-----

%keywords in in_redundantly \into
## Data-group membership
An in clause adds its associated field to one or more data groups. \into occurs in the maps ... \into ... syntax, while in_redundantly is a legacy redundant form. See Reference Manual §§9.4.1–9.4.2.


-----

%keywords initializer static_initializer
## Initialization contracts
initializer specifies the common instance-initialization process, while static_initializer specifies class initialization. Their contracts summarize initialization independently of individual constructors or static blocks. See Reference Manual §§7.10–7.11.


-----

%keywords initially
## Constructor postcondition
An initially clause adds a postcondition to every constructor of the declaring class for which the clause is visible. It is evaluated in each constructor's post-state and is not inherited by constructors of derived classes. See Reference Manual §7.4.


-----

%keywords instance
## Instance members
The instance modifier explicitly declares a JML field to be non-static. It is principally useful for model fields in interfaces, where fields would otherwise default to static. See Reference Manual §§3.8 and 9.1.7.


-----

%keywords two_state no_state
## State dependence of methods
A no_state method neither reads nor writes heap state, so its result depends only on its arguments. two_state is a KeY extension for model methods that can refer to an old and a current state. See Reference Manual §§3.9.4 and 8.7.1.


-----

%keywords non_null_by_default nullable_by_default
## Default nullness
These modifiers set the default nullness of otherwise unannotated reference types within a class or method. The default applies recursively to nested declarations unless overridden, but is not inherited by derived classes. A declaration cannot use both defaults simultaneously. See Reference Manual §§3.4.2, 7.1.2, and 8.7.3.


-----

%keywords non_null nullable
## Nullness type modifiers
non_null specifies that a reference value may never be null, while nullable permits null. They correspond to the Java type annotations @NonNull and @Nullable. See Reference Manual §§3.4 and 9.1.2.


-----

%keywords \invariant_for
## Object invariants
\invariant_for(object) denotes the conjunction of the applicable instance invariants for the given object. Its value is true when the argument is null. See Reference Manual §12.5.28.


-----

%keywords \is_initialized
## Class-initialization state
\is_initialized(Type) is true if the named class has completed static initialization. With no arguments, it is true. See Reference Manual §12.5.27.


-----

%keywords \lbl \lblneg \lblpos
## Labeled expression values
\lbl labels a subexpression so a verification tool can report its value in a counterexample or runtime diagnostic. \lblpos and \lblneg are deprecated variants. See Reference Manual §12.5.26 and Appendix C.18.


-----

%keywords \lockset
## Current lock set
\lockset denotes the set of objects whose locks are currently held. It belongs to JML's limited concurrency specification facilities and may not be supported by all tools. See Reference Manual §12.5.34.


-----

%keywords loop_contract
## Loop contracts
loop_contract is a tool-specific wrapper or marker for a loop specification. Standard JML places loop_invariant, loop_writes, and decreases clauses immediately before the loop without this keyword. See Reference Manual §11.10.


-----

%keywords loop_invariant maintaining loop_invariant_free
## Loop invariants
A loop_invariant states a Boolean property that must hold before the loop and after each completed iteration. maintaining is a synonym, while loop_invariant_free is a tool-specific extension. See Reference Manual §11.10.1.


-----

%keywords loop_invariant_redundantly maintaining_redundantly
## Redundant loop invariants
These clauses state loop invariants expected to follow from the other loop specifications. maintaining_redundantly is the redundant form of the maintaining synonym. See Reference Manual §§3.16 and 11.10.1.


-----

%keywords maps maps_redundantly
## Data-group mapping
A maps clause adds the locations denoted by a store-reference expression to one or more data groups. maps_redundantly is a legacy redundant form. See Reference Manual §9.4.2.


-----

%keywords model
## Model declarations
A model declaration introduces an abstract specification-only field, method, class, or interface. Model fields may be constrained by contracts or defined using represents, and model methods without bodies are interpreted through their specifications. See Reference Manual §§3.5, 7.6, 7.8, and 7.9.


-----

%keywords model_program
## Model programs
A model program specifies behavior as an abstract algorithm that an implementation must refine. The standalone model_program keyword is legacy syntax; current JML places a model-program block inside a behavior. See Reference Manual Chapter 16.


-----

%keywords monitored
## Monitored fields
A monitored field may be read or written only while the executing thread holds the lock of the object containing that field. It is intended for concurrency specifications. See Reference Manual §9.1.8.


-----

%keywords monitors_for
## Field monitors
A monitors_for clause specifies objects whose locks must be held whenever the associated field is read or written.
java //@ monitors_for value = lock;
This is a concurrency-oriented feature and may have limited tool support. See Reference Manual §7.14.


-----

%keywords } {
## Braces
Braces delimit Java or JML blocks, including model-program blocks and nested specification cases. In JML, {| and |} are separate tokens used for nested specification clauses. See Reference Manual §§4.5, 8.1.2, and 16.3.


-----

%keywords \nonnullelements nonnullelements
## Non-null array or collection elements
\nonnullelements(value) is true when the value is non-null and all of its elements are non-null. For multidimensional arrays, intermediate subarrays must also be non-null. The spelling without the backslash is legacy or tool-specific. See Reference Manual §12.5.17.


-----

%keywords \nothing \strictly_nothing
## Empty location set
\nothing denotes an empty set of memory locations or, in context, an empty exception or method set. \strictly_nothing is a KeY extension with tool-specific framing semantics. See Reference Manual §§5.9.1, 8.5.3, and Appendix B.


-----

%keywords \not_assigned \not_modified
## Unchanged-state predicates
\not_assigned(locations) states that the given locations were not assigned during the relevant execution interval. \not_modified(locations) states that their values are unchanged between the old and current states. See Reference Manual §§12.5.30–12.5.31.


-----

%keywords \not_specified
## Deprecated unspecified predicate
\not_specified was formerly used where a clause intentionally omitted a predicate. It is deprecated and should be replaced by an explicit specification or the clause's defined default. See Reference Manual Appendix C.12.


-----

%keywords \nowarn \nowarn_op \warn \warn_op
## Warning control
These tokens control or suppress verification warnings in some older tools. \nowarn_op and \warn_op, together with the former nowarn annotation, have been removed from standard JML because they can introduce implicit unsound assumptions. Consult tool documentation and Reference Manual Appendix C.13.


-----

%keywords \old old \pre
## Previous-state values
\old(expression) evaluates its argument in the contract's pre-state or at an explicitly named label, while \pre(expression) is shorthand for the method pre-state. An old method-specification clause declares and initializes a named value in the method's pre-state. See Reference Manual §§8.6.7 and 12.5.14.


-----

%keywords \only_accessed \only_assigned \only_called \only_captured
## Execution-footprint predicates
These two-state predicates constrain, respectively, the locations read, locations assigned, methods called, or references captured during an execution interval. They are principally used in postconditions and specification statements. See Reference Manual §§12.5.32–12.5.33.


-----

%keywords or
## Alternative specification or choice
or separates alternatives in JML's experimental choose and repeat statements. It is not a general Boolean operator; use || or | for Boolean disjunction. See Reference Manual §§11.8–11.9.


-----

%keywords post_redundantly pre_redundantly
## Legacy redundant contract clauses
post_redundantly and pre_redundantly are legacy synonyms for ensures_redundantly and requires_redundantly. New specifications should use the current clause names. See Reference Manual §3.16 and §§8.5.1–8.5.2.


-----

%keywords pure strictly_pure
## Method purity
A pure method does not modify memory locations from its pre-state, although it may allocate and initialize new objects. A strictly_pure method neither modifies the heap nor allocates objects and returns a deterministic result. See Reference Manual §§3.9.1–3.9.3 and 8.7.1.


-----

%keywords \reach
## Reachable objects
\reach(object) denotes the set containing the object and the objects recursively reachable through its fields. This is an experimental feature and is generally unsuitable for runtime assertion checking. See Reference Manual §12.5.35.


-----

%keywords readable writable
## Conditional field access
readable field if predicate; requires the predicate whenever the field is read, and writable field if predicate; requires it whenever the field is written. Their effective visibility is that of the field being constrained. See Reference Manual §7.13.


-----

%keywords \real
## Mathematical real numbers
\real is JML's primitive type for mathematical real numbers. Java integral and floating-point values can be converted to it, subject to restrictions for NaN and infinity. See Reference Manual §5.6 and §13.2.


-----

%keywords refining
## Block refinement
refining introduces a contract for the following statement or block. The implementation must satisfy the block contract, and subsequent reasoning may use that contract as a summary. See Reference Manual §11.11.


-----

%keywords represents represents_redundantly \such_that
## Model-field representation
A represents clause defines or constrains the value of a model field. = gives a functional representation, while \such_that gives a relational constraint; represents_redundantly marks a redundant definition. See Reference Manual §7.7.


-----

%keywords \result
## Method result
\result denotes the value returned by the method being specified. It may appear only in clauses evaluated after normal termination and is invalid for constructors or void methods. See Reference Manual §12.5.10.


-----

%keywords \safe_math
## Safe arithmetic expression
\safe_math(expression) evaluates the enclosed specification expression using fixed-width Java numeric types while reporting possible overflow or underflow. It overrides enclosing arithmetic-mode settings for its argument. See Reference Manual §§12.5.18 and 13.1.3.


-----

%keywords \same
## Inherited precondition
\same is a legacy token formerly used to reuse an inherited precondition. It is no longer part of current standard JML; specification inheritance and also provide the standard composition mechanism. See Reference Manual §§3.2 and 8.1.4.


-----

%keywords set
## Ghost-state execution
A set statement executes a Java-like statement for specification purposes, most commonly to assign a ghost variable or field.
java //@ set calls++;
Its side effects must be confined to specification state. See Reference Manual §11.7.


-----

%keywords signals signals_redundantly exsures exsures_redundantly
## Exceptional postconditions
A signals (ExceptionType e) predicate; clause specifies a condition that must hold when a method exits by throwing the indicated exception type. exsures is a legacy synonym, and _redundantly variants state redundant exceptional postconditions. See Reference Manual §8.5.4.    


-----

%keywords signals_only signals_only_redundantly
## Permitted exceptions
A signals_only clause lists the exception types that a behavior permits the method to throw. signals_only \nothing; forbids exceptional termination. The _redundantly form marks a redundant restriction. See Reference Manual §8.5.5.


-----

%keywords spec_package spec_private spec_protected spec_public
## Specification visibility
spec_public and spec_protected give a Java declaration public or protected visibility for specification purposes without changing Java access control. spec_package and spec_private are tool-specific extensions for package or private specification visibility. See Reference Manual §§3.7, 7.1.6, 8.7.5, and 9.1.3.


-----

%keywords static_initializer
## Static-initialization contract
A static_initializer contract summarizes initialization of a class's static fields and static blocks. Static invariants must hold when class initialization completes. See Reference Manual §7.10.


-----

%keywords <:
## Proper subtype operator
<: compares two \TYPE values and is true when the left operand is a proper subtype of the right operand. Current JML uses <:= for the subtype-or-equal relation; older JML used <: for that relation. See Reference Manual §§5.7.4, 12.5.8, and Appendix C.9.


-----

%keywords \TYPE \type
## First-class Java types
\TYPE is JML's primitive type for representing Java types, including parameterized types. \type(TypeName) constructs the corresponding \TYPE value. See Reference Manual §§5.7 and 12.5.20.


-----

%keywords \typeof
## Dynamic type
\typeof(expression) returns the JML \TYPE corresponding to the expression's dynamic type. Its argument must be non-null when it has reference type. See Reference Manual §12.5.21.


-----

%keywords uninitialized
## Deliberately uninitialized declarations
The uninitialized modifier tells JML to treat a field or local variable as uninitialized even when Java syntax supplies an initializer. Verification must then prove that every path writes it before reading it. See Reference Manual §9.1.6.


-----

%keywords <# <#=
## Lock-order operators
<# and <#= express proper and non-strict ordering between object locks. Users must provide the axioms or invariants defining the intended ordering. See Reference Manual §12.5.9.


-----

%keywords unreachable
## Unreachable code
An unreachable statement asserts that no feasible execution path can reach that program point. Static verification must prove the location unreachable, while runtime checking reports execution of it as a failure. See Reference Manual §11.6.


-----

%keywords when when_redundantly
## Concurrency guard
A when clause gives a condition that must hold before a concurrent method proceeds past its commit point. The default condition is true, and when_redundantly marks a redundant guard. This concurrency-oriented feature has limited tool support. See Reference Manual §8.6.6.


-----

%keywords abrupt_behavior abrupt_behaviour
## Abrupt behavior
These legacy or tool-specific behavior forms describe termination through control transfer such as break, continue, return, or an exception. Current standard JML represents such outcomes in block contracts with breaks, continues, returns, and throws. See Reference Manual §§8.6.12–8.6.15 and 11.11.


-----

%keywords throws
## Throw postcondition
A throws clause in a block contract specifies a condition that must hold when the governed statement exits by throwing an exception. The thrown object is available through \exception. See Reference Manual §8.6.13.


-----

%keywords \exception
## Thrown exception
\exception denotes the exception object when evaluating a clause in an exceptional post-state. It can be used instead of the variable declared by a signals clause and is also available in suitable resource clauses. See Reference Manual §12.5.11.


-----

%keywords \by
## Tool-specific proof annotation
\by is used by some verification tools to attach a proof justification or strategy. It has no standard semantics in the current JML Reference Manual. Consult the documentation of the tool processing the specification.


-----

%keywords constructor
## Deprecated constructor marker
The former constructor marker explicitly identified a constructor declaration in specification syntax. It has been removed from current JML because normal Java declaration syntax is unambiguous. See Reference Manual Appendix C.17.


-----

%keywords method
## Deprecated method marker
The former method marker explicitly identified a method declaration in specification syntax. It has been removed from current JML because normal Java declaration syntax is sufficient. See Reference Manual Appendix C.17.


-----

%keywords field
## Deprecated field marker
The former field marker explicitly identified a field declaration in specification syntax. It has been removed from current JML because normal Java declaration syntax is sufficient. See Reference Manual Appendix C.17.


-----

%keywords initializer
## Instance initializer specification
An initializer contract summarizes the instance-field initializers and instance-initialization blocks executed during construction. It is distinct from an individual constructor contract. See Reference Manual §7.11.


-----

%keywords no_state
## Heap-independent method
A no_state method neither reads nor modifies heap state or the external environment. Its result is therefore entirely determined by its arguments, apart from permitted primitive compile-time constants. See Reference Manual §§3.9.4 and 8.7.1.


-----

%keywords non_null
## Non-null type
non_null is a type modifier requiring values of the annotated reference type to be non-null. Initializers, assignments, arguments, and return values must respect that requirement. See Reference Manual §3.4.


-----

%keywords nullable
## Nullable type
nullable is a type modifier allowing values of the annotated reference type to be null. It overrides a surrounding non-null default for that type use. See Reference Manual §3.4.


-----

%keywords normal_example exceptional_example
## Normal and exceptional examples
normal_example illustrates a normally terminating case, while exceptional_example illustrates an exceptionally terminating case. These cases document consequences of the primary specification and do not replace it. See Reference Manual §8.3.


-----

%keywords <-
## Deprecated representation arrow
<- was formerly used in functional represents and monitors_for clauses. Current JML uses = instead. See Reference Manual Appendices C.3–C.4.


-----

%keywords writable

Writable-if condition
`writable <field> \if <predicate>;` states a condition that must hold whenever the named field is written. The field must be visible in the containing class, and the predicate must have Boolean type. See Reference Manual §7.13.