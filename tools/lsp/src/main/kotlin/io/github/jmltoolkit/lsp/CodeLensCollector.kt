/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.ast.jml.clauses.ContractType
import com.github.javaparser.ast.jml.clauses.JmlContract
import io.github.jmltoolkit.lsp.actions.VerifyAgainstParent
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.Command

/**
 * Runs through the AST and collect code lens actions.
 */
class CodeLensCollector : ResultingVisitor<MutableList<out CodeLens>>() {
    override val result = arrayListOf<CodeLens>()

    override fun visit(n: JmlContract, arg: Unit?) {
        if (n.type == ContractType.METHOD) {
            // result.add(VerifyAgainstParent.createCodeLens(n))
        }
    }
}
