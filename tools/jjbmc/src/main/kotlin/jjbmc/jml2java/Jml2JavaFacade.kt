/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.jml2java

import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.expr.JmlMultiCompareExpr
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.ModifierVisitor
import com.github.javaparser.ast.visitor.Visitable
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.*
import com.github.javaparser.resolution.types.ResolvedType
import jjbmc.JJBMCOptions
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Transformation of JML expressions into equivalent Java code.
 *
 * @author Alexander Weigl
 * @version 1 (04.10.22)
 */
object Jml2JavaFacade {
    var currentNode: Node? = null

    fun assumeStatement(e: Expression): Statement = ExpressionStmt(MethodCallExpr(NameExpr("CProver"), "assume", NodeList(e)))

    fun assertStatement(e: Expression?): Statement = AssertStmt(e)

    fun assume(ensures: Expression): Statement {
        val r = translate(ensures, TranslationMode.ASSUME)
        r.necessaryVars.addAll(r.statements)
        r.necessaryVars.add(assumeStatement(r.value))

        return BlockStmt(r.necessaryVars)
    }

    fun assert_(requires: Expression): BlockStmt {
        val r = translate(requires, TranslationMode.ASSERT)
        r.necessaryVars.addAll(r.statements)
        r.necessaryVars.add(assertStatement(r.value))

        return BlockStmt(r.necessaryVars)
    }

    fun getRelevantQuantifiers(expr: Expression): NodeList<JmlQuantifiedExpr> {
        var node: Node = expr
        val res: NodeList<JmlQuantifiedExpr> = NodeList()
        node = node.parentNode.get()
        while (true) {
            if (node !is Expression) {
                break
            } else if (node is JmlQuantifiedExpr) {
                res.add(node)
            }
            node = if (node.parentNode.isPresent) node.parentNode.get() else break
        }
        res.removeIf({ v -> !isSubNode(expr, QuantifierSplitter.getVariable(v).name) })
        return res
    }

    // Returns a list of statements that save expressions under "\old"
    fun storeOlds(requires: Expression, maxArraySize: Int): MutableList<Statement?> = OldVisitor(maxArraySize).run(requires)

    fun isSubNode(parent: Node, child: Node?): Boolean {
        val res = AtomicBoolean(false)
        parent.walk({ i ->
            if (i.equals(child)) {
                res.set(true)
            }
        })
        return res.get()
    }

    fun storeOld(
        expression: Expression, relevantQuantifiers: MutableList<JmlQuantifiedExpr>?, maxArraySize: Int
    ): NodeList<Statement?> {
        var relevantQuantifiers: MutableList<JmlQuantifiedExpr>? = relevantQuantifiers
        relevantQuantifiers = NodeList(relevantQuantifiers)
        relevantQuantifiers.removeIf { v: JmlQuantifiedExpr ->
            !isSubNode(
                expression,
                QuantifierSplitter.getVariable(v).name
            )
        }
        val translatedExpression = translate(expression.clone(), TranslationMode.JAVA)
        expression.setParentNode(expression.parentNode.get())
        val exprCopy = translatedExpression.value
        val res: NodeList<Statement?> = NodeList()
        res.addAll(translatedExpression.necessaryVars)
        res.addAll(translatedExpression.statements)

        if (relevantQuantifiers.isEmpty()) {
            // save references to old variables
            val decl =
                VariableDeclarator(VarType(), "old_" + Math.abs(expression.hashCode()), exprCopy)
            res.add(ExpressionStmt(VariableDeclarationExpr(decl, Modifier.finalModifier())))
            return res
        }

        var st = BlockStmt()
        st.statements.addAll(res)
        res.clear()

        var type: Type = VarType()

        var resolvedType: ResolvedType? = null
        var realType: Type? = null
        try {
            setCurrentNode(expression)
            resolvedType = expression.calculateResolvedType()
            realType = resolvedType2Type(resolvedType)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            println(expression)
        } catch (e: UnsolvedSymbolException) {
            e.printStackTrace()
        }

        for (i in relevantQuantifiers.indices) {
            type = ArrayType(realType)
        }
        val varDecl = VariableDeclarator(
            type,
            "old_" + Math.abs(expression.hashCode()),
            ArrayCreationExpr(
                realType,
                NodeList(ArrayCreationLevel(IntegerLiteralExpr(maxArraySize.toString()))),
                null
            )
        )
        res.add(ExpressionStmt(VariableDeclarationExpr(varDecl, Modifier.finalModifier())))
        var e: Expression? = varDecl.nameAsExpression
        for (i in relevantQuantifiers.indices.reversed()) {
            e = ArrayAccessExpr(
                e,
                BinaryExpr(
                    QuantifierSplitter.getVariable(relevantQuantifiers[i])
                        .nameAsExpression,
                    IntegerLiteralExpr(maxArraySize.toString()),
                    BinaryExpr.Operator.REMAINDER
                )
            )
        }

        e = AssignExpr(e, exprCopy, AssignExpr.Operator.ASSIGN)
        st.addStatement(ExpressionStmt(e))

        for (quantifiedExpr in relevantQuantifiers) {
            var lowerBound = QuantifierSplitter.getLowerBound(quantifiedExpr)
            val translatedLowerBound = translate(
                lowerBound.clone().also { it.setParentNode(quantifiedExpr) }, TranslationMode.DEMONIC
            )
            lowerBound = translatedLowerBound.value
            var upperBound: Expression? = QuantifierSplitter.getUpperBound(quantifiedExpr)
            val translatedUpperBound = translate(
                upperBound.clone().also { it.setParentNode(quantifiedExpr) }, TranslationMode.DEMONIC
            )
            upperBound = translatedUpperBound.value

            val loopVarDecl = VariableDeclarationExpr(
                PrimitiveType.intType(), "__tmp__" + Jml2JavaExpressionTranslator.counter.getAndIncrement()
            )
            val loopVar =
                loopVarDecl.getVariable(0).nameAsExpression
            st.accept(
                ReplaceVariable(QuantifierSplitter.getVariable(quantifiedExpr), loopVar.nameAsString),
                null
            )
            val forLoop = ForStmt(
                NodeList(AssignExpr(loopVarDecl, lowerBound, AssignExpr.Operator.ASSIGN)),
                BinaryExpr(loopVar, upperBound, BinaryExpr.Operator.LESS_EQUALS),
                NodeList<Expression?>(UnaryExpr(loopVar, UnaryExpr.Operator.POSTFIX_INCREMENT)),
                st
            )
            st = BlockStmt()
            val finalSt: BlockStmt = st
            st.addStatement(forLoop)
            translatedLowerBound.necessaryVars.forEach({ s -> finalSt.addStatement(s) })
            translatedLowerBound.statements.forEach({ s -> finalSt.addStatement(s) })
            translatedUpperBound.necessaryVars.forEach({ s -> finalSt.addStatement(s) })
            translatedUpperBound.statements.forEach({ s -> finalSt.addStatement(s) })
            st = finalSt
        }
        res.add(st)
        return res
    }

    private fun setCurrentNode(expression: Node) {
        var expression: Node = expression
        while (expression.parentNode.isPresent) {
            expression.parentNode.get()
            expression = expression.parentNode.get()
        }
        expression.setParentNode(currentNode)
    }

    fun havoc(expression: Expression): Statement = havoc(expression, true)

    fun havoc(expression: Expression, allowNull: Boolean): Statement {
        var expression: Expression = expression
        if (expression.toString() == "\\nothing") {
            return BlockStmt()
        }
        val type: ResolvedType = expression.calculateResolvedType()
        var functionName = ""
        if (expression is ArrayAccessExpr) {
            if (expression.toString().contains("*") || expression.toString().contains("..")) {
                return havocArray(expression)
            }
        }

        functionName = when (type) {
            INT -> "nondetInt"

            CHAR -> "nondetChar"

            BOOLEAN -> "nondetBoolean"

            SHORT -> "nondetShort"

            BYTE -> "nondetByte"

            LONG -> "nondetLong"

            FLOAT -> "nondetFloat"

            DOUBLE -> "nondetDouble"

            else ->
                if (allowNull) {
                    "nondetWithNull"
                } else {
                    "nondetWithoutNull"
                }
        }
        val nondetFunction = MethodCallExpr(NameExpr("CProver"), functionName, NodeList())
        if (expression is VariableDeclarationExpr) {
            expression = expression.getVariable(0).nameAsExpression
        }
        return ExpressionStmt(AssignExpr(expression, nondetFunction, AssignExpr.Operator.ASSIGN))
    }

    fun havocArray(expr: ArrayAccessExpr): Statement {
        val blockStmt = BlockStmt()
        blockStmt.setParentNode(expr.parentNode.get())
        val min = IntegerLiteralExpr("0")
        val max = FieldAccessExpr(expr.name, "length")
        val loopVarDecl = VariableDeclarationExpr(
            PrimitiveType.intType(), "__tmp__" + Jml2JavaExpressionTranslator.counter.getAndIncrement()
        )
        val loopVar =
            loopVarDecl.getVariable(0).nameAsExpression
        val element = expr.clone()
        element.setParentNode(blockStmt)
        element.setIndex(loopVar)
        val forLoop = ForStmt(
            NodeList(AssignExpr(loopVarDecl, min, AssignExpr.Operator.ASSIGN)),
            BinaryExpr(loopVar, max, BinaryExpr.Operator.LESS),
            NodeList(UnaryExpr(loopVar, UnaryExpr.Operator.POSTFIX_INCREMENT)),
            BlockStmt()
        )
        blockStmt.addStatement(forLoop)
        val havocElement: Statement = havoc(element)
        ((forLoop.body) as BlockStmt).addStatement(havocElement)

        return blockStmt
    }

    fun translate(cu: CompilationUnit, options: JJBMCOptions): CompilationUnit {
        // Normlize all binary expressions
        cu.accept(NormalizeBinaryExpressions(), null)

        // add method stubs for call to contracts
        cu.accept(CreateMethodContracts(options), null)

        // rewrite methods and loops
        val res = cu.accept(EmbeddContracts(options), null)

        // add exception type to the compilation unit
        cu.addType(createExceptionClass())

        // add CProver import statement
        cu.addImport(createCProverImport())
        return res as CompilationUnit
    }

    fun createGeneratedAnnotation(): AnnotationExpr = SingleMemberAnnotationExpr(
            Name("javax.annotation.processing.Generated"), StringLiteralExpr("JJBMC")
        )

    /**
     * Fixes an error in JavaParser pretty printing of JML-contracts
     *
     * @param translation
     * @return
     */
    fun pprint(translation: Node?): String {
        val pp = DefaultPrettyPrinter()
        return pp.print(translation)
    }

    fun createCProverImport(): ImportDeclaration = ImportDeclaration("org.cprover.CProver", false, false)

    fun createExceptionClass(): ClassOrInterfaceDeclaration {
        val exceptionClass = ClassOrInterfaceDeclaration()
        // exceptionClass.addModifier(Modifier.DefaultKeyword.PUBLIC, Modifier.DefaultKeyword.STATIC);
        exceptionClass.setName("ReturnException")
        exceptionClass.setExtendedTypes(NodeList(ClassOrInterfaceType().setName("Exception")))
        exceptionClass.addSingleMemberAnnotation("javax.annotation.processing.Generated", "\"JJBMC\"")
        return exceptionClass
    }

    /**
     * Checks whether the given node is annotated by `@javax.annotation.processing.Generated("JJBMC")`
     *
     * @param node
     * @return
     */
    fun ignoreNodeByAnnotation(node: NodeWithAnnotations<*>): Boolean {
        try {
            val value = node.getAnnotationByName("javax.annotation.processing.Generated").orElse(null)
                ?.asSingleMemberAnnotationExpr()?.memberValue?.asStringLiteralExpr()?.getValue()
            return value!!.equals("JJBMC")
        } catch (_: NoSuchElementException) {
        } catch (_: ClassCastException) {
        } catch (_: IllegalStateException) {
        }
        return false
    }

    fun translate(expression: Expression, mode: TranslationMode): Result {
        val j2jt = Jml2JavaExpressionTranslator()
        return j2jt.accept(expression, mode)
    }

    fun containsJmlExpression(expression: Expression?): Boolean {
        val search: Stack<Expression> = Stack<Expression>()
        search.add(expression)

        while (!search.isEmpty()) {
            val e: Expression = search.pop()
            if (e is Jmlish) {
                return true
            }

            if (e is NameExpr) {
                if (e.nameAsString.startsWith("\\")) {
                    return true
                }
            }

            if (e is MethodCallExpr) {
                if (e.nameAsString.startsWith("\\")) {
                    return true
                }
            }

            if (e is BinaryExpr) {
                if (e.operator === BinaryExpr.Operator.IMPLICATION) return true
                if (e.operator === BinaryExpr.Operator.RIMPLICATION) return true
                if (e.operator === BinaryExpr.Operator.EQUIVALENCE) return true
                if (e.operator === BinaryExpr.Operator.SUB_LOCK) return true
                if (e.operator === BinaryExpr.Operator.SUB_LOCKE) return true
                if (e.operator === BinaryExpr.Operator.SUBTYPE) return true
                if (e.operator === BinaryExpr.Operator.RANGE) return true
                if (e.operator === BinaryExpr.Operator.ANTIVALENCE) return true
            }

            for (childNode in e.childNodes) {
                if (childNode is Expression) search.add(childNode)
            }
        }
        return false
    }

    fun unroll(n: JmlMultiCompareExpr): Expression {
        val r =
            if (n.expressions.isEmpty()) {
                BooleanLiteralExpr(true)
            } else if (n.expressions.size == 1) {
                n.expressions[0]
            } else {
                var e: Expression? = null
                for (i in 0..<n.expressions.size - 1) {
                    val cmp = BinaryExpr(
                        n.expressions[i].clone(),
                        n.expressions[i + 1].clone(),
                        n.operators[i]
                    )
                    e = if (e == null) cmp else BinaryExpr(e, cmp, BinaryExpr.Operator.AND)
                }
                e!!
            }
        r.setParentNode(n.parentNode.orElse(null))
        return r
    }

    fun resolvedType2Type(type: ResolvedType): Type? {
        if (type.isPrimitive) {
            val rType = type.asPrimitive()
            val t = when (rType) {
                BYTE -> PrimitiveType.Primitive.BYTE
                SHORT -> PrimitiveType.Primitive.SHORT
                CHAR -> PrimitiveType.Primitive.CHAR
                INT -> PrimitiveType.Primitive.INT
                LONG -> PrimitiveType.Primitive.LONG
                BOOLEAN -> PrimitiveType.Primitive.BOOLEAN
                FLOAT -> PrimitiveType.Primitive.FLOAT
                DOUBLE -> PrimitiveType.Primitive.DOUBLE
            }
            return PrimitiveType(t)
        }

        if (type.isArray) {
            val aType = type.asArrayType()
            return ArrayType(resolvedType2Type(aType.componentType))
        }

        if (type.isReferenceType) {
            val rType = type.asReferenceType()
            return ClassOrInterfaceType().setName(rType.qualifiedName)
        }

        throw RuntimeException("Unsupported type")
    }

    data class Result(
        var statements: NodeList<Statement> = NodeList(),
        var value: Expression,
        var necessaryVars: NodeList<Statement> = NodeList()
    )

    private class OldVisitor(private val maxArraySize: Int) : ModifierVisitor<Any>() {
        val currentQuantifiers: NodeList<JmlQuantifiedExpr> = NodeList()
        val statements: NodeList<Statement> = NodeList()

        fun run(expr: Expression): NodeList<Statement> {
            val v = OldVisitor(maxArraySize)
            expr.accept(v, null)
            return v.statements
        }

        override fun visit(jmlQuantifiedExpr: JmlQuantifiedExpr, arg: Any): Visitable {
            currentQuantifiers.add(jmlQuantifiedExpr)
            val res = super.visit(jmlQuantifiedExpr, arg)
            currentQuantifiers.remove(jmlQuantifiedExpr)
            return res
        }

        override fun visit(n: MethodCallExpr, arg: Any): Visitable {
            if (n.nameAsString.equals("\\old")) {
                statements.addAll(storeOld(n.getArgument(0), currentQuantifiers, maxArraySize))
            }
            return super.visit(n, arg)
        }
    }
}
