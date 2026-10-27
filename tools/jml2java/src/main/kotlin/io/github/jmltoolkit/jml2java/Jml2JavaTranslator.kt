/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jml2java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.expr.JmlLetExpr
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedVariable
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.VarType
import com.github.javaparser.ast.visitor.GenericVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.utils.JMLUtils
import io.github.jmltoolkit.utils.Pattern
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Alexander Weigl
 * @version 1 (04.10.22)
 */
class Jml2JavaTranslator {
    private val counter = AtomicInteger()

    fun accept(e: Expression?, arg: BlockStmt): Expression? {
        if (Jml2JavaFacade.containsJmlExpression(e)) {
            return e!!.accept(Jml2JavaVisitor(), arg)
        }
        return e // createAssignmentAndAdd(e, arg);
    }

    private fun createAssignmentAndAdd(e: Expression, arg: BlockStmt): Expression {
        arg.addAndGetStatement(createAssignmentFor(e))
        return NameExpr(targetForAssignment)
    }

    private fun createAssignmentFor(e: Expression): Statement {
        val decl = VariableDeclarationExpr(
            VariableDeclarator(
                VarType(),
                newTargetForAssignment(), e
            )
        )
        decl.addModifier(Modifier.DefaultKeyword.FINAL)
        return ExpressionStmt(decl)
    }

    private val targetForAssignment: SimpleName
        get() = SimpleName("_gen_" + counter.get())

    private fun newTargetForAssignment(): SimpleName {
        counter.getAndIncrement()
        return targetForAssignment
    }

    fun findPredicate(n: JmlQuantifiedExpr): Expression = n.expressions[n.expressions.size - 1]

    fun findUpperBound(n: JmlQuantifiedExpr, variable: String): Expression? {
        if (n.expressions.size == 3) return n.expressions[1]

        val e = findBound(n)

        if (e is JmlMultiCompareExpr) {
            if (e.expressions.size == 3 &&
                (e.expressions[1] as? NameExpr)?.nameAsString == variable
            ) {
                if (e.operators[0] == BinaryExpr.Operator.LESS_EQUALS) return e.expressions[0]
                if (e.operators[0] == BinaryExpr.Operator.LESS) {
                    return BinaryExpr(
                    e.expressions[0],
                    IntegerLiteralExpr("1"),
                    BinaryExpr.Operator.PLUS
                )
                }
                throw IllegalStateException()
            }
        }

        val ph: Expression = NameExpr("_")
        run {
            val be = BinaryExpr(NameExpr(variable), ph, BinaryExpr.Operator.GREATER_EQUALS)
            val pattern = Pattern(be)
            pattern.addPlaceholder(ph, "min")
            val result = pattern.find(n)
            if (result != null) return result["min"] as Expression?
        }

        run {
            val be = BinaryExpr(NameExpr(variable), ph, BinaryExpr.Operator.GREATER)
            val pattern = Pattern(be)
            pattern.addPlaceholder(ph, "min")
            val result = pattern.find(n)
            if (result != null) {
                return BinaryExpr(
                result["min"] as Expression?,
                IntegerLiteralExpr("1"),
                BinaryExpr.Operator.PLUS
            )
            }
        }

        run {
            val be = BinaryExpr(ph, NameExpr(variable), BinaryExpr.Operator.LESS_EQUALS)
            val pattern = Pattern(be)
            pattern.addPlaceholder(ph, "min")
            val result = pattern.find(n)
            if (result != null) return result["min"] as Expression?
        }

        run {
            val be = BinaryExpr(ph, NameExpr(variable), BinaryExpr.Operator.LESS)
            val pattern = Pattern(be)
            pattern.addPlaceholder(ph, "min")
            val result = pattern.find(n)
            if (result != null) {
                return BinaryExpr(
                result["min"] as Expression?,
                IntegerLiteralExpr("1"),
                BinaryExpr.Operator.PLUS
            )
            }
        }

        return null
    }

    private inner class Jml2JavaVisitor : GenericVisitorAdapter<Expression, BlockStmt>() {
        override fun visit(n: JmlQuantifiedExpr, arg: BlockStmt): Expression {
            if (n.variables.size == 1) {
                val binding = n.variables[0]
                if (binding is JmlQuantifiedVariable && binding.iterable.isPresent) {
                    return visitElementQuantified(n, arg)
                }
            }
            if (n.binder === JmlQuantifiedExpr.JmlDefaultBinder.FORALL) return visitForall(n, arg)
            if (n.binder === JmlQuantifiedExpr.JmlDefaultBinder.EXISTS) return visitExists(n, arg)

            return when (n.binder) {
                JmlQuantifiedExpr.JmlDefaultBinder.SUM ->
                    visitClassicAccumulate(n, arg, "0", AssignExpr.Operator.PLUS)
                JmlQuantifiedExpr.JmlDefaultBinder.PRODUCT ->
                    visitClassicAccumulate(n, arg, "1", AssignExpr.Operator.MULTIPLY)
                JmlQuantifiedExpr.JmlDefaultBinder.NUM_OF -> visitClassicCount(n, arg)
                else -> throw IllegalArgumentException("Unsupport quantifier " + n.binder)
            }
        }

        /**
         * Element-domain quantification, desugared to a loop over the domain:
         * `(\forall int e : a; P)` becomes
         * `boolean result_N = true; for (int e : a) { boolean pred_M; { pred_M = <P>; } result_N &= pred_M; }`,
         * `(\sum int e : a; v)` accumulates `sum_N += v`, etc. A range domain
         * `lo..hi` becomes a plain index loop `for (int x = lo; x < hi; x++)`.
         */
        private fun visitElementQuantified(n: JmlQuantifiedExpr, arg: BlockStmt): Expression =
            when (n.binder) {
                JmlQuantifiedExpr.JmlDefaultBinder.FORALL -> visitElementForall(n, arg)
                JmlQuantifiedExpr.JmlDefaultBinder.EXISTS ->
                    UnaryExpr(visitElementForall(n, arg), UnaryExpr.Operator.LOGICAL_COMPLEMENT)
                JmlQuantifiedExpr.JmlDefaultBinder.SUM ->
                    visitElementAccumulate(n, arg, "0", AssignExpr.Operator.PLUS)
                JmlQuantifiedExpr.JmlDefaultBinder.PRODUCT ->
                    visitElementAccumulate(n, arg, "1", AssignExpr.Operator.MULTIPLY)
                JmlQuantifiedExpr.JmlDefaultBinder.NUM_OF -> visitElementCount(n, arg)
                else -> throw IllegalArgumentException("Unsupport quantifier " + n.binder)
            }

        private fun visitElementForall(n: JmlQuantifiedExpr, arg: BlockStmt): Expression {
            val rvalue = newSymbol("result_")
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(
                            PrimitiveType(PrimitiveType.Primitive.BOOLEAN), rvalue, BooleanLiteralExpr(true)
                        )
                    )
                )
            )
            val pred = Jml2JavaFacade.translate(predicatesOf(n))
            val predVar = newSymbol("pred_")
            val loopBody = BlockStmt()
            loopBody.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.BOOLEAN), predVar)
                    )
                )
            )
            pred.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(predVar), pred.b!!, AssignExpr.Operator.ASSIGN))
            )
            loopBody.addAndGetStatement(pred.a)
            loopBody.addAndGetStatement(
                ExpressionStmt(
                    AssignExpr(
                        NameExpr(rvalue),
                        BinaryExpr(NameExpr(rvalue), NameExpr(predVar), BinaryExpr.Operator.AND),
                        AssignExpr.Operator.ASSIGN
                    )
                )
            )
            emitElementLoop(n, arg, loopBody)
            return NameExpr(rvalue)
        }

        /** `(\sum int e : a; v)` / `(\product int e : a; v)` accumulate into an `int`. */
        private fun visitElementAccumulate(
            n: JmlQuantifiedExpr, arg: BlockStmt, init: String, op: AssignExpr.Operator
        ): Expression {
            val acc = newSymbol(if (op == AssignExpr.Operator.PLUS) "sum_" else "prod_")
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), acc, IntegerLiteralExpr(init))
                    )
                )
            )
            val value = Jml2JavaFacade.translate(findPredicate(n))
            val filters = filtersOf(n)
            val loopBody = BlockStmt()
            val assign = ExpressionStmt(AssignExpr(NameExpr(acc), value.b!!, op))
            loopBody.addAndGetStatement(value.a)
            if (filters != null) {
                loopBody.addAndGetStatement(IfStmt(filters, assign, null))
            } else {
                loopBody.addAndGetStatement(assign)
            }
            emitElementLoop(n, arg, loopBody)
            return NameExpr(acc)
        }

        /** `(\num_of int e : a; P)` counts the elements satisfying `P`. */
        private fun visitElementCount(n: JmlQuantifiedExpr, arg: BlockStmt): Expression {
            val acc = newSymbol("num_")
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), acc, IntegerLiteralExpr("0"))
                    )
                )
            )
            val pred = Jml2JavaFacade.translate(predicatesOf(n))
            val loopBody = BlockStmt()
            loopBody.addAndGetStatement(pred.a)
            loopBody.addAndGetStatement(
                IfStmt(
                    pred.b!!,
                    ExpressionStmt(UnaryExpr(NameExpr(acc), UnaryExpr.Operator.PREFIX_INCREMENT)),
                    null
                )
            )
            emitElementLoop(n, arg, loopBody)
            return NameExpr(acc)
        }

        /**
         * Appends to `arg` the loop iterating the domain of `n`'s single element
         * binding — enhanced-for over arrays/Iterables, plain index loop over a
         * range — with `loopBody` as its body. The domain (or the range endpoints)
         * is evaluated exactly once, before the loop.
         */
        private fun emitElementLoop(n: JmlQuantifiedExpr, arg: BlockStmt, loopBody: BlockStmt) {
            val variable = n.variables[0] as JmlQuantifiedVariable
            val iterable = variable.iterable.get()
            if (iterable is BinaryExpr && iterable.operator == BinaryExpr.Operator.RANGE) {
                val low = Jml2JavaFacade.translate(iterable.left)
                val high = Jml2JavaFacade.translate(iterable.right)
                for (prelude in listOf(low.a, high.a)) {
                    prelude?.let { arg.addAndGetStatement(it) }
                }
                val name = variable.nameAsString
                val loopType = if (variable.type is VarType) {
                    PrimitiveType(PrimitiveType.Primitive.INT)
                } else {
                    variable.type.clone()
                }
                val init = NodeList<Expression>()
                init.add(VariableDeclarationExpr(VariableDeclarator(loopType, name, low.b)))
                val update = NodeList<Expression>()
                update.add(UnaryExpr(NameExpr(name), UnaryExpr.Operator.POSTFIX_INCREMENT))
                arg.addAndGetStatement(
                    ForStmt(
                        init,
                        BinaryExpr(NameExpr(name), high.b!!, BinaryExpr.Operator.LESS),
                        update,
                        loopBody
                    )
                )
            } else {
                val domain = Jml2JavaFacade.translate(iterable)
                domain.a?.let { arg.addAndGetStatement(it) }
                arg.addAndGetStatement(
                    ForEachStmt(
                        VariableDeclarationExpr(
                            VariableDeclarator(variable.type.clone(), variable.nameAsString)
                        ),
                        domain.b!!,
                        loopBody
                    )
                )
            }
        }

        /** All filter expressions AND the final body/predicate of `n`. */
        private fun predicatesOf(n: JmlQuantifiedExpr): Expression {
            var pred: Expression = n.expressions[n.expressions.size - 1].clone()
            for (i in n.expressions.size - 2 downTo 0) {
                pred = BinaryExpr(n.expressions[i].clone(), pred, BinaryExpr.Operator.AND)
            }
            return pred
        }

        /** AND of the filter expressions, or null for a single-expression quantifier. */
        private fun filtersOf(n: JmlQuantifiedExpr): Expression? {
            var filters: Expression? = null
            for (i in 0 until n.expressions.size - 1) {
                val e = n.expressions[i].clone()
                filters = if (filters == null) e else BinaryExpr(filters, e, BinaryExpr.Operator.AND)
            }
            return filters
        }

        private fun visitForall(n: JmlQuantifiedExpr, arg: BlockStmt): Expression {
            assert(n.variables.size == 1)
            val rvalue = newSymbol("result_")
            val highVar = newSymbol("high_")
            val predVar = newSymbol("pred_")

            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(
                            PrimitiveType(PrimitiveType.Primitive.BOOLEAN),
                            rvalue, BooleanLiteralExpr(true)
                        )
                    )
                )
            )

            val variable = n.variables[0]
            val lowCode = Jml2JavaFacade.translate(findLowerBound(n, variable.nameAsString))
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(
                            variable.type.clone(), variable.nameAsString
                        )
                    )
                )
            )
            lowCode.a!!.addAndGetStatement(
                ExpressionStmt(
                    AssignExpr(
                        NameExpr(variable.nameAsString), lowCode.b,
                        AssignExpr.Operator.ASSIGN
                    )
                )
            )
            arg.addAndGetStatement(lowCode.a)

            val highCode = Jml2JavaFacade.translate(findUpperBound(n, variable.nameAsString))
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(
                            variable.type.clone(), highVar
                        )
                    )
                )
            )
            highCode.a!!.addAndGetStatement(
                ExpressionStmt(
                    AssignExpr(
                        NameExpr(highVar), highCode.b,
                        AssignExpr.Operator.ASSIGN
                    )
                )
            )
            arg.addAndGetStatement(highCode.a!!.clone())

            val predCode = Jml2JavaFacade.translate(findPredicate(n))

            val whileStmt = arg.addAndGetStatement(WhileStmt())
            val lessThanExpr = BinaryExpr(
                NameExpr(variable.nameAsString),
                NameExpr(highVar), BinaryExpr.Operator.LESS
            )
            whileStmt.setCondition(
                BinaryExpr(lessThanExpr, NameExpr(rvalue), BinaryExpr.Operator.AND)
            )
            val body = BlockStmt()

            body.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.BOOLEAN), predVar)
                    )
                )
            )
            predCode.a!!.addAndGetStatement(
                ExpressionStmt(
                    AssignExpr(
                        NameExpr(predVar), predCode.b,
                        AssignExpr.Operator.ASSIGN
                    )
                )
            )
            body.addAndGetStatement(predCode.a)

            val assignPred = ExpressionStmt(
                AssignExpr(
                    NameExpr(rvalue),
                    BooleanLiteralExpr(true), AssignExpr.Operator.ASSIGN
                )
            )
            body.addAndGetStatement(
                IfStmt(NameExpr(predVar), assignPred, null)
            )
            body.addAndGetStatement(highCode.a!!.clone())

            whileStmt.setBody(body)
            return NameExpr(rvalue)
        }

        private fun newSymbol(prefix: String): String = prefix + counter.getAndIncrement()

        private fun visitExists(n: JmlQuantifiedExpr, arg: BlockStmt): Expression = UnaryExpr(visitForall(n, arg), UnaryExpr.Operator.LOGICAL_COMPLEMENT)

        /**
         * Classic `(\sum int x; lo <= x && x < hi; v)` — accumulates over the
         * range described by the bound expressions. Mirrors {@link #visitForall}'s
         * loop, but with an `int` accumulator.
         */
        private fun visitClassicAccumulate(
            n: JmlQuantifiedExpr, arg: BlockStmt, init: String, op: AssignExpr.Operator
        ): Expression {
            assert(n.variables.size == 1)
            val acc = newSymbol(if (op == AssignExpr.Operator.PLUS) "sum_" else "prod_")
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), acc, IntegerLiteralExpr(init))
                    )
                )
            )
            val variable = n.variables[0]
            val highVar = newSymbol("high_")
            val low = Jml2JavaFacade.translate(findLowerBound(n, variable.nameAsString))
            val high = Jml2JavaFacade.translate(requireNotNull(findUpperBound(n, variable.nameAsString)))
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(VariableDeclarator(variable.type.clone(), variable.nameAsString))
                )
            )
            low.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(variable.nameAsString), low.b, AssignExpr.Operator.ASSIGN))
            )
            arg.addAndGetStatement(low.a)
            arg.addAndGetStatement(
                ExpressionStmt(VariableDeclarationExpr(VariableDeclarator(variable.type.clone(), highVar)))
            )
            high.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(highVar), high.b, AssignExpr.Operator.ASSIGN))
            )
            arg.addAndGetStatement(high.a!!.clone())

            val value = Jml2JavaFacade.translate(findPredicate(n))
            val loopBody = BlockStmt()
            loopBody.addAndGetStatement(value.a)
            loopBody.addAndGetStatement(ExpressionStmt(AssignExpr(NameExpr(acc), value.b!!, op)))

            val whileStmt = arg.addAndGetStatement(WhileStmt())
            whileStmt.setCondition(
                BinaryExpr(NameExpr(variable.nameAsString), NameExpr(highVar), BinaryExpr.Operator.LESS)
            )
            whileStmt.setBody(loopBody)
            return NameExpr(acc)
        }

        /**
         * Classic `(\num_of int x; lo <= x && x < hi; P)` — counts the elements
         * of the range satisfying `P`.
         */
        private fun visitClassicCount(n: JmlQuantifiedExpr, arg: BlockStmt): Expression {
            assert(n.variables.size == 1)
            val acc = newSymbol("num_")
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), acc, IntegerLiteralExpr("0"))
                    )
                )
            )
            val variable = n.variables[0]
            val highVar = newSymbol("high_")
            val low = Jml2JavaFacade.translate(findLowerBound(n, variable.nameAsString))
            val high = Jml2JavaFacade.translate(requireNotNull(findUpperBound(n, variable.nameAsString)))
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(VariableDeclarator(variable.type.clone(), variable.nameAsString))
                )
            )
            low.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(variable.nameAsString), low.b, AssignExpr.Operator.ASSIGN))
            )
            arg.addAndGetStatement(low.a)
            arg.addAndGetStatement(
                ExpressionStmt(VariableDeclarationExpr(VariableDeclarator(variable.type.clone(), highVar)))
            )
            high.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(highVar), high.b, AssignExpr.Operator.ASSIGN))
            )
            arg.addAndGetStatement(high.a!!.clone())

            val pred = Jml2JavaFacade.translate(predicatesOf(n))
            val predVar = newSymbol("pred_")
            val loopBody = BlockStmt()
            loopBody.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.BOOLEAN), predVar)
                    )
                )
            )
            pred.a!!.addAndGetStatement(
                ExpressionStmt(AssignExpr(NameExpr(predVar), pred.b!!, AssignExpr.Operator.ASSIGN))
            )
            loopBody.addAndGetStatement(pred.a)
            loopBody.addAndGetStatement(
                IfStmt(
                    NameExpr(predVar),
                    ExpressionStmt(UnaryExpr(NameExpr(acc), UnaryExpr.Operator.PREFIX_INCREMENT)),
                    null
                )
            )

            val whileStmt = arg.addAndGetStatement(WhileStmt())
            whileStmt.setCondition(
                BinaryExpr(NameExpr(variable.nameAsString), NameExpr(highVar), BinaryExpr.Operator.LESS)
            )
            whileStmt.setBody(loopBody)
            return NameExpr(acc)
        }

        override fun visit(n: JmlLetExpr, arg: BlockStmt): Expression {
            val inner = BlockStmt()
            val target = newTargetForAssignment()
            val type: ResolvedType = n.body.calculateResolvedType()
            arg.addAndGetStatement(
                ExpressionStmt(
                    VariableDeclarationExpr(
                        JMLUtils.resolvedType2Type(type),
                        target.asString()
                    )
                )
            )
            inner.addAndGetStatement(ExpressionStmt(n.variables))
            val e = accept(n.body, inner)
            arg.addAndGetStatement(inner)
            inner.addAndGetStatement(AssignExpr(NameExpr(target.asString()), e, AssignExpr.Operator.ASSIGN))
            return NameExpr(target.asString())
        }

        override fun visit(n: BinaryExpr, arg: BlockStmt): Expression {
            val left = accept(n.left, arg)
            val right = accept(n.right, arg)
            return when (n.operator) {
                BinaryExpr.Operator.IMPLICATION -> BinaryExpr(
                    UnaryExpr(
                        EnclosedExpr(left),
                        UnaryExpr.Operator.LOGICAL_COMPLEMENT
                    ),
                    right,
                    BinaryExpr.Operator.OR
                )

                BinaryExpr.Operator.RIMPLICATION -> BinaryExpr(
                    left,
                    UnaryExpr(
                        EnclosedExpr(right),
                        UnaryExpr.Operator.LOGICAL_COMPLEMENT
                    ),
                    BinaryExpr.Operator.OR
                )

                BinaryExpr.Operator.EQUIVALENCE -> BinaryExpr(
                    left,
                    right,
                    BinaryExpr.Operator.EQUALS
                )

                BinaryExpr.Operator.SUBTYPE, BinaryExpr.Operator.SUB_LOCK, BinaryExpr.Operator.SUB_LOCKE -> throw IllegalArgumentException(
                    "Unsupported operators."
                )

                else -> createAssignmentAndAdd(
                    BinaryExpr(left, right, n.operator),
                    arg
                )
            }
        }

        override fun visit(n: ArrayAccessExpr, arg: BlockStmt): Expression = super.visit(n, arg)

        override fun visit(n: ArrayCreationExpr, arg: BlockStmt): Expression = super.visit(n, arg)

        override fun visit(n: ArrayInitializerExpr, arg: BlockStmt): Expression = super.visit(n, arg)

        override fun visit(n: AssignExpr, arg: BlockStmt): Expression = super.visit(n, arg)
    }

    companion object {
        fun findBound(n: JmlQuantifiedExpr): Expression {
            if (n.expressions.size == 2) {
                return n.expressions[0]
            } else {
                if (n.expressions.size == 1 && n.expressions.first() is BinaryExpr) {
                    return n.expressions.first()
                }
            }
            throw IllegalArgumentException("Could not determine bound.")
        }

        fun findLowerBound(n: JmlQuantifiedExpr, variable: String): Expression? {
            if (n.expressions.size == 3) return n.expressions[0]

            val e = findBound(n)

            if (e is JmlMultiCompareExpr) {
                if (e.expressions.size == 3 &&
                    (e.expressions[1] as? NameExpr)?.nameAsString == variable
                ) {
                    if (e.operators[0] == BinaryExpr.Operator.LESS_EQUALS) return e.expressions[0]
                    if (e.operators[0] == BinaryExpr.Operator.LESS) {
                        return BinaryExpr(
                        e.expressions[0], IntegerLiteralExpr("1"), BinaryExpr.Operator.PLUS
                    )
                    }
                    throw IllegalStateException()
                }
            }

            val ph: Expression = NameExpr("_")
            run {
                val be = BinaryExpr(NameExpr(variable), ph, BinaryExpr.Operator.GREATER_EQUALS)
                val pattern = Pattern(be)
                pattern.addPlaceholder(ph, "min")
                val result = pattern.find(n)
                if (result != null) return result["min"] as Expression?
            }

            run {
                val be = BinaryExpr(NameExpr(variable), ph, BinaryExpr.Operator.GREATER)
                val pattern = Pattern(be)
                pattern.addPlaceholder(ph, "min")
                val result = pattern.find(n)
                if (result != null) {
                    return BinaryExpr(
                    result["min"] as Expression?,
                    IntegerLiteralExpr("1"),
                    BinaryExpr.Operator.PLUS
                )
                }
            }

            run {
                val be = BinaryExpr(ph, NameExpr(variable), BinaryExpr.Operator.LESS_EQUALS)
                val pattern = Pattern(be)
                pattern.addPlaceholder(ph, "min")
                val result = pattern.find(n)
                if (result != null) return result["min"] as Expression?
            }

            run {
                val be = BinaryExpr(ph, NameExpr(variable), BinaryExpr.Operator.LESS)
                val pattern = Pattern(be)
                pattern.addPlaceholder(ph, "min")
                val result = pattern.find(n)
                if (result != null) {
                    return BinaryExpr(
                    result["min"] as Expression?,
                    IntegerLiteralExpr("1"),
                    BinaryExpr.Operator.PLUS
                )
                }
            }

            return null
        }
    }
}
