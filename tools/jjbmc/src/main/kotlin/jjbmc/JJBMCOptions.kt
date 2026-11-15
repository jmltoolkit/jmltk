/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.math.max

/**
 * Created by jklamroth on 1/15/19.
 *
 * Holds the configuration used by the JBMC based verification. The class is intentionally free of
 * any command line parsing concerns: the CLI ([Main]) parses the command line arguments and
 * populates an instance of this class, which is then passed on to the actual logic ([Operations]).
 *
 * The handful of Java-style getters/setters ([getFileName], [setFileName], [isDebugMode], ...) are
 * part of the class' public contract: the [Operations] logic and the test suite rely on them.
 */
class JJBMCOptions {
    var apiArgs: MutableList<String> = ArrayList(10)

    /**
     * Keep the temporary file which contains the translation of the given file.
     */
    var keepTranslation: Boolean = false

    /**
     * Inline methods and unroll loops even if a contract is available.
     */
    var forceInlining: Boolean = false

    /**
     * Unroll loops even if a loop contract is available.
     */
    var forceInliningLoops: Boolean = false

    /**
     * Inline methods even if a method contract is available.
     */
    var forceInliningMethods: Boolean = false

    /**
     * Print out timing information.
     */
    var timed: Boolean = false

    /**
     * Split assertions if possible.
     */
    var splitAssertions: Boolean = true

    /**
     * Provide a timeout in ms for each jbmc call. (default 10s)
     */
    var timeout: Int = 10000

    /**
     * The method to be verified. If not provided -va is automatically added.
     */
    var functionName: String? = null

    /**
     * Prints out traces for failing pvcs.
     */
    var runWithTrace: Boolean = false

    /**
     * The jbmc binary used for the verification (relative or absolute path).
     */
    var jbmcBin: String = "jbmc"

    /**
     * Files to be copied to the translation folder.
     */
    var libFiles: Array<String> = emptyArray()

    /**
     * The javac binary used for compilation of source files manually.
     */
    var javacBin: String = "javac"

    /**
     * Allows to specify which of the contracts is going to be verified, index from 0 upwards.
     */
    var caseIdx: Int = 0

    /**
     * Adds a check for each method if assumptions are equals to false.
     */
    var doSanityCheck: Boolean = false

    /**
     * Adds additional assertions proving the preconditions of called methods while still
     * inlining them. (implies -fim option)
     */
    var proofPreconditions: Boolean = false

    /**
     * Options to be passed to jbmc.
     */
    var jbmcOptions: MutableList<String> = ArrayList()

    /**
     * Names of variables whose values should be printed in a trace. (Has to be run with -tr option)
     */
    var relevantVars: MutableList<String> = ArrayList()

    private var maxArraySizeValue = -1
    private var debugModeFlag = false
    private var fileNameValue: Path? = null
    private var unwindValue = -1
    private var fullTraceRequestedFlag = false

    private var tmpFolderValue: Path? = null
    private var tmpFileValue: Path? = null

    private val isWindowsValue =
        System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("windows")

    @Suppress("unused")
    private val expressionMap: MutableMap<String?, String?> = HashMap()

    fun reset() {
        timeout = 10000
        timed = false
        debugModeFlag = false
        keepTranslation = false
        functionName = null
        forceInlining = false
        forceInliningMethods = false
        forceInliningLoops = false
        runWithTrace = false
        unwindValue = -1
        maxArraySizeValue = -1
        caseIdx = 0
        jbmcOptions = ArrayList()
        fullTraceRequestedFlag = false
        relevantVars = ArrayList()
    }

    fun isWindows(): Boolean = isWindowsValue

    fun isDebugMode(): Boolean = debugModeFlag

    fun setDebugMode(debugMode: Boolean) {
        debugModeFlag = debugMode
    }

    fun isFullTraceRequested(): Boolean = fullTraceRequestedFlag

    fun setFullTraceRequested(fullTraceRequested: Boolean) {
        fullTraceRequestedFlag = fullTraceRequested
    }

    fun getFileName(): Path = fileNameValue ?: error("No file name was set.")

    fun setFileName(fileName: Path?) {
        fileNameValue = fileName
    }

    fun getTmpFolder(): Path {
        if (tmpFolderValue == null) {
            val p = getFileName().resolveSibling("tmp")
            setTmpFolder(p)
            return p
        }
        return tmpFolderValue!!
    }

    fun setTmpFolder(tmpFolder: Path?) {
        tmpFolderValue = tmpFolder
    }

    fun getTmpFile(): Path {
        if (tmpFileValue == null) {
            val p = getTmpFolder().resolve(getFileName().fileName)
            setTmpFile(p)
            return p
        }
        return tmpFileValue!!
    }

    fun setTmpFile(tmpFile: Path?) {
        tmpFileValue = tmpFile
    }

    fun getUnwinds(): Int {
        if (unwindValue < 0) {
            ErrorLogger.info("No unwind argument found. Default to 7.")
            unwindValue = 7
        }
        return unwindValue
    }

    fun setUnwinds(unwinds: Int) {
        unwindValue = unwinds
    }

    fun getMaxArraySize(): Int {
        if (maxArraySizeValue < 0) {
            ErrorLogger.info("No maxArraySize argument found. Default to " + (unwindValue - 2) + ".")
            setMaxArraySize(max(unwindValue - 2, 0))
        }
        return maxArraySizeValue
    }

    fun setMaxArraySize(maxArraySize: Int) {
        maxArraySizeValue = maxArraySize
    }

    val javacBinary: Path
        get() = Objects.requireNonNull<Path>(getPath(javacBin), "Could not find javac on \$PATH")

    val jbmcBinary: Path
        get() = Objects.requireNonNull<Path>(getPath(jbmcBin), "Could not find jbmc on \$PATH")

    private fun getPath(binary: String): Path? = System.getenv("PATH").split(File.pathSeparator.toRegex()).asSequence()
            .filter { it.isNotBlank() }
            .map { Paths.get(it, binary) }
            .filter { it.exists() }
            .map { it.absolute() }
            .firstOrNull()

    companion object {
        const val jbmcMajorVer: Int = 5
        const val jbmcMinorVer: Int = 22
    }
}
