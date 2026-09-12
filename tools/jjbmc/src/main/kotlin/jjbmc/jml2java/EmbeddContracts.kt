/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.jml.clauses.JmlContract
import com.github.javaparser.ast.jml.clauses.JmlMultiExprClause
import com.github.javaparser.ast.jml.clauses.JmlSimpleExprClause
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable
import jjbmc.JJBMCOptions
import org.jspecify.annotations.Nullable
import java.util.*

/**
 * @author Alexander Weigl
 * @version 1 (06.05.23)
 */
class EmbeddContracts(
    private val forceInliningMethods: Boolean,
    private val maxArraySize: Int
) : ModifierVisitor<@Nullable Any?>() {

    private var foundReturn = false

    constructor(options: JJBMCOptions) : this(options.forceInliningMethods, options.getMaxArraySize())

    override fun visit(n: MethodDeclaration, arg: Any?): Visitable {
        if (!Jml2JavaFacade.ignoreNodeByAnnotation(n)) {
            val contracts = n.contracts
            var ensures: Expression = BooleanLiteralExpr(true)
            var requires: Expression = BooleanLiteralExpr(true)
            var assignable: List<Expression> = ArrayList()
            var sigOnly: List<Expression> = ArrayList()
            // Only one contract currently supported
            if (contracts.size > 1) {
                return n
            }

            if (n.parentNode.isPresent) {
                val copy = n.clone()
                val parentClass = n.parentNode.get() as ClassOrInterfaceDeclaration
                copy.contracts.clear()
                parentClass.addMember(copy)
            }

            if (!contracts.isEmpty()) {
                val contract = contracts.first()

                assert(!containsInvalidClauses(contract))

                ensures = gatherAnd(contract, JmlClauseKind.ENSURES)
                ensures.setParentNode(contract)
                requires = gatherAnd(contract, JmlClauseKind.REQUIRES)
                requires.setParentNode(contract)
                assignable = gather(contract, JmlClauseKind.ASSIGNABLE)
                assignable.forEach { a -> a.setParentNode(contract) }
                sigOnly = gather(contract, JmlClauseKind.SIGNALS_ONLY)
                sigOnly.forEach { a -> a.setParentNode(contract) }
            }

            if (assignable.isEmpty()) {
                assignable = Collections.singletonList(NameExpr("\\everything"))
            }
            contracts.clear() // delete the contract

            Jml2JavaFacade.currentNode = n
            n.setBody(constructMethodBody(n, ensures, requires, assignable, sigOnly))
            n.setName(n.nameAsString + "Verification")
            n.addAnnotation(Jml2JavaFacade.createGeneratedAnnotation())
        }
        return n
    }

    private fun constructMethodBody(
        method: MethodDeclaration,
        ensures: Expression,
        requires: Expression,
        assignable: List<Expression>,
        sigOnly: List<Expression>
    ): BlockStmt {
        val block = BlockStmt()
        block.setParentNode(method)
        if (!method.type.isVoidType) {
            // create return value
            val returnVarExpr: Expression = declareVariable(method.type, RESULTVAR)
            val st: Statement = ExpressionStmt(returnVarExpr)
            st.setParentNode(method)
            block.statements.add(st)
            block.addStatement(Jml2JavaFacade.havoc(returnVarExpr.asVariableDeclarationExpr()))
        }

        // assume pre-condition
        block.addStatement(Jml2JavaFacade.assume(requires))

        // save references to old variables
        Jml2JavaFacade.storeOlds(ensures, maxArraySize).forEach(block::addStatement)

        foundReturn = false
        var body = method.body.get().accept(this, null) as BlockStmt

        if (!foundReturn && sigOnly.isEmpty()) {
            block.addStatement(body)
        } else {
            // build try-statement
            val bodyTry = TryStmt(body, NodeList(), null)
            val excBody = BlockStmt()
            if (foundReturn) {
                val excParam = Parameter(RETURN_EXCEPTION_TYPE, RETURN_EXCEPTION_NAME)
                val returnCatchClause = CatchClause(excParam, excBody)
                bodyTry.catchClauses.add(returnCatchClause)
            }
            for (sigOnlyClause in sigOnly) {
                bodyTry.catchClauses
                    .add(
                        CatchClause(
                        Parameter(ClassOrInterfaceType().setName(sigOnlyClause.toString()), "exc"),
                        excBody
                    )
                    )
            }
            block.addStatement(bodyTry)
        }

        // assert the post-condition
        block.addStatement(Jml2JavaFacade.assert_(ensures))

        if (!method.type.isVoidType) {
            // return stored result
            block.addStatement(ReturnStmt(NameExpr(RESULTVAR)))
        }
        return block
    }

    override fun visit(n: WhileStmt, arg: Any?): Visitable {
        if (n.contracts.size == 1) {
            val contract = n.contracts.first()
            val loopInvar: List<Expression> = gather(contract, JmlClauseKind.LOOP_INVARIANT)
            val assignables: List<Expression> = gather(contract, JmlClauseKind.ASSIGNABLE)
            val decreases: List<Expression> = gather(contract, JmlClauseKind.DECREASES)

            if (decreases.size != 1) {
                throw IllegalStateException(
                    "Only exactly one decreases clause supported. However found " +
                    decreases.size + " in " + n.contracts
                )
            }
            return handleLoop(
                loopInvar, assignables, decreases.get(0), n.condition, n.body, NodeList(), n
            )
        }
        return super.visit(n, arg)
    }

    override fun visit(n: ForStmt, arg: Any?): Visitable {
        if (n.contracts.size == 1) {

            var body = ensureBlock(n.body.clone())
            body.setParentNode(n)
            for (expression in n.update) {
                body.addStatement(expression)
            }

            val contract = n.contracts.first()
            val loopInvar: List<Expression> = gather(contract, JmlClauseKind.LOOP_INVARIANT)
            val assignables: List<Expression> = gather(contract, JmlClauseKind.ASSIGNABLE)
            val decreases: List<Expression> = gather(contract, JmlClauseKind.DECREASES)

            if (decreases.size != 1) {
                throw IllegalStateException("Too many decreases clauses")
            }
            val res = handleLoop(
                loopInvar,
                assignables,
                decreases.get(0),
                n.compare.orElse(BooleanLiteralExpr(true)),
                body,
                n.initialization,
                n
            )
            res.setParentNode(n)
            return res
        }
        return n
    }

    fun handleLoop(
        loopInvars: List<Expression>,
        assignables: List<Expression>,
        decreases: Expression,
        loopCondition: Expression,
        body: Statement,
        inits: List<Expression>,
        parent: Node
    ): BlockStmt {
        var block = BlockStmt()
        block.setParentNode(parent)
        for (e in inits) {
            block.addStatement(ExpressionStmt(e))
        }
        val oldD = "oldD" + Jml2JavaExpressionTranslator.counter.getAndIncrement()
        block.addStatement(
            VariableDeclarationExpr(
            VariableDeclarator(PrimitiveType(PrimitiveType.Primitive.INT), oldD, decreases.clone())
            )
        )

        for (loopInvar in loopInvars) {
            block.addStatement(Jml2JavaFacade.assert_(loopInvar.clone()))
        }
        for (assignable in assignables) {
            block.addStatement(Jml2JavaFacade.havoc(assignable))
        }

        var thenBlock = BlockStmt()
        val ifThen = IfStmt(loopCondition.clone(), thenBlock, null)
        ifThen.setParentNode(block)
        thenBlock.addStatement(body.accept(this, null) as Statement)
        for (loopInvar in loopInvars) {
            thenBlock.addStatement(Jml2JavaFacade.assert_(loopInvar).clone())
        }
        if (decreases != null) {
            thenBlock.addStatement(
                Jml2JavaFacade.assertStatement(
                    BinaryExpr(
                BinaryExpr(decreases.clone(), NameExpr(oldD), BinaryExpr.Operator.LESS),
                BinaryExpr(IntegerLiteralExpr("0"), decreases.clone(), BinaryExpr.Operator.LESS_EQUALS),
                BinaryExpr.Operator.AND
                    )
                )
            )
        }
        thenBlock.addStatement(Jml2JavaFacade.assumeStatement(BooleanLiteralExpr(false)))
        for (loopInvar in loopInvars) {
            block.addStatement(Jml2JavaFacade.assume(loopInvar).clone())
        }
        block.addStatement(ifThen)
        return block
    }

    private fun ensureBlock(clone: Statement): BlockStmt {
        if (clone is BlockStmt) return clone
        var b = BlockStmt()
        b.addStatement(clone)
        return b
    }

    private fun declareVariable(type: Type, name: String): Expression = VariableDeclarationExpr(type, name)

    override fun visit(n: MethodCallExpr, arg: Any?): Visitable {
        if (forceInliningMethods) {
            return super.visit(n, arg)
        }
        val arguments: ArrayList<Expression> = ArrayList()
        for (argument in n.arguments) {
            arguments.add(argument.accept(this, arg) as Expression)
        }
        val contractCall = MethodCallExpr(
            n.name.toString() + "Contract", n.arguments.toTypedArray()
        )
        return contractCall
    }

    override fun visit(n: ReturnStmt, arg: Any?): Visitable {
        foundReturn = true
        var block = BlockStmt()
        block.setParentNode(n.parentNodeForChildren)
        if (n.expression.isPresent) {
            val returnVal: Expression = n.expression.get().accept(this, arg) as Expression
            block.addStatement(AssignExpr(NameExpr(RESULTVAR), returnVal, AssignExpr.Operator.ASSIGN))
        }
        block.addStatement(
            ThrowStmt(
                ObjectCreationExpr(
            null, ClassOrInterfaceType().setName(RETURN_EXCEPTION_TYPE.asString()), NodeList()
                )
            )
        )
        return block
    }

    companion object {
        const val RESULTVAR: String = "__RESULT__"
        private val RETURN_EXCEPTION_TYPE: Type = ClassOrInterfaceType().setName("ReturnException")
        private const val RETURN_EXCEPTION_NAME: String = "returnExc"

        fun gatherAnd(contract: JmlContract, jmlClauseKind: JmlClauseKind): Expression {
            val all = gather(contract, jmlClauseKind)
            if (all.size == 1) {
                return all.first()
            }

            if (all.isEmpty()) {
                return BooleanLiteralExpr(true)
            }

            var res: Expression = all.removeFirst()
            while (!all.isEmpty()) {
                res = BinaryExpr(res, all.removeFirst(), BinaryExpr.Operator.AND)
            }
            res.setParentNode(contract)
            return res
        }

        fun gather(contract: JmlContract, jmlClauseKind: JmlClauseKind): List<Expression> {
            val seq: LinkedList<Expression> = LinkedList()
            for (clause in contract.clauses) {
                if (clause.getKind() == jmlClauseKind) {
                    if (clause is JmlSimpleExprClause) {
                        seq.add(clause.expression)
                    } else if (clause is JmlMultiExprClause) {
                        seq.addAll(clause.expression)
                    }
                }
            }
            return seq
        }

        fun containsInvalidClauses(contract: JmlContract): Boolean {
            for (clause in contract.clauses) {
                when (clause.getKind()) {
                    JmlClauseKind.ASSIGNABLE, JmlClauseKind.REQUIRES, JmlClauseKind.ENSURES,
                    JmlClauseKind.SIGNALS_ONLY -> {}

                    else -> return true
                }
            }
            return false
        }
    }
}
