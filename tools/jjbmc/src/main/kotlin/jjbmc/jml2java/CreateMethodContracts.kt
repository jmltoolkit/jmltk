/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.jml.clauses.JmlClauseKind
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import jjbmc.JJBMCOptions
import org.jspecify.annotations.Nullable

/**
 * @author Alexander Weigl
 * @version 1 (06.05.23)
 */
class CreateMethodContracts(private val maxArraySize: Int) : VoidVisitorAdapter<@Nullable Any?>() {
    var last: TypeDeclaration<*>? = null

    constructor(options: JJBMCOptions) : this(options.getMaxArraySize())

    override fun visit(n: ClassOrInterfaceDeclaration, arg: Any?) {
        last = n

        // Make a copy to avoid concurrent modification exception as new methods are created
        val newMembers: NodeList<BodyDeclaration<*>> = NodeList()
        for (bd in n.members) {
            newMembers.add(bd.clone())
        }
        n.setMembers(newMembers)
    }

    override fun visit(n: MethodDeclaration, arg: Any?) {
        val contracts = n.contracts
        // Only one contract currently supported
        if (contracts.isEmpty()) {
            return
        }
        if (contracts.isEmpty() || contracts.size != 1) {
            throw IllegalStateException(
                "The number of contracts is " + contracts.size + " only 1 contract supported for method: " +
                    n.nameAsString
            )
        }

        val contract = contracts.first()
        if (EmbeddContracts.containsInvalidClauses(contract)) {
            throw IllegalStateException("Found invalid clause in: $contract")
        }

        val ensures = EmbeddContracts.gatherAnd(contract, JmlClauseKind.ENSURES)
        val requires = EmbeddContracts.gatherAnd(contract, JmlClauseKind.REQUIRES)
        val assignable = EmbeddContracts.gather(contract, JmlClauseKind.ASSIGNABLE)

        val mContract = n.clone()
        mContract.setParentNode(n)
        mContract.setName(n.nameAsString + "Contract")
        mContract.addModifier(Modifier.DefaultKeyword.FINAL)
        val body = mContract.body.get()
        body.statements.clear()

        body.addStatement(Jml2JavaFacade.assert_(requires))
        if (!n.type.isVoidType) {
            val returnVarExpr = VariableDeclarationExpr(n.type, EmbeddContracts.RESULTVAR)
            val st: Statement = ExpressionStmt(returnVarExpr)
            body.statements.add(st)
            body.addStatement(Jml2JavaFacade.havoc(returnVarExpr.asVariableDeclarationExpr()))
        }
        // save references to old variables
        Jml2JavaFacade.storeOlds(ensures, maxArraySize).forEach(body::addStatement)

        for (expression in assignable) {
            body.addStatement(Jml2JavaFacade.havoc(expression))
        }

        body.addStatement(Jml2JavaFacade.assume(ensures))

        if (!n.type.isVoidType) {
            body.addStatement(ReturnStmt(NameExpr(EmbeddContracts.RESULTVAR)))
        }

        mContract.addAnnotation(Jml2JavaFacade.createGeneratedAnnotation())

        mContract.contracts.clear()

        last!!.addMember(mContract)
    }
}
