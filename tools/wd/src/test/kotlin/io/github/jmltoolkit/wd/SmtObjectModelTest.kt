package io.github.jmltoolkit.wd

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import io.github.jmltoolkit.smt.BitVectorArithmeticTranslator
import io.github.jmltoolkit.smt.SmtObjectModel
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.model.SmtType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for the SMT formalization of Java objects and its usage in the
 * well-definedness checks (null-pointer analysis).
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
internal class SmtObjectModelTest {
    @Test
    fun testObjectModelDeclaration() {
        val q = SmtQuery()
        SmtObjectModel.declare(q)
        val s = q.toString()
        Assertions.assertTrue(s.contains("(declare-sort Object 0)"), s)
        Assertions.assertTrue(s.contains("(declare-const null Object)"), s)
        Assertions.assertTrue(s.contains("(declare-fun instanceof (Object String) Bool)"), s)
        Assertions.assertTrue(
            s.contains("(assert (forall ((s String)) (not (instanceof null s))))"),
            s
        )
    }

    @Test
    fun testObjectModelIdempotent() {
        val q = SmtQuery()
        SmtObjectModel.declare(q)
        SmtObjectModel.declare(q)
        val s = q.toString()
        Assertions.assertEquals(1, s.split("declare-sort").size - 1, s)
    }

    @Test
    fun testMethodCallRequiresNonNullReceiver() {
        val f = wdFormula("new Object().equals(null)")
        val s = f.toString()
        // receiver of the method call must be non-null
        Assertions.assertTrue(s.contains("not (= anon1 null)"), s)
    }

    @Test
    fun testFieldAccessOnUnresolvableFallsBack() {
        // `x.f` cannot be resolved without a symbol solver; the WD check must
        // not fail but fall back to the well-definedness of the scope
        val f = wdFormula("x.f")
        Assertions.assertEquals("true", f.toString())
    }

    @Test
    fun testArrayAccessOutOfBounds() {
        // without a symbol solver, the array term cannot be resolved and the
        // bounds check is skipped: `a[i]` remains well-defined
        val f = wdFormula("a[i]")
        // only trivially true conjuncts, in particular no bounds or null check
        Assertions.assertFalse(f.toString().contains("null"), f.toString())
        Assertions.assertFalse(f.toString().contains("select"), f.toString())
    }

    @Test
    fun testBoxingUnboxingDeclarations() {
        val q = SmtQuery()
        SmtObjectModel.declare(q)
        val tr = BitVectorArithmeticTranslator(q)
        val x = SmtTermFactory.variable(SmtType.JAVA_OBJECT, null, "x")
        tr.unbox(x, ResolvedPrimitiveType.INT)

        val unboxed = tr.unbox(x, ResolvedPrimitiveType.INT).toString()
        Assertions.assertEquals("(unbox\$int x)", unboxed)

        val s = q.toString()
        Assertions.assertTrue(s.contains("(declare-fun unbox\$int (Object) (_ BitVec 32))"), s)
        Assertions.assertTrue(s.contains("(declare-fun box\$int ((_ BitVec 32)) Object)"), s)
        Assertions.assertTrue(
            s.contains("(assert (forall ((x (_ BitVec 32))) (= (unbox\$int (box\$int x)) x)))"),
            s
        )

        // idempotent declaration
        tr.unbox(x, ResolvedPrimitiveType.INT)
        Assertions.assertEquals(1, q.toString().split("declare-fun unbox").size - 1)
    }

    private fun wdFormula(expr: String) = run {
        val parser = JavaParser()
        val e = parser.parseJmlExpression<Expression>(expr)
        Assertions.assertTrue(e.isSuccessful, e.toString())
        WdFacade.wdFormula(e.result.get())
    }
}
