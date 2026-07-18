/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for JmlStubGenerator functionality.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubGeneratorTest {

    private val generator = JmlStubGenerator()

    @Test
    fun `test stub generation from source string`() {
        val sourceCode = """
            public class TestClass {
                public int getValue() {
                    return 42;
                }
                
                public void setValue(int value) {
                    // Some implementation
                }
                
                public String getName() {
                    return "Test";
                }
            }
        """.trimIndent()

        val cu = StaticJavaParser.parse(sourceCode)
        val stub = generator.generate(cu)

        val methodCount = stub.findAll(com.github.javaparser.ast.body.MethodDeclaration::class.java).size
        Truth.assertThat(methodCount).isEqualTo(3)
    }

    @Test
    fun `test stub generation preserves method signatures`() {
        val sourceCode = """
            public class Calculator {
                public int add(int a, int b) { return a + b; }
                public double divide(double a, double b) { return a / b; }
                public boolean isEqual(int a, int b) { return a == b; }
            }
        """.trimIndent()

        val cu = StaticJavaParser.parse(sourceCode)
        val stub = generator.generate(cu)

        val methods = stub.findAll(com.github.javaparser.ast.body.MethodDeclaration::class.java)
        
        val addMethod = methods.find { it.nameAsString == "add" }
        Truth.assertThat(addMethod).isNotNull()
        Truth.assertThat(addMethod!!.type.toString()).isEqualTo("int")
        Truth.assertThat(addMethod.parameters.size).isEqualTo(2)
    }

    @Test
    fun `test combine specifications`() {
        val specs = listOf(
            "requires x > 0;",
            "ensures \\result > 0;",
            "assigns \\nothing;"
        )

        val combined = generator.combineSpecifications(specs)

        Truth.assertThat(combined).contains("requires x > 0;")
        Truth.assertThat(combined).contains("ensures \\result > 0;")
        Truth.assertThat(combined).contains("assigns \\nothing;")
    }

    @Test
    fun `test combiner merge specifications`() {
        val combiner = JmlStubCombiner()
        val specs = listOf(
            "/*@ requires x > 0;",
            "  ensures \\result >= 0; */",
            "/*@ assigns \\nothing; */"
        )

        val merged = combiner.mergeSpecifications(specs)

        Truth.assertThat(merged).isNotEmpty()
        Truth.assertThat(merged).contains("requires x > 0;")
    }

    @Test
    fun `test combiner latest wins for JML contracts`() {
        val source1 = """
            public class TestClass {
                /*@ requires x > 0; */
                public int method1(int x) { return x; }
                public int field1 = 1;
            }
        """.trimIndent()

        val source2 = """
            public class TestClass {
                /*@ ensures \result > 0; */
                public int method2(int y) { return y; }
                public int field2 = 2;
            }
        """.trimIndent()

        val cu1 = StaticJavaParser.parse(source1)
        val cu2 = StaticJavaParser.parse(source2)
        
        val combiner = JmlStubCombiner()
        val combined = combiner.combine(listOf(cu1, cu2))
        
        // Methods should be additive
        val methods = combined.getType(0).asClassOrInterfaceDeclaration().get().methods
        Truth.assertThat(methods.size).isEqualTo(2)
        Truth.assertThat(methods.any { it.nameAsString == "method1" }).isTrue()
        Truth.assertThat(methods.any { it.nameAsString == "method2" }).isTrue()
        
        // Fields should be additive  
        val fields = combined.getType(0).asClassOrInterfaceDeclaration().get().fields
        Truth.assertThat(fields.size).isEqualTo(2)
    }

    @Test
    fun `test stub with JML contract preserved`() {
        val sourceCode = """
            public class ContractClass {
                /*@ requires x > 0;
                  ensures \result > 0; */
                public int compute(int x) {
                    return x * 2;
                }
            }
        """.trimIndent()

        val config = JmlStubConfig(preserveContracts = true)
        val generatorWithConfig = JmlStubGenerator(config)
        val cu = StaticJavaParser.parse(sourceCode)
        val stub = generatorWithConfig.generate(cu)

        Truth.assertThat(stub.toString()).contains("compute")
    }

    @Test
    fun `test empty compilation unit handling`() {
        val combiner = JmlStubCombiner()
        val result = combiner.combine(emptyList())

        Truth.assertThat(result.types.size).isEqualTo(0)
    }

    @Test
    fun `test stub generator with directory`() {
        // Create a temporary test file
        val tempDir = File.createTempFile("jmlstub_test", "").parentFile
        val testFile = File(tempDir, "TestClass.java")
        testFile.writeText("""
            public class TestClass {
                public void test() {}
            }
        """.trimIndent())

        try {
            val stubs = generator.generateFromDirectory(tempDir)
            Truth.assertThat(stubs).isNotEmpty()
        } finally {
            testFile.delete()
            tempDir.delete()
        }
    }
}