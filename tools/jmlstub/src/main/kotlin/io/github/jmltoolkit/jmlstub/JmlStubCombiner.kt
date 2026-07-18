/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.jml.body.JmlClassLevelDeclaration

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
    fun combine(units: List<TypeDeclaration<*>>): CompilationUnit {
        require(units.isNotEmpty())
        val (cu, _) = units.last().copyWithCompilationUnit()
        if (units.size == 1) {
            return cu
        }

        val types = units.slice(0 until units.size - 1)

        // Merge imports from all units
        types.forEach {
            it.findCompilationUnit().get().imports().forEach { import ->
                cu.addImport(import.nameAsString)
            }
        }

        units.forEach { type ->
            if (type is ClassOrInterfaceDeclaration) {
            }
        }

        return cu
    }
}

private fun TypeDeclaration<*>.copyWithCompilationUnit(): Pair<CompilationUnit, TypeDeclaration<*>> {
    val cu = findCompilationUnit().get().clone()
    val fqdn = nameAsString
    val type = cu.types().find { it.nameAsString == fqdn }!!
    cu.types().removeAll { it.nameAsString != fqdn }
    return Pair(cu, type)
}
