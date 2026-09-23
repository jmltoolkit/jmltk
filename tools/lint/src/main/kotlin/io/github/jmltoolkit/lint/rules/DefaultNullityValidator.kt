/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration
import io.github.jmltoolkit.lint.JmlLintingConfig
import io.github.jmltoolkit.lint.LintProblemReporter
import io.github.jmltoolkit.lint.LintRule
import io.github.jmltoolkit.lint.LintRuleVisitor

/**
 * The possible default nullity declarations of a class.
 *
 * See JML Reference Manual, §3.4 (Default Nullity Values):
 * The `non_null_by_default` and `nullable_by_default` modifiers, or equivalently the
 * `@NonNullByDefault` and `@NullableByDefault` annotations, specify the default nullity
 * declaration within the class. The default applies recursively to nested and inner classes
 * that do not have default nullity declarations of their own. These modifiers are *not*
 * inherited by derived classes. The default for a top-level class is
 * `non_null_by_default` (alterable by tools via the linting configuration).
 *
 * `UNMARKED` models the JSpecify `@NullUnmarked` semantics: nullness is unspecified,
 * i.e. an own declaration that terminates the inheritance of an enclosing default
 * without making types non-null or nullable.
 */
enum class DefaultNullity(val keyword: String, val jmlAnnotationName: String) {
    NON_NULL("non_null_by_default", "NonNullByDefault"),
    NULLABLE("nullable_by_default", "NullableByDefault"),

    /**
     * JSpecify `@NullUnmarked`: nullness is unspecified, there is no default nullity.
     */
    UNMARKED("unmarked", "NullUnmarked");

    companion object {
        /**
         * The JSpecify annotations that act as a default nullity declaration:
         * `@NullMarked` (as well as `@NonNullApi` and `@NonNullFields`) declares
         * unannotated type uses as non-null, `@NullUnmarked` declares them unspecified.
         */
        val JSPECIFY_DEFAULT_ANNOTATIONS: Map<String, DefaultNullity> = mapOf(
            "NullMarked" to NON_NULL,
            "NonNullApi" to NON_NULL,
            "NonNullFields" to NON_NULL,
            "NullUnmarked" to UNMARKED,
        )

        /** The JML annotation forms of the default nullity declarations. */
        val JML_DEFAULT_ANNOTATIONS: List<DefaultNullity> = listOf(NON_NULL, NULLABLE)
    }
}

/**
 * Validates the JML default nullity modifiers (JML Reference Manual §3.4).
 *
 * Checks:
 *  - a class must not be modified by both `non_null_by_default` and `nullable_by_default`
 *    (or their equivalent annotations) at once,
 *  - these modifiers are only allowed on type declarations,
 *  - default nullity modifiers are not inherited by derived classes (hint).
 *
 * When the `checkJspecifyNullness` option is enabled, the JSpecify nullness
 * annotations are considered with their semantics:
 *  - `@NullMarked`, `@NonNullApi` and `@NonNullFields` act as a `non_null_by_default`
 *    declaration for the annotated type (and recursively for its nested types),
 *  - `@NullUnmarked` declares nullness as unspecified and terminates the inheritance
 *    of an enclosing default nullity,
 *  - conflicting JSpecify declarations (`@NullMarked` with `@NullUnmarked`) and
 *    combinations of JSpecify and JML default nullity declarations with opposite
 *    meaning are reported as errors.
 * The JSpecify type-use annotations `@NonNull` and `@Nullable` do not declare a default
 * nullity and are not inspected by this rule.
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
class DefaultNullityValidator : LintRuleVisitor() {
    override val visitor: VoidVisitorAdapter<LintProblemReporter>
        get() = object : VoidVisitorAdapter<LintProblemReporter>() {
            override fun visit(n: ClassOrInterfaceDeclaration, arg: LintProblemReporter) {
                val own = explicitDefault(n, n.modifiers, n.annotations, arg)

                // Default nullity modifiers are not inherited by derived classes. Hint when a
                // superclass declares an explicit default nullity but the subclass does not,
                // because the subclass silently uses its enclosing (or top-level) default instead.
                if (own == null && n.extendedTypes.isNotEmpty()) {
                    val superClass = resolveSuperclass(n.extendedTypes[0])
                    if (superClass != null) {
                        val parentDefault =
                            explicitDefault(superClass, superClass.modifiers, superClass.annotations, arg)
                        if (parentDefault != null) {
                            val effective = effectiveDefault(n, arg)
                            arg.hint(
                                n, CATEGORY, NOT_INHERITED.id,
                                "Default nullity modifiers are not inherited by derived classes. " +
                                    "Class ${n.nameAsString} does not inherit the ${parentDefault.keyword} " +
                                    "of ${superClass.nameAsString}; the effective default is ${effective.keyword}.",
                            )
                        }
                    }
                }
                super.visit(n, arg)
            }

            override fun visit(n: EnumDeclaration, arg: LintProblemReporter) {
                checkTypeDeclaration(n, arg, config!!)
                super.visit(n, arg)
            }

            override fun visit(n: RecordDeclaration, arg: LintProblemReporter) {
                checkTypeDeclaration(n, arg, config!!)
                super.visit(n, arg)
            }

            override fun visit(n: AnnotationDeclaration, arg: LintProblemReporter) {
                checkTypeDeclaration(n, arg, config!!)
                super.visit(n, arg)
            }

            override fun visit(n: MethodDeclaration, arg: LintProblemReporter) {
                checkNoNullityDefaultOnMember(n, n.modifiers, n.annotations, arg)
                super.visit(n, arg)
            }

            override fun visit(n: FieldDeclaration, arg: LintProblemReporter) {
                checkNoNullityDefaultOnMember(n, n.modifiers, n.annotations, arg)
                super.visit(n, arg)
            }
        }

    private fun reportProblem(n: Node, meta: LintProblemMeta, arg: LintProblemReporter) {
        @Suppress("UNCHECKED_CAST")
        val node = n as NodeWithTokenRange<Node?>
        arg.report(meta.create(node))
    }

    /**
     * Returns the default nullity explicitly declared on the given declaration, or null
     * if none was declared. Reports a conflict if both variants are given.
     */
    private fun explicitDefault(
        n: Node,
        modifiers: Iterable<Modifier>,
        annotations: Iterable<AnnotationExpr>,
        arg: LintProblemReporter,
    ): DefaultNullity? {
        var found: DefaultNullity? = null
        for (modifier in modifiers) {
            val d = when (modifier.keyword) {
                Modifier.DefaultKeyword.JML_NON_NULL_BY_DEFAULT -> DefaultNullity.NON_NULL
                Modifier.DefaultKeyword.JML_NULLABLE_BY_DEFAULT -> DefaultNullity.NULLABLE
                else -> null
            }
            if (d != null) {
                if (found == null) {
                    found = d
                } else if (found != d) {
                    reportProblem(n, BOTH_DEFAULT_NULLITY, arg)
                }
            }
        }
        for (annotation in annotations) {
            val d = annotationDefault(annotation.nameAsString)
            if (d != null) {
                if (found == null) {
                    found = d
                } else if (found != d) {
                    reportProblem(n, BOTH_DEFAULT_NULLITY, arg)
                }
            }
        }
        return found
    }

    /**
     * Maps an annotation name to the default nullity it declares, or null. Recognizes
     * the JML annotation forms `@NonNullByDefault`/`@NullableByDefault` and, if
     * the `checkJspecifyNullness` option is enabled, the JSpecify default nullness
     * annotations (`@NullMarked`, `@NonNullApi`, `@NonNullFields`, `@NullUnmarked`).
     * Both simple and qualified names (e.g. `org.jspecify.annotations.NullMarked`) match.
     */
    private fun annotationDefault(name: String): DefaultNullity? {
        var found: DefaultNullity? = null
        for (d in DefaultNullity.JML_DEFAULT_ANNOTATIONS) {
            if (name.endsWith(d.jmlAnnotationName)) found = d
        }
        if (checkJspecify) {
            for ((suffix, d) in DefaultNullity.JSPECIFY_DEFAULT_ANNOTATIONS) {
                if (name.endsWith(suffix)) found = d
            }
        }
        return found
    }

    private fun checkTypeDeclaration(
        n: TypeDeclaration<*>,
        arg: LintProblemReporter,
        config: JmlLintingConfig,
    ) {
        explicitDefault(n, n.modifiers, n.annotations, arg)
    }


    /**
     * Default nullity modifiers only make sense on classes; using them on members
     * is an error.
     */
    private fun checkNoNullityDefaultOnMember(
        n: Node,
        modifiers: Iterable<Modifier>,
        annotations: Iterable<AnnotationExpr>,
        arg: LintProblemReporter,
    ) {
        for (modifier in modifiers) {
            if (modifier.keyword == Modifier.DefaultKeyword.JML_NON_NULL_BY_DEFAULT ||
                modifier.keyword == Modifier.DefaultKeyword.JML_NULLABLE_BY_DEFAULT
            ) {
                reportProblem(n, NOT_A_CLASS, arg)
            }
        }
        for (annotation in annotations) {
            if (DefaultNullity.JML_DEFAULT_ANNOTATIONS.any {
                    annotation.nameAsString.endsWith(it.jmlAnnotationName)
                }
            ) {
                // Only the JML annotations; the JSpecify default nullness annotations are
                // also allowed on methods (@NullMarked) and are not checked here.
                reportProblem(n, NOT_A_CLASS, arg)
            }
        }
    }

    /**
     * The effective default nullity for the given class: the explicit declaration of the
     * closest enclosing class that has one, or the (tool-alterable) top-level default,
     * which is `non_null_by_default` per §3.4.
     */
    private fun effectiveDefault(n: ClassOrInterfaceDeclaration, arg: LintProblemReporter): DefaultNullity {
        var enclosing = n.findAncestor(ClassOrInterfaceDeclaration::class.java)
        while (enclosing.isPresent) {
            val c = enclosing.get()
            val d = explicitDefault(c, c.modifiers, c.annotations, arg)
            if (d != null) return d
            enclosing = c.findAncestor(ClassOrInterfaceDeclaration::class.java)
        }
        return topLevelDefault
    }

    /** Top-level default, may be altered by tools. */
    private var topLevelDefault: DefaultNullity = config.topLevelNullity ?: DefaultNullity.NON_NULL

    /** Whether the JSpecify nullness annotations should be considered. */
    private var checkJspecify: Boolean = config.checkJspecifyNullness ?: false

    private fun resolveSuperclass(type: ClassOrInterfaceType): ClassOrInterfaceDeclaration? {
        // Look up the superclass in the same compilation unit first; this is cheap and
        // deterministic. Fall back to symbol resolution for types from other sources.
        val name = type.nameAsString
        val cu = type.findAncestor(CompilationUnit::class.java)
        if (cu.isPresent) {
            val candidate = cu.get().findAll(ClassOrInterfaceDeclaration::class.java)
                .firstOrNull { it.nameAsString == name }
            if (candidate != null) return candidate
        }
        return try {
            (type.resolve() as? JavaParserClassDeclaration)?.wrappedNode
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CATEGORY = "nullness"

        /**
         * A class cannot be modified by conflicting default nullity declarations at once,
         * e.g. both `non_null_by_default` and `nullable_by_default`, or a JSpecify
         * declaration contradicting the JML one.
         */
        val BOTH_DEFAULT_NULLITY: LintProblemMeta = LintProblemMeta(
            "JML-NULLITY-1",
            "A class cannot have conflicting default nullity declarations " +
                "(both non_null_by_default and nullable_by_default at once).",
            LintRule.ERROR,
        )

        /** Default nullity modifiers apply only to classes. */
        val NOT_A_CLASS: LintProblemMeta = LintProblemMeta(
            "JML-NULLITY-2",
            "Default nullity modifiers are only allowed on class declarations.",
            LintRule.ERROR,
        )

        /** Default nullity modifiers are not inherited by derived classes. */
        val NOT_INHERITED: LintProblemMeta = LintProblemMeta(
            "JML-NULLITY-3",
            "Default nullity modifiers are not inherited by derived classes.",
            LintRule.HINT,
        )
    }
}
