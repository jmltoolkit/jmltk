/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.jml.clauses.*
import com.github.javaparser.ast.stmt.Behavior
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.ReferenceType
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Parameter

private val Class<*>.toUsableName
    get() = when {
        isArray -> this.simpleName
        else -> this.name
    }

class JREClassStubGenerator(private val clazz: Class<*>) {
    fun generate() = createCompilationUnit(clazz)
    private fun createCompilationUnit(clazz: Class<*>) = buildSourceFromClass(clazz)
    private fun buildSourceFromClass(clazz: Class<*>): CompilationUnit {
        val cu = CompilationUnit()
        val packageName = clazz.packageName
        if (!packageName.isNullOrBlank()) {
            cu.setPackageDeclaration(packageName)
        }

        val type = when {
            clazz.isInterface -> cu.addInterface(clazz.simpleName)
            clazz.isAnnotation -> cu.addAnnotationDeclaration(clazz.simpleName)
            clazz.isEnum -> cu.addEnum(clazz.simpleName)
            else -> cu.addClass(clazz.simpleName)
        }
        type.modifiers.clear()
        val modifiers = getModifier(clazz.modifiers)
        type.modifiers.addAll(modifiers.map { Modifier(it) })

        if (type is ClassOrInterfaceDeclaration) {
            if (!type.isInterface) {
                val superClass = clazz.superclass?.toUsableName
                if (superClass != null && superClass != "java.lang.Object") {
                    cu.addImport(superClass)
                    type.extendedTypes().add(ClassOrInterfaceType(null, superClass))
                }

                clazz.interfaces.forEach {
                    type.implementedTypes().add(ClassOrInterfaceType(null, it.name))
                }
            } else {
                clazz.interfaces.forEach {
                    type.extendedTypes().add(ClassOrInterfaceType(null, it.name))
                }
            }
        }

        clazz.declaredFields.forEach { field ->
            val fieldModifiers = getModifier(field.modifiers)
            type.addField(field.type.toUsableName, field.name, *fieldModifiers.toTypedArray())
        }

        clazz.constructors.forEach { constructor ->
            val mods = getModifier(constructor.modifiers)
            val ctor = type.addConstructor(*mods.toTypedArray())
            ctor.setBody(null)
            ctor.parameters().addAll(getParameters(constructor))
        }

        clazz.declaredMethods
            .filterNot { isClinitOrInit(it.name) }
            .filter { !it.isSynthetic }
            .forEach { method ->
                val methodModifiers = getModifier(method.modifiers)
                val met = type.addMethod(method.name, *methodModifiers.toTypedArray())
                met.parameters().addAll(getParameters(method))

                method.parameterAnnotations.forEach {
                    // met.tryAddImportToParentCompilationUnit(it.name)
                    // met.addTypeParameter(it.simpleName)
                }

                method.exceptionTypes.forEach { exc ->
                    met.tryAddImportToParentCompilationUnit(exc)
                    met.addThrownException(ClassOrInterfaceType(null, exc.simpleName))
                }

                method.annotations.forEach {
                    println("annotation: $it")
                }

                if (!method.returnType.isArray) {
                    met.tryAddImportToParentCompilationUnit(method.returnType)
                }
                val rt = met.setType(method.genericReturnType.typeName)
                method.annotatedReturnType.annotations.forEach { annotation ->
                    rt.addAnnotation(annotation.toJavaParser())
                }

                met.setBody(null)

                met.addStubContracts()
            }
        return cu
    }

    private fun getParameters(constructor: Constructor<*>) = getParameters(constructor.parameters)
    private fun getParameters(behavior: Method) = getParameters(behavior.parameters)
    private fun getParameters(parameters: Array<Parameter>) =
        parameters.map { com.github.javaparser.ast.body.Parameter(ClassOrInterfaceType(null, it.parameterizedType.typeName), it.name) }

    private fun isClinitOrInit(name: String): Boolean = name == "<clinit>" || name == "<init>"
}

internal fun MethodDeclaration.addStubContracts() {
    addStubNormalBehaviorContract()
    addStubExceptionalBehaviorContract()
}

private fun MethodDeclaration.addStubExceptionalBehaviorContract() {
    if (this.thrownExceptions.isEmpty()) return

    val c = JmlContract()
    c.behavior = Behavior.EXCEPTIONAL
    c.addModifier(Modifier.DefaultKeyword.PUBLIC)
    c.setName(SimpleName("with_exception"))

    c.clauses.add(clauseRequires("true"))
    c.clauses.add(clauseEnsures("true"))

    this.thrownExceptions.forEach {
        c.clauses.add(clauseSignals(it, "true"))
    }

    c.clauses.add(clauseAssignable("true"))

    addContract(c)
}

private fun MethodDeclaration.addStubNormalBehaviorContract() {
    val c = JmlContract()
    c.behavior = Behavior.NORMAL
    c.addModifier(Modifier.DefaultKeyword.PUBLIC)
    c.setName(SimpleName("normal"))

    c.clauses.add(clauseRequires("true"))
    c.clauses.add(clauseEnsures("true"))
    c.clauses.add(clauseAssignable("true"))

    addContract(c)
}

internal fun clauseSignals(
    it: ReferenceType,
    expr: String
) = JmlSignalsClause(
    null,
    com.github.javaparser.ast.body.Parameter(it.clone(), "e"),
    StaticJavaParser.parseJmlExpression(expr)
)

internal fun clauseRequires(expr: String): JmlClause =
    JmlSimpleExprClause(JmlClauseKind.REQUIRES, null, NodeList(), StaticJavaParser.parseJmlExpression(expr))

internal fun clauseEnsures(expr: String): JmlClause =
    JmlSimpleExprClause(JmlClauseKind.ENSURES, null, NodeList(), StaticJavaParser.parseJmlExpression(expr))

internal fun clauseAssignable(expr: String): JmlClause = JmlMultiExprClause(
    JmlClauseKind.ACCESSIBLE, null, NodeList(),
    NodeList(StaticJavaParser.parseJmlExpression<Expression>(expr))
)

@Suppress("UNCHECKED_CAST")
internal fun Annotation.toJavaParser(): AnnotationExpr {
    val annotationType = this.annotationClass.java
    val name = Name(annotationType.simpleName)

    // Get all annotation member methods and their values
    val memberMethods = annotationType.declaredMethods.filter { it.name != "annotationType" }

    if (memberMethods.isEmpty()) {
        // No members - use MarkerAnnotationExpr
        return MarkerAnnotationExpr(name)
    }

    // Build member value pairs
    val pairs = NodeList<MemberValuePair>()
    var singleValue: Expression? = null

    for (method in memberMethods) {
        val value = method.invoke(this)
        val expr = value.toExpression()

        if (memberMethods.size == 1 && method.name == "value") {
            // Single member named "value" - use SingleMemberAnnotationExpr
            singleValue = expr
        } else {
            // Multiple members or non-"value" named member - use NormalAnnotationExpr
            pairs.add(MemberValuePair(SimpleName(method.name), expr))
        }
    }

    return when {
        singleValue != null -> SingleMemberAnnotationExpr(name, singleValue)
        pairs.isNotEmpty() -> NormalAnnotationExpr(name, pairs)
        else -> MarkerAnnotationExpr(name)
    }
}

internal fun Any?.toExpression(): Expression = when (val v = this) {
        is String -> StringLiteralExpr(v)

        is Boolean -> if (v) NameExpr("true") else NameExpr("false")

        is Char -> StringLiteralExpr(v.toString())

        is Number -> {
            when (v) {
                is Int -> IntegerLiteralExpr(v.toString())
                is Long -> IntegerLiteralExpr("${v}L")
                is Float -> DoubleLiteralExpr("${v}f")
                is Double -> DoubleLiteralExpr(v.toString())
                is Short -> IntegerLiteralExpr(v.toString())
                is Byte -> IntegerLiteralExpr(v.toString())
                else -> StringLiteralExpr(v.toString())
            }
        }

        is Class<*> -> {
            NameExpr("${v.name}.class")
        }

        is Enum<*> -> {
            NameExpr("${v.javaClass.declaringClass.simpleName}.${v.name}")
        }

        is Array<*> -> {
            val elements = v.mapNotNull { elem -> elem?.toExpression() }
            ArrayInitializerExpr(NodeList(elements))
        }

        is Annotation -> {
            v.toJavaParser()
        }

        else -> StringLiteralExpr(v?.toString() ?: "")
    }
