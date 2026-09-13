/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.expr.BinaryExpr.Operator.*
import com.github.javaparser.ast.jml.*
import com.github.javaparser.ast.jml.body.*
import com.github.javaparser.ast.jml.clauses.*
import com.github.javaparser.ast.jml.expr.*
import com.github.javaparser.ast.jml.type.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.GenericVisitorAdapter
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable
import jjbmc.jml2java.Jml2JavaFacade.resolvedType2Type
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Translates JML expressions into a bunch of Java statements and a final expression.
 *
 * @author Alexander Weigl
 * @version 1 (04.10.22)
 */
class Jml2JavaExpressionTranslator {
    private val maxArraySize = 0

    fun accept(e: Expression, arg: TranslationMode): Jml2JavaFacade.Result {
        if (Jml2JavaFacade.containsJmlExpression(e)) {
            return e.accept(Jml2JavaExpressionTranslator.Jml2JavaVisitor(), arg)
        }
        return Jml2JavaFacade.Result(e)
    }

    private fun createAssignmentFor(e: Expression?): Statement {
        val decl =
            VariableDeclarationExpr(VariableDeclarator(VarType(), newTargetForAssignment(), e))
        decl.addModifier(Modifier.DefaultKeyword.FINAL)
        return ExpressionStmt(decl)
    }

    private val targetForAssignment: SimpleName
        get() = SimpleName("_gen_" + counter.get())

    private fun newTargetForAssignment(): SimpleName {
        counter.getAndIncrement()
        return this.targetForAssignment
    }

    fun findPredicate(n: JmlQuantifiedExpr): Expression = n.expressions[n.expressions.size - 1]

    private inner class Jml2JavaVisitor : GenericVisitorAdapter<Jml2JavaFacade.Result, TranslationMode>() {
        var quantifierSplitter = QuantifierSplitter
        private val replaceStack: MutableMap<String?, String?> = TreeMap<String?, String?>()

        override fun visit(n: ConditionalExpr, arg: TranslationMode): Jml2JavaFacade.Result = super.visit(n, arg)

        override fun visit(n: JmlQuantifiedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            if (n.binder === JmlQuantifiedExpr.JmlDefaultBinder.FORALL) {
                return if (arg == TranslationMode.ASSERT) {
                    visitForall(
                n,
                arg
            )
                } else {
                    visitForallLoop(n, arg)
                }
            }
            if (n.binder === JmlQuantifiedExpr.JmlDefaultBinder.EXISTS) {
                return if (arg == TranslationMode.ASSUME) {
                    visitExists(
                n,
                arg
            )
                } else {
                    visitExistsLoop(n, arg)
                }
            }

            throw IllegalArgumentException("Unsupported quantifier " + n.binder)
        }

        /**
         * Translate a universal quantification into a for loop over the range
         */
        fun visitForallLoop(n: JmlQuantifiedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val b = BlockStmt()
            b.setParentNode(n.parentNodeForChildren)
            val boolVar = "b" + counter.getAndIncrement()
            val loopVar = "i" + counter.getAndIncrement()

            // final var loopVar = n.getVariables().get(0).getNameAsString();
            val boundedVar = n.variables[0].nameAsString
            replaceStack.put(boundedVar, loopVar)

            val lowerBoundRes = QuantifierSplitter.getLowerBound(n).accept(this, arg)
            val upperBoundRes = QuantifierSplitter.getUpperBound(n).accept(this, arg)
            val lowerBound = lowerBoundRes.value
            val upperBound = upperBoundRes.value
            b.statements.addAll(lowerBoundRes.statements)
            b.statements.addAll(upperBoundRes.statements)

            // add: boolean bN = true
            val varDefs =
                NodeList(
                    ExpressionStmt(
                        VariableDeclarationExpr(
                            VariableDeclarator(
                                PrimitiveType(PrimitiveType.Primitive.BOOLEAN),
                                boolVar,
                                BooleanLiteralExpr(true)
                            )
                        )
                    )
                )

            //
            val init = VariableDeclarationExpr(
                VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), loopVar, lowerBound)
            )
            val compare = BinaryExpr(NameExpr(loopVar), upperBound, LESS)
            val update = UnaryExpr(NameExpr(loopVar), UnaryExpr.Operator.PREFIX_INCREMENT)
            var forBody = BlockStmt()

            val clone = n.expressions.last().clone()
            val res = clone.accept(this, arg)
            res.value = res.value.accept(
                ReplaceVariable(n.variables[0], init.getVariable(0).nameAsString),
                null
            ) as Expression?
            forBody.statements.addAll(res.statements)
            varDefs.addAll(res.necessaryVars)

            // boolVar = (boolVar && val)
            forBody.addStatement(
                AssignExpr(
                    NameExpr(boolVar),
                    EnclosedExpr(BinaryExpr(NameExpr(boolVar), res.value, AND)),
                    AssignExpr.Operator.ASSIGN
                )
            )

            forBody = rename(forBody, replaceStack)
            replaceStack.remove(boundedVar)

            b.addStatement(ForStmt(NodeList(init), compare, NodeList(update), forBody))
            return Jml2JavaFacade.Result(b.statements, NameExpr(boolVar), varDefs)
        }

        /**
         * Translate a universal quantification into a for loop over the range
         */
        fun visitExistsLoop(n: JmlQuantifiedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val b = BlockStmt()
            b.setParentNode(n.parentNodeForChildren)
            val boolVar = "b" + counter.getAndIncrement()
            val loopVar = "i" + counter.getAndIncrement()
            val lowerBound = QuantifierSplitter.getLowerBound(n)
            val upperBound = QuantifierSplitter.getUpperBound(n)
            // add: boolean bN = false
            val varDefs: NodeList<Statement?> =
                NodeList(
                    ExpressionStmt(
                        VariableDeclarationExpr(
                            VariableDeclarator(
                                PrimitiveType(PrimitiveType.Primitive.BOOLEAN),
                                boolVar,
                                BooleanLiteralExpr(false)
                            )
                        )
                    )
                )

            //
            val init = VariableDeclarationExpr(
                VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), loopVar, lowerBound)
            )
            val compare = BinaryExpr(NameExpr(loopVar), upperBound, LESS)
            val update = UnaryExpr(NameExpr(loopVar), UnaryExpr.Operator.PREFIX_INCREMENT)
            val forBody = BlockStmt()

            val clone = n.expressions.last().clone()
            val res = clone.accept(this, arg)
            forBody.statements.addAll(res.statements)
            res.value = res.value.accept(
                ReplaceVariable(
                    n.variables[0], init.getVariable(0).nameAsString
                ),
                null
            ) as Expression?
            varDefs.addAll(res.necessaryVars)

            // boolVar = (boolVar || val)
            forBody.addStatement(
                AssignExpr(
                    NameExpr(boolVar),
                    EnclosedExpr(BinaryExpr(NameExpr(boolVar), res.value, OR)),
                    AssignExpr.Operator.ASSIGN
                )
            )

            b.addStatement(ForStmt(NodeList(init), compare, NodeList(update), forBody))
            return Jml2JavaFacade.Result(b.statements, NameExpr(boolVar), varDefs)
        }

        fun visitForall(n: JmlQuantifiedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            var n: JmlQuantifiedExpr = n
            n = n.clone()
            val para = QuantifierSplitter.getVariable(n)
            val s: Statement = assignNondet(para)
            val newExpr = BinaryExpr(
                EnclosedExpr(n.expressions[0]),
                EnclosedExpr(n.expressions[1]),
                IMPLICATION
            )
                .setParentNode(n)
                .accept(this, arg)
            newExpr.statements.addFirst(s)
            return newExpr
        }

        fun newSymbol(prefix: String): String = prefix + counter.getAndIncrement()

        fun visitExists(n: JmlQuantifiedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val para = QuantifierSplitter.getVariable(n)
            // var lowerBoundO = quantifierSplitter.getLowerBound(n);
            // var upperBoundO= quantifierSplitter.getUpperBound(n);
            val s: Statement = assignNondet(para)
            val newExpr = BinaryExpr(
                n.expressions[0], n.expressions[1], AND
            )
                .accept(this, arg)
            newExpr.statements.addFirst(s)
            return newExpr
        }

        fun assignNondet(para: Parameter): Statement = ExpressionStmt(
                VariableDeclarationExpr(
                    VariableDeclarator(
                        para.type, para.nameAsString, MethodCallExpr("CProver.nondetInt")
                    )
                )
            )

        /**
         * `<pre>
         * (\let x = expr1; expr2)
        </pre>` *
         *
         * `<pre>
         * { var new; { val(expr1); x = v; eval(expr2); new = v; ) } ; new
        </pre>` *
         * * @param n
         *
         * @param arg
         * @return
         */
        override fun visit(n: JmlLetExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner = BlockStmt()
            val outer = BlockStmt()

            val target: SimpleName = newTargetForAssignment()
            val type =
                n.body.calculateResolvedType()
            outer.addAndGetStatement(
                ExpressionStmt(VariableDeclarationExpr(resolvedType2Type(type), target.asString()))
            )
            outer.addStatement(inner)

            for (variable in n.variables.variables) {
                val v = accept(variable.initializer.get(), arg)
                inner.statements.addAll(v.statements)
                inner.addAndGetStatement(declareAndAssign(variable, v.value))
            }
            val body = accept(n.body, arg)
            inner.statements.addAll(body.statements)
            inner.addAndGetStatement(
                AssignExpr(NameExpr(target.asString()), body.value, AssignExpr.Operator.ASSIGN)
            )
            return Jml2JavaFacade.Result(outer.statements, NameExpr(target.asString()))
        }

        fun declareAndAssign(variable: VariableDeclarator, value: Expression?): Statement = ExpressionStmt(
                VariableDeclarationExpr(VariableDeclarator(variable.type, variable.name, value))
            )

        override fun visit(n: BinaryExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val left = accept(n.left, arg)
            val right = accept(n.right, arg)
            val res =
                when (n.operator) {
                    AND -> combine(
                        left.statements,
                        ifThen(left.value, right.statements),
                        BinaryExpr(left.value, right.value, AND)
                    )

                    OR -> combine(
                        left.statements,
                        ifThen(negate(left.value), right.statements),
                        BinaryExpr(left.value, right.value, OR)
                    )

                    IMPLICATION -> combine(
                        left.statements,
                        ifThen(left.value, right.statements),
                        BinaryExpr(negate(left.value), right.value, OR)
                    )

                    RIMPLICATION -> combine(
                        right.statements,
                        ifThen(right.value, left.statements),
                        BinaryExpr(negate(right.value), left.value, OR)
                    )

                    EQUIVALENCE -> combine(
                        left,
                        right,
                        BinaryExpr(left.value, right.value, EQUALS)
                    )

                    SUBTYPE, SUB_LOCK, SUB_LOCKE -> throw IllegalArgumentException("Unsupported operators.")

                    else -> combine(left, right, BinaryExpr(left.value, right.value, n.operator))
                }
            res.necessaryVars = left.necessaryVars
            res.necessaryVars.addAll(right.necessaryVars)
            return res
        }

        fun combine(
            left: Jml2JavaFacade.Result,
            right: Jml2JavaFacade.Result,
            expr: BinaryExpr
        ): Jml2JavaFacade.Result {
            val n = NodeList(left.statements)
            n.addAll(right.statements)
            val n1 = NodeList(left.necessaryVars)
            n1.addAll(right.necessaryVars)
            return Jml2JavaFacade.Result(n, expr, n1)
        }

        fun negate(value: Expression?): Expression = UnaryExpr(value, UnaryExpr.Operator.LOGICAL_COMPLEMENT)

        fun combine(before: NodeList<Statement>, combination: Statement, expr: BinaryExpr): Jml2JavaFacade.Result {
            val n = NodeList(before)
            n.add(combination)
            return Jml2JavaFacade.Result(n, expr)
        }

        fun ifThen(value: Expression, statements: NodeList<Statement>): IfStmt = IfStmt(value, BlockStmt(statements), null)

        override fun visit(n: ArrayAccessExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val name =
                n.name.accept(this, arg)
            val index =
                n.index.accept(this, arg)
            name.statements.addAll(index.statements)
            return Jml2JavaFacade.Result(
                name.statements,
                ArrayAccessExpr(name.value, index.value),
                name.necessaryVars
            )
        }

        override fun visit(n: ArrayCreationExpr, arg: TranslationMode): Jml2JavaFacade.Result = super.visit(n, arg)

        override fun visit(n: ArrayInitializerExpr, arg: TranslationMode): Jml2JavaFacade.Result = super.visit(n, arg)

        override fun visit(n: AssignExpr, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Assignments are forbidden in JML.")

        override fun visit(n: ClassExpr, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Assignments are forbidden in JML.")

        override fun visit(n: BooleanLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: CastExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner =
                n.expression.accept(this, arg)
            return Jml2JavaFacade.Result(inner.statements, CastExpr(n.type, inner.value), inner.necessaryVars)
        }

        override fun visit(n: CharLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: DoubleLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: EnclosedExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner =
                n.inner.accept(this, arg)
            return Jml2JavaFacade.Result(inner.statements, EnclosedExpr(inner.value), inner.necessaryVars)
        }

        override fun visit(n: FieldAccessExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner =
                n.scope.accept(this, arg)
            return Jml2JavaFacade.Result(
                inner.statements,
                FieldAccessExpr(
                    inner.value, n.typeArguments.orElse(null), SimpleName(n.nameAsString)
                ),
                inner.necessaryVars
            )
        }

        override fun visit(n: InstanceOfExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner =
                n.expression.accept(this, arg)
            return Jml2JavaFacade.Result(
                inner.statements,
                InstanceOfExpr(inner.value, n.type),
                inner.necessaryVars
            )
        }

        override fun visit(n: IntegerLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: LongLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: MarkerAnnotationExpr?, arg: TranslationMode): Jml2JavaFacade.Result = super.visit(n, arg)

        override fun visit(n: MethodCallExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            if (n.nameAsString.equals("\\old")) {
                var expr: Expression = NameExpr("old_" + Math.abs(n.getArgument(0).hashCode()))
                val relevantQuantifiers = Jml2JavaFacade.getRelevantQuantifiers(n.getArgument(0))
                for (q in relevantQuantifiers) {
                    expr = ArrayAccessExpr(
                        expr,
                        BinaryExpr(
                            QuantifierSplitter.getVariable(q).nameAsExpression,
                            IntegerLiteralExpr(maxArraySize.toString()),
                            REMAINDER
                        )
                    )
                }
                return Jml2JavaFacade.Result(expr)
            }

            var scope: Expression? = null
            val statements: NodeList<Statement> = NodeList()

            if (n.scope.isPresent) {
                val s =
                    n.scope.get().accept(this, arg)
                scope = s.value
                statements.addAll(s.statements)
            }

            val args: NodeList<Expression?> = NodeList<Expression?>()
            for (argument in n.arguments) {
                val a =
                    argument.accept(this, arg)
                statements.addAll(a.statements)
                args.add(a.value)
            }
            return Jml2JavaFacade.Result(
                statements,
                MethodCallExpr(scope, n.typeArguments.orElse(null), n.nameAsString, args)
            )
        }

        override fun visit(n: NameExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            if (n.name.toString().equals("\\result")) {
                return Jml2JavaFacade.Result(NameExpr(EmbeddContracts.RESULTVAR))
            }
            return Jml2JavaFacade.Result(n)
        }

        override fun visit(n: NullLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: ObjectCreationExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: SingleMemberAnnotationExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: StringLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: SuperExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: ThisExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: UnaryExpr, arg: TranslationMode): Jml2JavaFacade.Result {
            val inner =
                n.expression.accept(this, arg.switchPolarity())
            return Jml2JavaFacade.Result(inner.statements, UnaryExpr(inner.value, n.operator), inner.necessaryVars)
        }

        override fun visit(n: VariableDeclarationExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: LambdaExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: MethodReferenceExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: TypeExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("Object creation not allowed")

        override fun visit(n: SwitchExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("SwitchExpr not allowed")

        override fun visit(n: TextBlockLiteralExpr, arg: TranslationMode): Jml2JavaFacade.Result = Jml2JavaFacade.Result(n)

        override fun visit(n: TypePatternExpr, arg: TranslationMode): Jml2JavaFacade.Result? = Jml2JavaFacade.Result(n)

        override fun visit(n: JmlLabelExpr, arg: TranslationMode): Jml2JavaFacade.Result? {
            val inner =
                n.expression.accept(this, arg)
            // TODO weigl maybe assign a name to the expression
            return inner
        }

        override fun visit(n: JmlMultiCompareExpr?, arg: TranslationMode): Jml2JavaFacade.Result = unroll(n).accept(this, arg)

        override fun visit(n: JmlBinaryInfixExpr?, arg: TranslationMode): Jml2JavaFacade.Result? = throw IllegalStateException("not allowed")
    }

    companion object {
        val counter: AtomicInteger = AtomicInteger()
        fun findBound(n: JmlQuantifiedExpr): Expression {
            if (n.expressions.size == 2) {
                return n.expressions[0]
            } else if (n.expressions.size == 1) if (n.expressions[0] is BinaryExpr) return be.getLeft()
            throw IllegalArgumentException("Could not determine bound.")
        }

        private fun <T : Node> rename(forBody: T, replaceStack: Map<String, String>): T {
            return forBody.accept(
                object : ModifierVisitor<Any>() {
                    override fun visit(n: NameExpr, arg: Any): Visitable {
                        if (replaceStack.containsKey(n.nameAsString)) {
                            return NameExpr(replaceStack[n.nameAsString])
                        }
                        return n
                    }
                },
                Any()
            ) as T
        }
    }
}
