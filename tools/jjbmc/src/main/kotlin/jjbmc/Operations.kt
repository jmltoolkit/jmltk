/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package jjbmc

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import jjbmc.jml2java.Jml2JavaFacade
import jjbmc.trace.TraceParser
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.*
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

class Operations(val options: JJBMCOptions) : Callable<Int> {
    private var jbmcProcess: Process? = null
    private var jbmcOptions: MutableList<String?> = LinkedList<String?>()
    private var didCleanUp = false

    @Throws(Exception::class)
    fun translateAndRunJBMC(file: Path?, functionName: String?) {
        options.functionName = functionName
        options.setFileName(file)
        translateAndRunJBMC()
    }

    @Throws(Exception::class)
    fun prepareSource() {
        didCleanUp = false

        if (options.getMaxArraySize() > options.getUnwinds() - 2) {
            ErrorLogger.warn("Unwinds is set to less than maxArraySize + 2. This may lead to unsound behaviour.")
        }

        val file = options.getFileName()
        if (!Files.exists(file)) {
            throw FileNotFoundException("Could not find file " + file)
        }

        val tmpFile = options.getTmpFile()

        try {
            deleteFolder(options.getTmpFolder(), true)
            Files.createDirectories(options.getTmpFolder())

            /*var tmpClassFile = options.getTmpFolder().resolve(
                    file.getFileName().toString()
                            .replace(".java", ".class"));

            Files.deleteIfExists(tmpFile);
            Files.deleteIfExists(tmpClassFile);
             */
            copyLibraryFiles(options.getTmpFolder())
            createCProverFolder(options.getTmpFolder())
            Companion.copySubjectOfVerification(file, tmpFile)

            val start = System.currentTimeMillis()
            val translation: CompilationUnit = translate(options, tmpFile)
            val finish = System.currentTimeMillis()
            ErrorLogger.debug("Translation.Translation took: " + (finish - start) + "ms")

            var packageName: String = translation.packageDeclaration()?.nameAsString ?: ""
            Files.deleteIfExists(tmpFile)
            packageName = packageName.replace(".", "/")
            val packageFolder = options.getTmpFolder().resolve(packageName)
            Files.createDirectories(packageFolder)
            options.setTmpFile(packageFolder.resolve(tmpFile.getFileName()))
            val content = Jml2JavaFacade.pprint(translation)
            Files.writeString(options.getTmpFile(), content, StandardOpenOption.CREATE)
        } finally {
            options.keepTranslation = true
            cleanUp()
        }
    }

    @Throws(Exception::class)
    fun compile() {
        if (!compileWithApi()) {
            compileWithJavac()
        }
    }

    @Throws(IOException::class)
    private fun compileWithApi(): Boolean {
        val javac = ToolProvider.getSystemJavaCompiler()
        if (javac == null) return false

        val diagnostics = DiagnosticCollector<JavaFileObject?>()
        val fileManager = javac.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.defaultCharset())

        Files.walk(options.getTmpFolder()).use { s ->
            val files = s.filter { f: Path? -> !Files.isDirectory(f) }
                .filter { f: Path? -> f!!.fileName.toString().endsWith(".java") }
                .toList()
            val compilationUnits =
                fileManager.getJavaFileObjects(*files.toTypedArray<Path?>())

            val task = javac.getTask(
                PrintWriter(System.out),
                fileManager,
                diagnostics,
                mutableListOf<String?>("-g"),
                mutableListOf<String?>(),
                compilationUnits
            )

            val start = System.currentTimeMillis()
            val b = task.call()
            val stop = System.currentTimeMillis()

            ErrorLogger.info("Compilation took %d ms using the internal API", stop - start)
            for (diagnostic in diagnostics.getDiagnostics()) {
                ErrorLogger.info("%s", diagnostic)
            }
        }
        return true
    }

    @Throws(Exception::class)
    private fun compileWithJavac() {
        val tmpFile = options.getTmpFile()
        val commands = ArrayList<String?>(
            listOf(
                options.javacBinary.toString(),
                "-g",
                options.getTmpFolder().relativize(tmpFile).toString()
            )
        )
        commands.addAll(options.apiArgs)

        ErrorLogger.debug("Compiling translated file: " + commands)
        val out = ProcessBuilder.Redirect.to(
            options.getTmpFolder().resolve("compilationErrors.txt").toFile()
        )
        val pb = ProcessBuilder(commands)
            .redirectOutput(out)
            .redirectError(out)
            .directory(options.getTmpFolder().toFile())

        val proc = pb.start()
        proc.waitFor()
        if (proc.exitValue() != 0) {
            options.keepTranslation = true
            throw Exception("Compilation failed. See compilationErrors.txt for javac output.")
        }

        ErrorLogger.debug("Compilation successful.")

        if (!JbmcFacade.verifyJBMCVersion(options.jbmcBin, options.isWindows())) {
            throw Exception("Unverified JBMC version")
        }
    }

    @Throws(IOException::class)
    private fun copyLibraryFiles(fileName: Path) {
        for (s in options.libFiles) {
            val tmpF = fileName.resolveSibling(s)
            if (!Files.exists(tmpF)) {
                throw FileNotFoundException("Could not find libFile: " + tmpF)
            } else {
                copySubjectOfVerification(tmpF, options.getTmpFolder().resolve(tmpF.fileName))
            }
        }
    }

    @Throws(Exception::class)
    fun translateAndRunJBMC() {
        prepareSource()
        compile()

        val fnv = FunctionNameVisitor.parseFile(options.getTmpFile(), true)
        var functionNames: List<String> = fnv.functionNames
        val paramMap = fnv.paramMap

        val allFunctionNames = arrayListOf(functionNames)

        if (options.functionName != null) {
            if (!options.functionName.endsWith("Verification")) {
                options.functionName += "Verification"
            }
            functionNames = functionNames
                .filter { f -> f.contains("." + options.functionName + ":") }
                .toList()
            if (functionNames.isEmpty()) {
                ErrorLogger.warn("Function " + options.functionName + " could not be found in the specified file.")
                ErrorLogger.warn("Found the following functions: %s", allFunctionNames)
                return
            }
        }
        ErrorLogger.info("Run jbmc for " + functionNames.size + " functions.")

        for (functionName in functionNames) {
            var functionName = functionName
            if (options.isWindows()) {
                if (functionName.contains("()")) {
                    functionName = functionName.replace("<init>", "<clinit>")
                }
                functionName = "\"" + functionName + "\""
            }

            try {
                Executors.newFixedThreadPool(1).use { executerService ->
                    val finalFunctionName: String = functionName
                    val worker = Runnable { runJBMC(finalFunctionName, paramMap) }
                    val handler = executerService.submit(worker)
                    handler.get(options.timeout.toLong(), TimeUnit.MILLISECONDS)
                }
            } catch (e: TimeoutException) {
                if (jbmcProcess != null) {
                    jbmcProcess!!.destroyForcibly()
                }
                ErrorLogger.info(ErrorLogger.YELLOW_BOLD + "JBMC call for function " + functionName + " timed out." + ErrorLogger.RESET + "\n")
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }
        }
    }

    fun printOutput(@Nullable output: JBMCOutput?, time: Long, functionName: String?) {
        if (output == null) {
            options.keepTranslation = true
            ErrorLogger.error("Error parsing xml-output of JBMC.")
            return
        }
        if (options.doSanityCheck) {
            if (output.printStatus().contains("SUCC")) {
                ErrorLogger.warn("Sanity check failed for: " + functionName)
            } else {
                ErrorLogger.info("Sanity check ok for function: " + functionName)
            }
            return
        }
        ErrorLogger.info("Result for function " + functionName + ":")
        if (options.timed) {
            ErrorLogger.info("JBMC took " + time + "ms.")
        }

        if (output.getErrors().isEmpty()) {
            if (options.runWithTrace) {
                val traces = output.printAllTraces()
                if (!traces.isEmpty()) {
                    ErrorLogger.info(traces)
                }
            }
            // Arrays.stream(traces.split("\n")).forEach(s -> log.info(s));
            val status = output.printStatus()
            if (status.contains("SUCC")) {
                ErrorLogger.info(ErrorLogger.GREEN_BOLD + status + ErrorLogger.RESET)
            } else {
                ErrorLogger.info(ErrorLogger.RED_BOLD + status + ErrorLogger.RESET)
            }
        } else {
            options.keepTranslation = true
            ErrorLogger.error(output.printErrors())
        }
    }

    fun runJBMC(functionName: String?, paramMap: MutableMap<String?, MutableList<String?>?>?) {
        try {
            ErrorLogger.debug("Running jbmc for function: " + functionName)
            var classFile = options.getTmpFile().getFileName().toString().replace(".java", "")
            classFile = classFile.substring(classFile.lastIndexOf(File.separator + "tmp") + 5)

            // classFile = "." + classFile;
            val tmp = ArrayList<String?>()
            if (options.isWindows()) {
                tmp.add("cmd.exe")
                tmp.add("/c")
                classFile = classFile.replace("\\\\".toRegex(), "/")
            }
            tmp.add("jbmc")
            tmp.add(classFile)
            tmp.add("--function")
            tmp.add(functionName)
            tmp.add("--unwind")
            tmp.add(options.getUnwinds().toString())
            tmp.add("--max-nondet-array-length")
            tmp.add(options.getMaxArraySize().toString())

            jbmcOptions = prepareJBMCOptions(options.getJbmcOptions())
            tmp.addAll(options.getJbmcOptions())
            tmp.add("--xml-ui")
            // tmp.add("--cp");
            val libPath = System.getProperty("java.library.path")
            // tmp.add(libPath);
            var commands = arrayOfNulls<String>(tmp.size)
            commands = tmp.toArray<String?>(commands)

            ErrorLogger.debug(commands.contentToString())
            val rt = Runtime.getRuntime()
            rt.addShutdownHook(
                Thread(
                    Runnable {
                try {
                    cleanUp()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
                )
            )
            val start = System.currentTimeMillis()

            jbmcProcess = rt.exec(commands, null, options.getTmpFolder().toFile())

            val stdInput = BufferedReader(InputStreamReader(jbmcProcess!!.inputStream))

            val stdError = BufferedReader(InputStreamReader(jbmcProcess!!.errorStream))

            val sb = StringBuilder()
            var line = stdInput.readLine()
            while (line != null) {
                sb.append(line)
                sb.append(System.lineSeparator())
                line = stdInput.readLine()
            }

            if (Thread.interrupted()) {
                return
            }

            // Has to stay down here otherwise not reading the output may block the process
            jbmcProcess!!.waitFor()
            val end = System.currentTimeMillis()

            val xmlOutput = sb.toString()

            // String error = sb2.toString();
            if ((jbmcProcess!!.exitValue() != 0 && jbmcProcess!!.exitValue() != 10) || options.keepTranslation) {
                options.keepTranslation = true
                Files.writeString(options.getTmpFolder().toAbsolutePath().resolve("xmlout.xml"), xmlOutput)
                if (jbmcProcess!!.exitValue() != 0 && jbmcProcess!!.exitValue() != 10) {
                    ErrorLogger.error(
                        (
                            "JBMC did not terminate as expected for function: " + functionName +
                            "\nif ran with -kt option jbmc output can be found in xmlout.xml in the tmp folder"
                        )
                    )
                    return
                }
            } else {
                ErrorLogger.debug("JBMC terminated normally.")
            }

            if ((options.isFullTraceRequested() || !options.getRelevantVars().isEmpty()) && !options.runWithTrace) {
                options.runWithTrace = true
                ErrorLogger.warn(
                    "Options concerning the trace where found but not -tr option was given. \"-tr\" was automatically added."
                )
            }

            if (xmlOutput.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
                val start1 = System.currentTimeMillis()
                val output: JBMCOutput = TraceParser.parse(xmlOutput, options.runWithTrace)
                printOutput(output, end - start, functionName)
                val duration = System.currentTimeMillis() - start1
                ErrorLogger.debug("Parsing xml took: " + duration + "ms.")
            } else {
                ErrorLogger.error("Unexpected jbmc output:")
                ErrorLogger.error(xmlOutput)
            }
        } catch (e: Exception) {
            ErrorLogger.error("Error running jbmc.")
            options.keepTranslation = true
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun cleanUp() {
        if (!didCleanUp && !options.keepTranslation) {
            deleteFolder(options.getTmpFolder(), false)
            if (!options.keepTranslation) {
                try {
                    Files.deleteIfExists(options.getTmpFolder())
                } catch (e: IOException) {
                    // log.info("Could not delete tmp folder.");
                }
            }
        }
        didCleanUp = true
    }

    private fun verifyJavaVersion(binary: String?): Boolean {
        val commands = arrayOf<String?>(binary, "-version")
        val p: Process
        try {
            val pb = ProcessBuilder().command(*commands).redirectErrorStream(true)
            p = pb.start()
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val line = reader.readLine()
            if (line != null) {
                return line.contains("1.8")
            }
        } catch (e: IOException) {
            return false
        }
        return false
    }

    @Throws(Exception::class)
    override fun call(): Int {
        if (options.isDebugMode()) {
            ErrorLogger.setDebugOn()
            options.keepTranslation = true
        }
        if (options.forceInlining) {
            options.forceInliningLoops = true
            options.forceInliningMethods = true
        }

        val f = options.getFileName()
        translateAndRunJBMC()
        if (options.doSanityCheck) {
            options.doSanityCheck = false
            translateAndRunJBMC()
        }
        return 0
    }

    companion object {
        @Throws(Exception::class)
        fun translate(file: File, options: JJBMCOptions): CompilationUnit = translate(options, file.toPath())

        @Throws(Exception::class)
        fun translate(options: JJBMCOptions, fileName: Path?): CompilationUnit {
            val config: ParserConfiguration = ParserConfiguration()
            config.setJmlKeys(ImmutableList.of(ImmutableList.of("openjml")))
            config.setProcessJml(true)
            config.setSymbolResolver(
                JavaSymbolSolver(
                    TypeSolverBuilder()
                        .withSourceCode(options.getTmpFolder())
                        .withCurrentJRE()
                        .build()
                )
            )
            val parser: JavaParser = JavaParser(config)

            val compilationUnits: MutableList<CompilationUnit?> = ArrayList<CompilationUnit?>(32)
            val result: ParseResult<CompilationUnit?> = parser.parse(fileName)
            if (result.isSuccessful()) {
                val compilationUnit =
                    result.getResult().get()
                return rewriteAssert(compilationUnit, options)
            } else {
                result.getProblems().forEach(System.out::println)
                val first =
                    result.getProblems().get(0)
                throw RuntimeException(
                    first.getVerboseMessage(), first.getCause().orElse(null)
                )
            }
        }

        @Throws(IOException::class)
        private fun copySubjectOfVerification(fileName: Path, tmpFile: Path) {
            Files.copy(fileName, tmpFile, StandardCopyOption.REPLACE_EXISTING)
        }

        @Throws(IOException::class)
        private fun createCProverFolder(folder: Path) {
            val dir = folder.resolve("org/cprover")
            ErrorLogger.debug("Copying CProver.java to %s", dir.toAbsolutePath())
            Files.createDirectories(dir)
            JJBMCOptions::class.java.getResourceAsStream("/cli/CProver.java").use { `is` ->
                Files.copy(Objects.requireNonNull<InputStream?>(`is`), dir.resolve("CProver.java"))
            }
        }

        private fun prepareJBMCOptions(options: MutableList<String>): MutableList<String?> {
            val res: MutableList<String?> = ArrayList<String?>()
            for (s in options) {
                res.addAll(listOf<String>(*s.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
            return res
        }

        @Throws(IOException::class)
        fun deleteFolder(folder: Path, all: Boolean) {
            if (Files.exists(folder)) {
                Files.walk(folder).use { walk ->
                    walk.sorted(Comparator.reverseOrder<Path>()).forEach { path: Path? ->
                        try {
                            Files.deleteIfExists(path)
                        } catch (ignored: IOException) {
                        }
                    }
                }
            }
            /*File[] tmpFiles = folder.listFiles();
        if (tmpFiles != null) {
            for (File f : tmpFiles) {
                if (!keepTranslation || fileName != null && !f.getName().endsWith(new File(fileName).getName()) || all) {
                    if (f.isDirectory()) {
                        if (!f.getName().contains("testannotations")) {
                            deleteFolder(f, all);
                        }
                    }
                    try {
                        if (f.exists()) {
                            Files.delete(f.toPath());
                        }
                    } catch (IOException ex) {
                        //log.info("Could not delete temporary file: " + f.getAbsolutePath());
                    }
                }
            }
        }*/
        }

        fun rewriteAssert(cu: CompilationUnit, options: JJBMCOptions?): CompilationUnit = Jml2JavaFacade.translate(cu, options)
    }
}
