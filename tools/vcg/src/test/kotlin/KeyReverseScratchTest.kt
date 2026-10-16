/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import org.junit.jupiter.api.Test

class KeyReverseScratchTest {
    private fun verify(name: String, src: String) {
        val parser = com.github.javaparser.JavaParser(VcgSemanticsTestParserConfig.newConfig())
        val r = parser.parse(src)
        val cu = r.result.get()
        for (md in cu.findAll(com.github.javaparser.ast.body.MethodDeclaration::class.java)) {
            if (md.contracts.isEmpty()) continue
            try {
                Vcg(VcgContext.of(md), VcgOptions()).verify()
                println("OK   $name")
            } catch (e: Exception) {
                println("FAIL $name: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    @Test
    fun variants() {
        verify(
            "return-new-param-dim",
            """
            class T { //@ requires n >= 0; //@ ensures true;
            public int[] m(int n) { return new int[n]; } }
        """.trimIndent()
        )
        verify(
            "assign-new-param-dim",
            """
            class T { //@ requires n >= 0; //@ ensures true;
            public int[] m(int n) { int[] b = new int[n]; return b; } }
        """.trimIndent()
        )
        verify(
            "return-new-thisfield-dim",
            """
            class T { public int[] a;
            //@ requires a != null && a.length >= 0; //@ ensures true;
            public int[] m() { return new int[a.length]; } }
        """.trimIndent()
        )
        verify(
            "assign-new-thisfield-dim",
            """
            class T { public int[] a;
            //@ requires a != null && a.length >= 0; //@ ensures true;
            public int[] m() { int[] b = new int[a.length]; return b; } }
        """.trimIndent()
        )
        verify(
            "assign-new-paramfield-dim",
            """
            class T { //@ requires p_a != null && p_a.length >= 0; //@ ensures true;
            public int[] m(int[] p_a) { int[] b = new int[p_a.length]; return b; } }
        """.trimIndent()
        )
        verify(
            "assign-new-literal-dim",
            """
            class T { //@ ensures true;
            public int[] m() { int[] b = new int[4]; return b; } }
        """.trimIndent()
        )
    }
}

object VcgSemanticsTestParserConfig {
    fun newConfig(): com.github.javaparser.ParserConfiguration {
        val config = com.github.javaparser.ParserConfiguration()
        config.setProcessJml(true)
        config.setSymbolResolver(
            com.github.javaparser.symbolsolver.JavaSymbolSolver(
                com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(
                    com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver(java.nio.file.Path.of("src/test/resources")),
                    com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver()
                )
            )
        )
        return config
    }
}
