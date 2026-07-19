/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.jml.body.JmlClassLevelDeclaration
import java.io.File

/**
 * Combiner for merging multiple JML specifications and stub files.
 * Allows combining JML contracts from different sources into unified specifications.
 *
 * Merge Behavior:
 * - Methods and fields are additive (combined from all compilation units)
 * - JML contracts: Only the latest compilation unit's contracts are used (not merged)
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubCombiner(val config: StubConfig = StubConfig()) {

    /**
     * Combine multiple CompilationUnits into a single unit.
     * 
     * Merge Behavior:
     * - Methods and fields are additive from all units
     * - JML contracts: Only the last unit's contracts are preserved
     */
    fun combine(units: List<CompilationUnit>): CompilationUnit {
        if (units.isEmpty()) {
            return CompilationUnit()
        }

        if (units.size == 1) {
            return units.first().clone()
        }

        val result = CompilationUnit()
        result.setPackageDeclaration(units.firstOrNull()?.packageDeclaration?.get())

        // Merge imports from all units
        val allImports = mutableSetOf<String>()
        units.forEach { cu ->
            cu.imports.forEach { import ->
                allImports.add(import.nameAsString)
            }
        }
        allImports.forEach { importName ->
            result.addImport(importName)
        }

        // Group types by qualified name
        val typesByName = mutableMapOf<String, MutableList<ClassOrInterfaceDeclaration>>()

        units.forEach { cu ->
            cu.types.forEach { type ->
                if (type is ClassOrInterfaceDeclaration) {
                    val key = buildTypeName(cu, type)
                    typesByName.computeIfAbsent(key) { mutableListOf() }.add(type)
                }
            }
        }

        // Merge types - methods/fields are additive, JML contracts from latest only
        typesByName.forEach { (_, declarations) ->
            val merged = mergeClassDeclarations(declarations)
            result.addType(merged)
        }

        return result
    }

    /**
     * Combine JML contracts from multiple method declarations.
     * Only uses contracts from the last method declaration (latest wins).
     */
    fun combineContracts(methods: List<MethodDeclaration>): MethodDeclaration {
        if (methods.isEmpty()) {
            throw IllegalArgumentException("Methods list cannot be empty")
        }

        // Use the last method's contracts only - latest wins
        return methods.last().clone()
    }

    /**
     * Merge JML specification strings.
     */
    fun mergeSpecifications(specs: List<String>, separator: String = "\n"): String {
        return specs.filter { it.isNotBlank() }.joinToString(separator)
    }

    /**
     * Load and combine JML files from a directory.
     */
    fun combineFromDirectory(directory: File): CompilationUnit {
        val files = directory.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".java") || it.name.endsWith(".jml")) }
            .toList()
        return combineFromFiles(files)
    }

    /**
     * Load and combine JML files from a list of files.
     */
    fun combineFromFiles(files: List<File>): CompilationUnit {
        val units = mutableListOf<CompilationUnit>()
        val generator = StubGenerator()

        files.forEach { file ->
            if (file.name.endsWith(".java")) {
                val cu = generator.parser.parse(file).result.get()
                units.add(generator.generate(cu))
            }
        }

        return combine(units)
    }

    private fun buildTypeName(cu: CompilationUnit, type: ClassOrInterfaceDeclaration): String {
        val packageName = cu.packageDeclaration()?.nameAsString ?: ""
        return if (packageName.isEmpty()) type.nameAsString else "$packageName.${type.nameAsString}"
    }

    private fun mergeClassDeclarations(declarations: List<ClassOrInterfaceDeclaration>): ClassOrInterfaceDeclaration {
        if (declarations.isEmpty()) {
            throw IllegalArgumentException("Declarations list cannot be empty")
        }

        val result = declarations.first().clone()
        val methodSignatures = mutableMapOf<String, MethodDeclaration>()
        val fieldNames = mutableSetOf<String>()

        declarations.forEachIndexed { index, decl ->
            val isLast = index == declarations.lastIndex

            // Methods are additive, but use latest version for same signature
            decl.methods.forEach { method ->
                val signature = getMethodSignature(method)
                methodSignatures[signature] = method.clone()
            }

            // Fields are additive (unique by name)
            decl.fields.forEach { field ->
                field.variables.forEach { variable ->
                    if (!fieldNames.contains(variable.nameAsString)) {
                        fieldNames.add(variable.nameAsString)
                        result.addMember(field.clone())
                    }
                }
            }

            // JML contracts: Only use the last declaration's contracts
            if (isLast) {
                replaceJmlContracts(result, decl)
            }
        }

        // Replace methods with collected ones
        result.methods.forEach { it.remove() }
        methodSignatures.values.forEach { result.addMember(it) }

        return result
    }

    private fun getMethodSignature(method: MethodDeclaration): String {
        val paramTypes = method.parameters.joinToString(",") { it.type.toString() }
        return "${method.nameAsString}($paramTypes)"
    }

    private fun replaceJmlContracts(target: ClassOrInterfaceDeclaration, source: ClassOrInterfaceDeclaration) {
        target.getJmlClassLevelDeclarations().forEach { it.remove() }
        source.getJmlClassLevelDeclarations().forEach { contract ->
            target.addMember(contract.clone())
        }

        source.methods.forEach { sourceMethod ->
            val targetMethod = target.getMethodsByName(sourceMethod.nameAsString)
            if (targetMethod.isNotEmpty()) {
                targetMethod.forEach { method ->
                    replaceMethodJmlContracts(method, sourceMethod)
                }
            }
        }
    }

    private fun replaceMethodJmlContracts(target: MethodDeclaration, source: MethodDeclaration) {
        target.annotations.filter { isJmlAnnotation(it.nameAsString) }.forEach { it.remove() }
        source.annotations.filter { isJmlAnnotation(it.nameAsString) }.forEach { annotation ->
            target.addAnnotation(annotation.clone())
        }
    }

    private fun isJmlAnnotation(name: String): Boolean {
        val jmlAnnotations = setOf(
            "Requires", "Ensures", "Assignable", "Invariant",
            "Pure", "Helper", "SpecPublic", "Ghost", "Model"
        )
        return jmlAnnotations.any { name.contains(it, ignoreCase = true) }
    }
}

/**
 * Extension function to get JML class-level declarations.
 */
fun ClassOrInterfaceDeclaration.getJmlClassLevelDeclarations(): NodeList<JmlClassLevelDeclaration<*>> {
    val declarations = NodeList<JmlClassLevelDeclaration<*>>()
    this.members.forEach { member ->
        if (member is JmlClassLevelDeclaration) {
            declarations.add(member)
        }
    }
    return declarations
}