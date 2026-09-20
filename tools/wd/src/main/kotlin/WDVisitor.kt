/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.wd

import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.body.JmlClassExprDeclaration
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import com.github.javaparser.ast.jml.expr.JmlLabelExpr
import com.github.javaparser.ast.jml.expr.JmlLetExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlTypeExpr
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt
import com.github.javaparser.ast.visitor.GenericVisitorAdapter
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import io.github.jmltoolkit.smt.ArithmeticTranslator
import io.github.jmltoolkit.smt.JmlExpr2Smt
import io.github.jmltoolkit.smt.SmtQuery
import io.github.jmltoolkit.smt.SmtTermFactory
import io.github.jmltoolkit.smt.Boxing
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType
import java.math.BigInteger

/**
 *
 * @author Alexander Weigl
 * @version 1 (14.06.22)
 */
class WDVisitor : VoidVisitorAdapter<Any?>() {
    override fun visit(n: JmlExpressionStmt, arg: Any?) {
        n.expression.accept<Any>(this, arg)
    }
}

class WDVisitorExpr(smtLog: SmtQuery, private val translator: ArithmeticTranslator) : GenericVisitorAdapter<SExpr, Any?>() {
    private val smtFormula: JmlExpr2Smt = JmlExpr2Smt(smtLog, translator)

    override fun visit(n: NameExpr, arg: Any?): SExpr {
        val name = n.nameAsString
        return when (name) {
            "\\result", "\\exception" -> term.makeTrue()
            else -> term.makeTrue()
        }
    }

    /**
     * An array access `a[i]` is well-defined iff `a` and `i` are well-defined
     * and the index is within the bounds of the array (JLS 10.4, no
     * ArrayIndexOutOfBoundsException).
     */
    override fun visit(n: ArrayAccessExpr, arg: Any?): SExpr {
        val base = term.and(
            wd(n.name),
            wd(n.index)
        )
        val array = smtTerm(n.name) ?: return base
        val index = smtTerm(n.index) ?: return base
        return try {
            val length = translator.arrayLength(array)
            val zero = translator.makeInt(BigInteger.ZERO)
            term.and(
                base,
                term.lessOrEquals(zero, index, true),
                term.lessThan(index, length)
            )
        } catch (e: Throwable) {
            // fall back to the well-definedness of the sub-expressions,
            // e.g. if types could not be resolved
            base
        }
    }

    override fun visit(n: ArrayCreationExpr, arg: Any?): SExpr {
        // TODO
        return n.initializer.get().accept(this, arg)
    }

    override fun visit(n: ArrayInitializerExpr, arg: Any?): SExpr {
        val seq = n.values.map { it.accept(this, arg) }
        return term.and(seq)
    }

    override fun visit(n: AssignExpr, arg: Any?): SExpr = term.makeFalse()

    override fun visit(n: BinaryExpr, arg: Any?): SExpr {
        when (n.operator) {
            BinaryExpr.Operator.IMPLICATION -> {
                val be = BinaryExpr(
                    UnaryExpr(n.left, UnaryExpr.Operator.LOGICAL_COMPLEMENT),
                    n.right, BinaryExpr.Operator.OR
                )
                return be.accept(this, arg)
            }

            BinaryExpr.Operator.DIVIDE, BinaryExpr.Operator.REMAINDER -> {
                // division by zero and unboxing of a null operand
                // (JLS 5.6.2) make the expression undefined
                val npe = term.and(listOfNotNull(npeUnboxing(n.left), npeUnboxing(n.right)))
                val divisorNotZero = try {
                    val fml = n.right.accept(smtFormula, arg)
                    term.not(
                        translator.binary(
                            BinaryExpr.Operator.EQUALS,
                            fml, smtFormula.translator.makeInt(BigInteger.ZERO)
                        )
                    )
                } catch (t: Throwable) {
                    // types could not be resolved, e.g. an unresolvable
                    // operand; fall back to the sub-expressions' well-definedness
                    term.makeTrue()
                }
                return term.and(
                    n.right.accept(this, arg),
                    n.left.accept(this, arg),
                    npe,
                    divisorNotZero
                )
            }

            else -> {
                // unboxing of a null operand causes a NullPointerException
                val npe = term.and(listOfNotNull(npeUnboxing(n.left), npeUnboxing(n.right)))
                return term.and(
                    n.right.accept(this, arg),
                    n.left.accept(this, arg),
                    npe
                )
            }
        }
    }

    /**
     * Well-definedness contribution of an unboxing conversion (JLS 5.6.2):
     * if the operand is a boxed primitive, its value must be non-null,
     * otherwise `x.intValue()` throws a NullPointerException.
     */
    private fun npeUnboxing(e: Expression): SExpr? {
        val primitive = try {
            Boxing.primitiveOf(e.calculateResolvedType())
        } catch (t: Throwable) {
            null
        } ?: return null
        val obj = smtTerm(e) ?: return null
        return try {
            translator.unbox(obj, primitive)
            term.nonNull(obj)
        } catch (t: Throwable) {
            null
        }
    }

    override fun visit(n: BooleanLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: CastExpr, arg: Any?): SExpr {
        // TODO Type-check?
        return n.expression.accept(this, arg)
    }

    override fun visit(n: CharLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: ClassExpr, arg: Any?): SExpr = term.makeFalse()

    override fun visit(n: DoubleLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: EnclosedExpr, arg: Any?): SExpr = wd(n.inner)

    /**
     * A field access `expr.f` is well-defined iff `expr` is well-defined and
     * its value is non-null (no NullPointerException). Static field accesses,
     * i.e. a scope naming a type, cannot be translated into an object term
     * and are skipped.
     */
    override fun visit(n: FieldAccessExpr, arg: Any?): SExpr {
        val scopeWd = wd(n.scope)
        if (n.scope is ThisExpr || n.scope is SuperExpr || n.scope is TypeExpr) {
            // `this` and `super` are always non-null in a well-formed
            // specification context
            return scopeWd
        }
        val receiver = smtTerm(n.scope) ?: return scopeWd
        return term.and(scopeWd, term.nonNull(receiver))
    }

    override fun visit(n: InstanceOfExpr, arg: Any?): SExpr = n.expression.accept(this, arg)

    override fun visit(n: IntegerLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: StringLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: SuperExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: ThisExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: UnaryExpr, arg: Any?): SExpr = n.expression.accept(this, arg)

    override fun visit(n: LambdaExpr, arg: Any?): SExpr = super.visit(n, arg)

    override fun visit(n: MethodReferenceExpr, arg: Any?): SExpr = super.visit(n, arg)

    override fun visit(n: TypeExpr, arg: Any?): SExpr = super.visit(n, arg)

    override fun visit(n: SwitchExpr, arg: Any?): SExpr = term.and(wd(n.selector))

    override fun visit(n: TextBlockLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: RecordPatternExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: TypePatternExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: JmlQuantifiedExpr, arg: Any?): SExpr {
        /*The quantified-expression is well-defined iff the two sub-expressions are well-defined. For a quantifier Q*/
        val seq: List<SExpr> = n.expressions.map { it.accept(this, arg) }
        val r: Expression = n.expressions[0]
        val v: Expression = n.expressions[0]

        val args: List<SExpr> = ArrayList<SExpr>()

        if (JmlQuantifiedExpr.JmlDefaultBinder.CHOOSE == n.binder) {
            return term.and(
                term.forall(args, wd(r)),
                term.forall(args, term.impl(valueOf(r), wd(v))),
                term.exists(
                    args,
                        term.and(
                        valueOf(r),
                        valueOf(v)
                    )
                )
            )
        }
        return term.and(
            term.forall(args, wd(r)),
            term.forall(args, term.impl(valueOf(r), wd(v)))
        )
    }

    override fun visit(n: NullLiteralExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: ObjectCreationExpr, arg: Any?): SExpr =
        term.and(n.arguments.map { e: Expression -> wd(e) })

    private fun valueOf(e: Expression): SExpr = e.accept(smtFormula, null)

    private fun wd(e: Expression): SExpr = e.accept(this, null) ?: term.makeTrue()

    /**
     * Translates an expression into an object term of the SMT object
     * formalization, cf. [io.github.jmltoolkit.smt.SmtObjectModel].
     * Returns `null` if the expression is not an object (e.g. a primitive or
     * array term) or cannot be resolved.
     */
    private fun smtTerm(e: Expression): SExpr? = try {
        val t = e.accept(smtFormula, null)
        if (t != null && t.smtType == SmtType.JAVA_OBJECT) t else null
    } catch (t: Throwable) {
        null
    }

    override fun visit(n: JmlExpressionStmt, arg: Any?): SExpr = wd(n.expression)

    override fun visit(n: JmlLabelExpr, arg: Any?): SExpr = wd(n.expression)

    override fun visit(n: JmlLetExpr, arg: Any?): SExpr = term.and(wd(n.body),  /* TODO  arguments */term.makeTrue())

    override fun visit(n: JmlClassExprDeclaration, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: JmlTypeExpr, arg: Any?): SExpr = term.makeTrue()

    override fun visit(n: JmlMultiExprClause, arg: Any?) =
        term.and(n.expressions.map { e: Expression -> this.wd(e) })

    /**
     * A method invocation `expr.m(...)` is well-defined iff the receiver (if
     * any) is well-defined and non-null (no NullPointerException) and all
     * arguments are well-defined.
     */
    override fun visit(n: MethodCallExpr, arg: Any?): SExpr {
        val name = n.nameAsString
        when (name) {
            "\\old", "\\pre", "\\past" ->                 /* Well-definedness: The expression is well-defined if the first argument is well-defined
                   and any label argument names either a built-in label (§11.611.6) or an in-scope Java or
                   JML ghost label (S11.511.5).*/
                return n.arguments[0].accept(this, arg)

            "\\fresh" ->                 /* Well-definedness: The argument must be well-defined and non-null. The second argument,
                   if present, must be the identifier corresponding to an in-scope label or a built-in label. */
                return n.arguments[0].accept(this, arg)
        }
        val parts: MutableList<SExpr> = ArrayList()
        n.scope.ifPresent { scope: Expression ->
            parts.add(wd(scope))
            if (scope !is ThisExpr && scope !is SuperExpr && scope !is TypeExpr) {
                val receiver = smtTerm(scope)
                if (receiver != null) {
                    parts.add(term.nonNull(receiver))
                }
            }
        }
        n.arguments.forEach { e: Expression -> parts.add(e.accept(this, arg) ?: term.makeTrue()) }
        if (parts.isEmpty()) return term.makeTrue()
        return term.and(parts)
    }

    companion object {
        private val term: SmtTermFactory = SmtTermFactory
    }
}
