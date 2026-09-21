/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
import com.github.javaparser.ast.expr.NameExpr
import io.github.jmltoolkit.lint.JmlLintingConfig
import io.github.jmltoolkit.lint.JmlLintingFacade
import io.github.jmltoolkit.lint.rules.locset.AbsLoc
import io.github.jmltoolkit.lint.rules.locset.Emptiness
import io.github.jmltoolkit.lint.rules.locset.LocsetEvaluator
import io.github.jmltoolkit.lint.rules.locset.Universality
import io.github.jmltoolkit.utils.TestWithJavaParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * Tests for the locset abstract domain and interpreter.
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */
internal class LinterTest : TestWithJavaParser() {
    @Test
    fun everythingWrong() {
        val result = parser.parse(javaClass.getResourceAsStream("EverythingWrong.java"))
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))

        for (lintProblem in actual) {
            println(lintProblem)
        }
    }

    @Test
    fun nullityDefaults() {
        val result = parser.parse(javaClass.getResourceAsStream("NullityDefaults.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))
        val messages = actual.map { it.message }

        // error: both default nullity declarations at once
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("both non_null_by_default and nullable_by_default") },
            "Expected conflict error, got: $messages"
        )
        // error: default nullity modifiers only on classes
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("only allowed on class declarations") },
            "Expected misplaced modifier error, got: $messages"
        )
        // hint: not inherited by derived classes
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("not inherited by derived classes") },
            "Expected non-inheritance hint, got: $messages"
        )
    }

    @Test
    fun jspecifyNullnessDisabledByDefault() {
        val result = parser.parse(javaClass.getResourceAsStream("JspecifyNullness.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))
        val messages = actual.map { it.message }

        // JSpecify annotations must be ignored unless checkJspecifyNullness is enabled
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.none { it.contains("conflicting default nullity") },
            "Expected no JSpecify findings when disabled, got: $messages"
        )
    }

    @Test
    fun jspecifyNullness() {
        val result = parser.parse(javaClass.getResourceAsStream("JspecifyNullness.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual =
            JmlLintingFacade(JmlLintingConfig(checkJspecifyNullness = true))
                .lint(listOf(result.result.get()))
        val messages = actual.map { it.message }

        // error: @NullMarked together with @NullUnmarked conflicts
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.count { it.contains("conflicting default nullity") } >= 2,
            "Expected @NullMarked/@NullUnmarked and mixed JML/JSpecify conflicts, got: $messages"
        )
        // hint: default nullity is not inherited by derived classes (also for JSpecify)
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("not inherited by derived classes") },
            "Expected non-inheritance hint, got: $messages"
        )
    }

    @Test
    fun specVisibility() {
        val result = parser.parse(javaClass.getResourceAsStream("SpecVisibility.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val cu = result.result.get()
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(cu))
        val messages = actual.map { it.message }

        // error: a public specification mentioning a private field/method
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("less visible than the spec_public") },
            "Expected private names in public specs to be reported, got: $messages"
        )
        // ok: spec_private invariant may mention a private field; private method's spec too;
        // spec_public widens a private field/method
        org.junit.jupiter.api.Assertions.assertTrue(
            !messages.any { it.contains("'widened'") || it.contains("'hidden'") },
            "spec_public widened names must not be reported, got: $messages"
        )
    }

    @Test
    fun duplicateNames() {
        val result = parser.parse(javaClass.getResourceAsStream("DuplicateNames.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))
        val messages = actual.map { it.message }
        messages.forEach { println("DUP: $it") }

        // error: ghost field `count` duplicates Java field `count`
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("'count' is already used") },
            "Expected ghost field duplicating a Java field to be reported, got: $messages"
        )
        // error: model method `f` duplicates Java method `f`
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.count { it.contains("'f' is already used") } >= 1,
            "Expected model method duplicating a Java method to be reported, got: $messages"
        )
        // error: Java local `q` duplicates ghost variable `q`
        org.junit.jupiter.api.Assertions.assertTrue(
            messages.any { it.contains("'q' is already used") },
            "Expected Java local duplicating a ghost variable to be reported, got: $messages"
        )
        // ok: ghost `other` and Java local `r` do not clash
        org.junit.jupiter.api.Assertions.assertTrue(
            !messages.any { it.contains("'other'") || it.contains("'r'") },
            "Non-clashing names must not be reported, got: $messages"
        )
    }

    @Test
    fun locsetDomain() {
        // lattice algebra
        assertEquals(AbsLoc.NOTHING, AbsLoc.NOTHING union AbsLoc.NOTHING)
        // union with a single location is precise: ∅ ∪ S = S
        assertEquals(AbsLoc.SINGLE_LOCATION, AbsLoc.NOTHING union AbsLoc.SINGLE_LOCATION)
        assertEquals(AbsLoc.EVERYTHING, AbsLoc.SINGLE_LOCATION union AbsLoc.EVERYTHING)
        assertEquals(AbsLoc.EVERYTHING, AbsLoc.EVERYTHING intersect AbsLoc.EVERYTHING)
        assertEquals(AbsLoc.NOTHING, AbsLoc.NOTHING intersect AbsLoc.EVERYTHING)
        // the contradictory value: empty, yet contains everything
        assertEquals(AbsLoc.BOTTOM, AbsLoc(Emptiness.DEF_EMPTY, Universality.DEF_UNIV))

        // difference: everything minus nothing stays everything
        assertEquals(AbsLoc.EVERYTHING, AbsLoc.EVERYTHING minus AbsLoc.NOTHING)
        // finite minus everything is empty
        assertEquals(AbsLoc.NOTHING, AbsLoc.SINGLE_LOCATION minus AbsLoc.EVERYTHING)
        // nothing joined with a precise set loses the emptiness information
        assertEquals(
            AbsLoc(Emptiness.MAYBE, Universality.DEF_NOT_UNIV),
            AbsLoc.ALL_ARRAY_ELEMENTS union AbsLoc.SINGLE_LOCATION,
        )

        // evaluator: literals, names, environment lookup
        val ev = LocsetEvaluator(mapOf("g" to AbsLoc.EVERYTHING))
        assertEquals(AbsLoc.NOTHING, ev.eval(NameExpr("\\nothing")))
        assertEquals(AbsLoc.NOTHING, ev.eval(NameExpr("\\strictly_nothing")))
        assertEquals(AbsLoc.EVERYTHING, ev.eval(NameExpr("\\everything")))
        assertEquals(AbsLoc.SINGLE_LOCATION, ev.eval(NameExpr("x")))
        assertEquals(AbsLoc.EVERYTHING, ev.eval(NameExpr("g")))
        // frame list is the union
        assertEquals(AbsLoc.EVERYTHING, ev.frame(listOf(NameExpr("x"), NameExpr("g"))))
        assertEquals(AbsLoc.NOTHING, ev.frame(listOf(NameExpr("\\nothing"))))
        // empty expression list is the JML default: \everything
        assertEquals(AbsLoc.EVERYTHING, ev.frame(emptyList()))
    }

    @Test
    fun locsetClauses() {
        val result = parser.parse(javaClass.getResourceAsStream("Locsets.java"))
        result.problems.forEach { System.err.println(it) }
        Assumptions.assumeTrue(result.isSuccessful)
        val actual = JmlLintingFacade(JmlLintingConfig()).lint(listOf(result.result.get()))
        val messages = actual.map { it.message }
        messages.forEach { println("LOCSET: $it") }

        // hint: provably empty frames
        org.junit.jupiter.api.Assertions.assertEquals(
            2,
            messages.count { it.contains("provably empty") },
            "Expected \\nothing and \\strictly_nothing frames to be empty, got: $messages"
        )
        // warn: vacuous frames containing \everything
        org.junit.jupiter.api.Assertions.assertEquals(
            2,
            messages.count { it.contains("vacuous") },
            "Expected vacuous frames to be reported, got: $messages"
        )
        // no false positives for the precise frames
        assertTrue(
            !messages.any { it.contains("assignable") && it.contains("a[*]") },
            "Precise frames must not be reported, got: $messages"
        )
    }
}
