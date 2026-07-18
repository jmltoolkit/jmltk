/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrowsExactly
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.streams.asSequence

/**
 * Tests for JmlStubGenerator functionality.
 *
 * @author Alexander Weigl
 * @version 1 (7/19/26)
 */
class JmlStubGeneratorTest {
    private val generator = StubGenerator(StubConfig())

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
        val stub = generator.run(listOf(listOf(cu))).first()
        val methodCount = stub.findAll(MethodDeclaration::class.java).size
        assertThat(methodCount).isEqualTo(3)
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
        val stub = generator.run(cu)

        val methods = stub.findAll(MethodDeclaration::class.java)
        val addMethod = methods.find { it.nameAsString == "add" }
        assertThat(addMethod).isNotNull()
        assertThat(addMethod!!.type.toString()).isEqualTo("int")
        assertThat(addMethod.parameters.size).isEqualTo(2)
    }

    @Test
    fun `test combine specifications`() {
        val specs = listOf(
            "requires x > 0;",
            "ensures \\result > 0;",
            "assigns \\nothing;"
        )

        val combined = generator.combineSpecifications(specs)

        assertThat(combined).contains("requires x > 0;")
        assertThat(combined).contains("ensures \\result > 0;")
        assertThat(combined).contains("assigns \\nothing;")
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

        val config = StubConfig(preserveContracts = true)
        val generatorWithConfig = StubGenerator(config)
        val cu = StaticJavaParser.parse(sourceCode)
        val stub = generatorWithConfig.run(cu)
        assertThat(stub.toString()).contains("compute")
    }

    @Test
    fun `test empty compilation unit handling`() {
        val combiner = JmlStubCombiner()
        assertThrowsExactly<IllegalArgumentException> {
            combiner.combine(emptyList())
        }
    }

    @Test
    fun `test with compilation unit`() {
        val combiner = ClassStubGenerator(Path("build/classes/java/test/TestClass.class"))
        val result = combiner.generate()
        val text = result.toString()
        assertThat(text).isEqualTo(
            """
            public class TestClass {

                public TestClass();

                public int getValue();

                public void setValue(int value);

                public String getName();
            }

        """.trimIndent()
        )
    }

    @Test
    fun `test stub generator with directory`() {
        // Create a temporary test file
        val tempDir = File.createTempFile("jmlstub_test", "").parentFile
        val testFile = File(tempDir, "TestClass.java")
        testFile.writeText(
            """
            public class TestClass {
                public void test() {}
            }
        """.trimIndent()
        )

        try {
            // val stubs = generator.generateFromDirectory(tempDir)
            // Truth.assertThat(stubs).isNotEmpty()
        } finally {
            testFile.delete()
            tempDir.delete()
        }
    }

    @Test
    fun `test matchesPattern with exact match`() {
        assertThat(matchesPatternForTest("com.example.MyClass", "com.example.MyClass")).isTrue()
        assertThat(matchesPatternForTest("com.example.MyClass", "com.example.OtherClass")).isFalse()
    }

    private fun matchesPatternForTest(fqdn: String, pattern: String) = matchesPattern(pattern).matches(fqdn)

    @Test
    fun `test matchesPattern with single wildcard`() {
        // * matches anything except dots
        assertThat(matchesPatternForTest("com.example.MyClass", "com.example.*")).isTrue()
        assertThat(matchesPatternForTest("com.MyClass", "*.MyClass")).isTrue()
        assertThat(matchesPatternForTest("com.example.MyClass", "com.*.MyClass")).isTrue()
        // Should not match across package boundaries with single *
        assertThat(matchesPatternForTest("com.example.MyClass", "com.*")).isFalse()
    }

    @Test
    fun `test matchesPattern with double wildcard`() {
        // ** matches anything including dots
        assertThat(matchesPatternForTest("com.example.MyClass", "**.MyClass")).isTrue()
        assertThat(matchesPatternForTest("com.example.MyClass", "com.**")).isTrue()
        assertThat(matchesPatternForTest("com.example.subpkg.MyClass", "com.**.MyClass")).isTrue()
        assertThat(matchesPatternForTest("com.deep.nested.pkg.MyClass", "**.MyClass")).isTrue()
    }

    @Test
    fun `test typeIn with list of patterns`() {
        val patterns = listOf("*.Test", "**.MyIntegrationTest", "com.example.Excluded")

        assertThat(typeInForTest("com.example.MyTest", patterns)).isFalse()  // matches *.Test
        assertThat(typeInForTest("com.example.MyIntegrationTest", patterns)).isTrue()  // matches **.IntegrationTest
        assertThat(typeInForTest("com.example.Excluded", patterns)).isTrue()  // exact match
        assertThat(typeInForTest("com.example.NormalClass", patterns)).isFalse()
    }

    private fun typeInForTest(fqdn: String, patterns: List<String>) = typeIn(fqdn, patterns.map { matchesPattern(it) })

    @Test
    fun `test matchesPattern with complex patterns`() {
        // Complex real-world patterns
        assertThat(matchesPatternForTest("org.junit.Test", "**.Test")).isTrue()
        assertThat(matchesPatternForTest("org.junit.jupiter.api.Test", "**.Test")).isTrue()
        assertThat(
            matchesPatternForTest(
                "com.google.common.util.concurrent.ListenableFuture",
                "com.google.**"
            )
        ).isTrue()
        assertThat(matchesPatternForTest("java.lang.String", "java.**")).isTrue()
        assertThat(matchesPatternForTest("javax.servlet.http.HttpServlet", "javax.**.http.*")).isTrue()
    }

    @TestFactory
    fun testJREClasses(): Sequence<DynamicTest> {
        val files = Files.list(Paths.get("src/test/resources/JREstubs/expected"))
        val target = Paths.get("src/test/resources/JREstubs/actual")

        target.createDirectories()

        return files.asSequence().map {
            DynamicTest.dynamicTest(it.name) {
                testJREClass(it, target)
            }
        }
    }

    val printer = DefaultPrettyPrinter(DefaultPrinterConfiguration())

    private fun testJREClass(it: Path, target: Path) {
        val name = it.fileName.toString()
        val clazz = Class.forName(name)
        val cu = JREClassStubGenerator(clazz).generate()
        val actual = printer.print(cu)
        target.resolve(name).writeText(actual)

        assertThat(actual).isEqualTo(
            it.readText()
        )
    }
}
