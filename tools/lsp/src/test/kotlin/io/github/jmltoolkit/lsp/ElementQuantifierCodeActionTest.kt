/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.google.common.truth.Truth.assertThat
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Test

/**
 * Element-domain quantifiers (single binding with an iterable domain) must not
 * crash the code-action collector, and kinds 2 (range) / 3 (array) offer an
 * "expand" quick-fix rewriting to the classic index form via
 * [com.github.javaparser.jml.JmlQuantifierDesugarer].
 *
 * @author Alexander Weigl
 */
class ElementQuantifierCodeActionTest {

    private fun collect(code: String): MutableList<Either<Command, CodeAction>> {
        val config = ParserConfiguration()
            .setProcessJml(true)
            .setSymbolResolver(JavaSymbolSolver(ReflectionTypeSolver(true)))
        val cu: CompilationUnit = JavaParser(config).parse(code).result.get()
        val range = cu.range.orElseThrow()
        val collector = CodeActionCollector(CodeActionContext(listOf(), null), range)
        cu.accept(collector, null)
        return collector.result
    }

    @Test
    fun elementQuantifiersExpandToClassicForm() {
        val code = """
            class Tst {
                int n;
                int[] a;
                //@ invariant (\forall int e : a; e > 0);
                //@ invariant (\forall int i : 0..n; a[i] > 0);
            }
        """.trimIndent()
        val actions = collect(code)
        val expands = actions.mapNotNull { it.right?.title }
            .filter { it.startsWith("Expand quantifier domain") }
        assertThat(expands).containsExactly(
            "Expand quantifier domain: (\\forall int _i; 0 <= _i && _i < a.length; a[_i] > 0)",
            "Expand quantifier domain: (\\forall int i; 0 <= i && i < n; a[i] > 0)"
        )
    }

    @Test
    fun classicAndIterableDomainsOfferNoExpand() {
        val code = """
            class Tst {
                int[] a;
                java.util.List<String> words;
                //@ invariant (\forall int i; 0 <= i && i < a.length; a[i] > 0);
                //@ invariant (\forall String s : words; s.length() > 0);
            }
        """.trimIndent()
        val actions = collect(code)
        val expands = actions.mapNotNull { it.right?.title }
            .filter { it.startsWith("Expand quantifier domain") }
        assertThat(expands).isEmpty()
        // the classic quantifier still gets its boundary action, without crashing
        assertThat(actions.mapNotNull { it.right?.title }).contains("Add boundary")
    }
}
