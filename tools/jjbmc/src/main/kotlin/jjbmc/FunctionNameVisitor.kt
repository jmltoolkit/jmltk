/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder
import java.io.IOException
import java.nio.file.Path
import java.util.*

class FunctionNameVisitor(cu: CompilationUnit, private val getAll: Boolean) {
    val unwinds: MutableList<String> = ArrayList<String>()
    val functionNames: MutableList<String> = ArrayList<String>()
    val paramMap = HashMap<String, MutableList<String>>()
    val functionBehaviours: MutableList<TestBehaviour> = LinkedList<TestBehaviour>()

    init {
        if (cu.primaryType.isPresent) {
            cu.primaryType.get().members
                .filter { it.isCallableDeclaration }
                .forEach { this.visit(it.asCallableDeclaration()) }
        }
    }

    enum class TestBehaviour {
        Verifyable,
        Fails,
        Ignored
    }

    fun visit(that: CallableDeclaration<*>) {
        // var rm = that.resolve();
        // not interested in methods of inner classes
        if (that.name.toString().contains("$")) {
            return
        }
        // String f = rm.getQualifiedName();
        val f = that.nameAsString
        // String rtString = typeToString(that.getType());
        val paramString = that.getDeclarationAsString(false, false, false)
        if (f.endsWith("Verification") || f.endsWith("<init>") || getAll) {
            functionNames.add(paramString)
        }
        for (p in that.parameters) {
            val name =
                if (that.hasModifier(Modifier.DefaultKeyword.STATIC)) {
                    "\$static_$f"
                } else {
                    f
                }
            paramMap.computeIfAbsent(name) { LinkedList() }.add(p.nameAsString)
        }
        translateAnnotations(that.annotations)
    }

    private fun translateAnnotations(annotations: NodeList<AnnotationExpr>) {
        for (annotation in annotations) {
            // var ra = annotation.resolve();
            when (annotation.nameAsString) {
                "Fails" -> functionBehaviours.add(TestBehaviour.Fails)

                "Verifyable" -> functionBehaviours.add(TestBehaviour.Verifyable)

                "Unwind" -> {
                    try {
                        unwinds.add(
                            annotation
                                .asSingleMemberAnnotationExpr()
                                .memberValue
                                .asIntegerLiteralExpr()
                                .getValue()
                        )
                    } catch (e: Exception) {
                        try {
                            unwinds.add(
                                annotation.asNormalAnnotationExpr()
                                    .pairs.first().value
                                    .asIntegerLiteralExpr()
                                    .getValue()
                            )
                        } catch (e1: Exception) {
                            ErrorLogger.warn("Cannot parse annotation %s", annotation)
                        }
                    }
                }

                else -> ErrorLogger.warn("Found unknown annotation: %s", annotation)
            }
        }

        if (functionNames.size != functionBehaviours.size) {
            functionBehaviours.add(TestBehaviour.Ignored)
        }
        if (functionBehaviours.size != unwinds.size) {
            unwinds.add(null)
        }
    }

    private fun typeToString(type: Type): String = type.toDescriptor()

    companion object {
        fun parseFile(fileName: Path, getAll: Boolean): FunctionNameVisitor {
            try {
                val config = ParserConfiguration()
                config.setProcessJml(true)
                val typeSolver = TypeSolverBuilder().withCurrentJRE().build()
                config.setSymbolResolver(JavaSymbolSolver(typeSolver))
                val cu = JavaParser(config).parse(fileName)
                if (!cu.isSuccessful) {
                    cu.problems.forEach(System.out::println)
                    throw RuntimeException()
                }
                return FunctionNameVisitor(cu.getResult().get(), getAll)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}
