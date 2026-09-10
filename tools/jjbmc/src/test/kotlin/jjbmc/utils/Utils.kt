/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Problem
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.TypeSolverBuilder
import jjbmc.ErrorLogger.debug
import jjbmc.ErrorLogger.info
import jjbmc.FunctionNameVisitor.TestBehaviour
import jjbmc.JJBMCOptions
import jjbmc.Operations
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.function.Consumer

/**
 * Created by jklamroth on 12/3/18.
 */
object Utils {
    val SRC_TEST_JAVA: Path = Paths.get("src", "test", "java")

    val TMP_FOLDER: Path = Paths.get("tmp")

    @JvmField
    val SRC_TEST_RESOURCES: Path = SRC_TEST_JAVA.getParent().resolve("resources")

    private const val filterOutput = false

    @JvmStatic
    @Throws(Exception::class)
    fun prepareParameters(fileName: Path): Sequence<JJBMCTest> {
        val options = JJBMCOptions()
        options.keepTranslation = true
        options.setDebugMode(true)
        options.setFileName(fileName)
        options.setTmpFolder(
            TMP_FOLDER.resolve(fileName.getFileName().toString().replace(".java", ""))
        )

        createAnnotationsFolder(options.getTmpFolder())

        val operations = Operations(options)
        operations.prepareSource()
        operations.compile()

        debug("Parsing file for functions.")
        val config = ParserConfiguration()
        config.setJmlKeys(listOf(listOf("openjml")))
        config.setProcessJml(true)
        config.setSymbolResolver(
            JavaSymbolSolver(
                TypeSolverBuilder()
                    .withSourceCode(options.getTmpFolder())
                    .withCurrentJRE()
                    .build()
            )
        )
        val parser = JavaParser(config)

        val result: ParseResult<CompilationUnit?>?
        try {
            result = parser.parse(fileName)
        } catch (e: IOException) {
            println("Error parsing file: " + fileName)
            throw RuntimeException(e)
        }

        if (!result.isSuccessful()) {
            println(fileName)
            result.getProblems().forEach(Consumer { x: Problem? -> println(x) })
            return emptySequence()
        }

        val testOptions = ArrayList<TestOptions>(32)
        result.getResult().get().accept(TestOptionsListener(), testOptions)
        val params = testOptions.asSequence()
            .filter { it.behaviour != TestBehaviour.Ignored }
            .map { JJBMCTest(operations, it) }
        debug("Found %s functions", testOptions.size)
        return params
    }

    @Throws(IOException::class)
    private fun createAnnotationsFolder(path: Path) {
        val dir = path.resolve("jjbmc")
        info("Copying Annotation files to %s", dir.toAbsolutePath())

        Files.createDirectories(dir)

        Files.copy(
            SRC_TEST_JAVA.resolve("jjbmc/Fails.java"),
            dir.resolve("Fails.java"),
            StandardCopyOption.REPLACE_EXISTING
        )

        Files.copy(
            SRC_TEST_JAVA.resolve("jjbmc/Verifyable.java"),
            dir.resolve("Verifyable.java"),
            StandardCopyOption.REPLACE_EXISTING
        )

        Files.copy(
            SRC_TEST_JAVA.resolve("jjbmc/Unwind.java"),
            dir.resolve("Unwind.java"),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    @JvmStatic
    @Throws(InterruptedException::class, IOException::class)
    fun runTests(test: JJBMCTest) {
        val topts = test.topts
        val opts = test.op.options

        var function = topts.functionName
        val classFile = Objects.requireNonNull<Path>(opts.getTmpFile())
        val unwind = topts.unwinds

        if (topts.behaviour == TestBehaviour.Ignored) {
            Assumptions.abort<Any>("Function: $function ignored due to missing annotation.")
        }

        info("Running test for function: %s", function)

        val commandList: MutableList<String?> = ArrayList<String?>()
        if (opts.isWindows()) {
            if (function.contains("()")) {
                function = function.replace("<init>", "<clinit>")
            }
            function = "\"$function\""
            // classFile = classFile.replaceAll("\\\\", "/");
            commandList.add("cmd.exe")
            commandList.add("/c")
        }

        commandList.add(opts.jbmcBinary.toString())
        commandList.add(classFile.toAbsolutePath().toString())
        commandList.add("--function")
        commandList.add(function)

        if (unwind != -1) {
            commandList.add("--unwind")
            commandList.add(unwind.toString())
        }

        info("Run jbmc with commands: %s", commandList)

        val parentDir = opts.getTmpFolder()

        val proc =
            ProcessBuilder(commandList).directory(parentDir.toFile()).start()

        val stdInput = BufferedReader(InputStreamReader(proc.getInputStream()))

        val stdError = BufferedReader(InputStreamReader(proc.getErrorStream()))
        proc.waitFor()

        val out = stdInput.lines().toList()
        val errors = stdError.lines().toList()

        System.out.format("JBMC Output for file: %s with function %s%n", classFile, function)
        out.stream()
            .filter { s: String? -> !filterOutput || s!!.contains("**") || s.contains("FAILURE") || s.contains("VERIFICATION") }
            .forEach { x: String? -> println(x) }
        errors.forEach(Consumer { x: String? -> println(x) })

        /*
                if (!filterOutput) {
            info(out);
            info(errOut);
        }*/
        val behaviour = topts.behaviour
        Assertions.assertFalse(out.contains("FAILURE") && behaviour == TestBehaviour.Verifyable)
        Assertions.assertFalse(out.contains("SUCCESSFUL") && behaviour == TestBehaviour.Fails)
        Assertions.assertTrue(out.contains("VERIFICATION"))
    }
}
