/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File

/**
 * Generator for creating Java stub files from existing Java source code.
 * The generator preserves JML specifications while replacing method bodies with empty implementations.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubGenerator(
    private val config: JmlStubConfig = JmlStubConfig()
) {
    private val parser: JavaParser by lazy {
        val parserConfig = ParserConfiguration()
        parserConfig.setProcessJml(config.processJml)
        if (!config.jmlKeys.isEmpty()) {
            parserConfig.jmlKeys.addAll(config.jmlKeys)
        }
        JavaParser(parserConfig)
    }

    /**
     * Generate stubs from a list of Java source files.
     */
    fun generate(files: List<File>): List<CompilationUnit> {
        val result = mutableListOf<CompilationUnit>()
        for (file in files) {
            if (file.isDirectory) {
                result.addAll(generateFromDirectory(file))
            } else if (file.name.endsWith(".java")) {
                parseAndGenerateStub(file)?.let { result.add(it) }
            }
        }
        return result
    }

    /**
     * Generate stubs from a directory recursively.
     */
    fun generateFromDirectory(directory: File): List<CompilationUnit> {
        val result = mutableListOf<CompilationUnit>()
        directory.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.forEach { file ->
            parseAndGenerateStub(file)?.let { result.add(it) }
        }
        return result
    }

    /**
     * Generate a stub from a single Java file.
     */
    fun generateFromFile(file: File): CompilationUnit? {
        return parseAndGenerateStub(file)
    }

    /**
     * Generate a stub from a CompilationUnit.
     */
    fun generate(cu: CompilationUnit): CompilationUnit {
        val stubCreator = StubCreator(config)
        val visitor = StubVisitor(stubCreator)
        cu.accept(visitor, null)
        return cu
    }

    /**
     * Combine multiple JML specifications into a single specification.
     */
    fun combineSpecifications(specs: List<String>): String {
        return specs.filter { it.isNotBlank() }.joinToString("\n    ")
    }

    private fun parseAndGenerateStub(file: File): CompilationUnit? {
        val parseResult: ParseResult<CompilationUnit> = parser.parse(file)
        if (!parseResult.isSuccessful) {
            parseResult.problems.forEach { problem ->
                System.err.println("Error parsing ${file.name}: $problem")
            }
            return null
        }
        val cu = parseResult.result.get()
        return generate(cu)
    }

    /**
     * Visitor that transforms Java declarations into stubs.
     */
    private class StubVisitor(private val stubCreator: StubCreator) : VoidVisitorAdapter<Unit>() {
        
        override fun visit(n: ClassOrInterfaceDeclaration, arg: Unit?) {
            if (n.isAbstract) {
                super.visit(n, arg)
                return
            }
            stubCreator.makeStub(n)
            super.visit(n, arg)
        }

        override fun visit(n: MethodDeclaration, arg: Unit?) {
            if (n.isAbstract || n.isNative) {
                super.visit(n, arg)
                return
            }
            stubCreator.makeMethodStub(n)
            super.visit(n, arg)
        }

        override fun visit(n: ConstructorDeclaration, arg: Unit?) {
            stubCreator.makeConstructorStub(n)
            super.visit(n, arg)
        }
    }
}

/**
 * Configuration options for JML stub generation.
 */
data class JmlStubConfig(
    val processJml: Boolean = true,
    val jmlKeys: List<String> = emptyList(),
    val excludeClasses: Boolean = false,
    val removeGhostFields: Boolean = false,
    val preserveContracts: Boolean = true,
    val addGeneratedAnnotation: Boolean = true,
    val throwUnsupportedForStubs: Boolean = false
)

/**
 * Helper class for creating stub implementations.
 */
class StubCreator(private val config: JmlStubConfig) {
    
    fun makeStub(declaration: ClassOrInterfaceDeclaration) {
        if (config.addGeneratedAnnotation) {
            addGeneratedMarker(declaration)
        }
    }

    fun makeMethodStub(method: MethodDeclaration) {
        if (config.addGeneratedAnnotation) {
            addGeneratedMarker(method)
        }
        val stubBody = createStubBody(method.type)
        method.setBody(stubBody)
    }

    fun makeConstructorStub(constructor: ConstructorDeclaration) {
        if (config.addGeneratedAnnotation) {
            addGeneratedMarker(constructor)
        }
        constructor.setBody(BlockStmt())
    }

    private fun createStubBody(returnType: Type): BlockStmt {
        val body = BlockStmt()
        when {
            returnType.toString() == "void" -> { /* Empty */ }
            returnType.isPrimitive -> {
                val defaultExpr = getDefaultPrimitiveValue(returnType)
                body.addStatement(com.github.javaparser.StaticJavaParser.parseStatement("return $defaultExpr;"))
            }
            else -> {
                if (config.throwUnsupportedForStubs) {
                    body.addStatement(com.github.javaparser.StaticJavaParser.parseStatement(
                        "throw new UnsupportedOperationException(\"Stub method\");"))
                } else {
                    body.addStatement(com.github.javaparser.StaticJavaParser.parseStatement("return null;"))
                }
            }
        }
        return body
    }

    private fun getDefaultPrimitiveValue(type: Type): String {
        return when (type.toString()) {
            "byte", "short", "int", "long" -> "0"
            "float", "double" -> "0.0"
            "char" -> "'\\u0000'"
            "boolean" -> "false"
            else -> "null"
        }
    }

    private fun addGeneratedMarker(node: com.github.javaparser.ast.Node) {
        node.addSingleMemberAnnotation("Generated", StringLiteralExpr("io.github.jmltoolkit.jmlstub.JmlStubGenerator"))
    }
}