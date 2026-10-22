/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path

/**
 * The entry point for the program. Initializes the clikt based command line and runs the
 * verification logic.
 *
 * @author jklamroth
 * @version 1 (1.10.18)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        JJBMCCliCommand().main(args)
    }
}

/**
 * The clikt command line interface for openJBMC. Parses all options/arguments and
 * forwards them to the actual verification logic via a freshly created [JJBMCOptions].
 *
 * @author jklamroth
 */
class JJBMCCliCommand : CliktCommand(name = "openJBMC") {

    override fun help(context: Context): String = "openJBMC Bounded Model checking for JML"

    init {
        // keep the picocli-compatible -h / -help aliases for the usage help
        context { helpOptionNames = setOf("--help", "-h", "-help") }
    }

    private val keepTranslation by option(
        "--keepTranslation", "-kt", "-keepTranslation",
        help = "Keep the temporary file which contains the translation of the given file."
    ).flag()

    private val forceInlining by option(
        "--forceInlining", "-fi", "-forceInlining",
        help = "Inline methods and unroll loops even if a contract is available."
    ).flag()

    private val forceInliningLoops by option(
        "--forceInliningLoopsOnly", "-fil", "-forceInliningLoopsOnly",
        help = "Unroll loops even if a loop contract is available."
    ).flag()

    private val forceInliningMethods by option(
        "--forceInliningMethodsOnly", "-fim", "-forceInliningMethodsOnly",
        help = "Inline methods even if a method contract is available."
    ).flag()

    private val timed by option(
        "--clock", "-c", "-clock",
        help = "Print out timing information."
    ).flag()

    private val dontSplitAssertions by option(
        "--dontsplitasserts", "-dsa", "-dontsplitasserts",
        help = "Split assertions if possible."
    ).flag()

    private val timeout by option(
        "--timeout", "-t", "-timeout",
        help = "Provide a timeout in ms for each jbmc call. (default 10s)"
    ).int().default(10000)

    private val runWithTrace by option(
        "--trace", "-tr", "-trace",
        help = "Prints out traces for failing pvcs."
    ).flag()

    private val jbmcBin by option(
        "--jbmcBinary", "-jbmc", "-jbmcBinary",
        help = "allows to set the jbmc binary that is used for the verification (has to be relative or absolute path no alias)"
    ).default("jbmc")

    private val libFiles by option(
        "--libFiles", "-lf",
        help = "Files to be copied to the translation folder."
    ).multiple()

    private val javacBin by option(
        "--javac", "-jc", "-javac",
        help = "allows to set the javac binary that is used for compilation of source files manually"
    ).default("javac")

    private val contractIndex by option(
        "--contractIndex", "-ci", "-contractIndex",
        help = "Allows to specify which of the contracts is going to be verified, index from 0 upwards"
    ).int().default(0)

    private val maxArraySize by option(
        "--maxArraySize", "-mas", "-maxArraySize",
        help = "Sets the maximum size for nondeterministic arrays."
    ).int().default(-1)

    private val doSanityCheck by option(
        "--sanityCheck", "-sc", "-sanityCheck",
        help = "Adds a check for each method if assumptions are equals to false."
    ).flag()

    private val debug by option(
        "--debug", "-d", "-debug",
        help = "Runs JJBMC in debug mode. More outputs and preventing clean up of temporary files."
    ).flag()

    private val unroll by option(
        "--unwind", "-u", "-unwind",
        help = "Number of times loops are unwound. (default 5)"
    ).int().default(-1)

    private val jbmcOptions by option(
        "--jbmcOptions", "-j", "-jbmcOptions",
        help = "Options to be passed to jbmc."
    ).multiple()

    private val relevantVars by option(
        "--relevantVar", "-rv", "-relevantVar",
        help = "Names of variables whose values should be printed in a trace. (Has to be run with -tr option)"
    ).multiple()

    private val fullTrace by option(
        "--fullTrace", "-ft", "-fullTrace",
        help = "Prevents traces from being filtered for relevant variables and prints all values. (Has to be run with -tr option)"
    ).flag()

    private val proofPreconditions by option(
        "--proofPreconditions", "-pp", "-proofPreconditions",
        help = "Adds additional assertions proving the preconditions of called methods while still inlining them. (implies -fim option)"
    ).flag()

    private val file by argument("FILE", help = "The file containing methods to be verified.")
        .path()

    private val functionName by argument("METHOD", help = "The method to be verified. If not provided -va is automatically added.")
        .optional()

    override fun run() {
        val options = JJBMCOptions()
        options.keepTranslation = keepTranslation
        options.forceInlining = forceInlining
        options.forceInliningLoops = forceInliningLoops
        options.forceInliningMethods = forceInliningMethods
        options.timed = timed
        options.splitAssertions = !dontSplitAssertions
        options.timeout = timeout
        options.functionName = functionName
        options.runWithTrace = runWithTrace
        options.jbmcBin = jbmcBin
        options.libFiles = libFiles.toTypedArray()
        options.javacBin = javacBin
        options.caseIdx = contractIndex
        options.setMaxArraySize(maxArraySize)
        options.doSanityCheck = doSanityCheck
        options.setDebugMode(debug)
        options.setFileName(file)
        options.setUnwinds(unroll)
        options.jbmcOptions = jbmcOptions.toMutableList()
        options.relevantVars = relevantVars.toMutableList()
        options.setFullTraceRequested(fullTrace)
        options.proofPreconditions = proofPreconditions

        val exitCode = Operations(options).call()
        if (exitCode != 0) {
            throw ProgramResult(exitCode)
        }
    }
}
