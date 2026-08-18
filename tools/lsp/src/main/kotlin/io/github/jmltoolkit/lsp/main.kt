/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.tinylog.Logger
import org.tinylog.configuration.Configuration
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * @author Alexander Weigl
 * @version 1 (10.07.22)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        JmlLspCommand().main(args)
    }
}

class JmlLspCommand : CliktCommand() {
    private val stdioMode by option("--stdio").flag()
    private val serverMode by option("--server").int()
    private val client by option("--client").int()

    override fun run() {
        Configuration.set("writer.console", "disabled")
        Configuration.set("writer2", "file")
        Configuration.set("writer2.level", "debug")
        Configuration.set("writer2.file", "/tmp/lsp.log")
        Configuration.set("writer2.format", "{date} {class}.{method}(): {message}")

        try {
            when {
                stdioMode -> launchLanguageServer(System.`in`, System.out)
                serverMode != null -> runAsServer(serverMode!!)
                client != null -> runAsClient(client!!)
            }
        } catch (e: Exception) {
            Logger.error(e)
        }
    }

    private fun runAsClient(port: Int) {
        val socket = Socket("localhost", port)
        launchLanguageServer(socket.getInputStream(), socket.getOutputStream())
    }

    private fun launchLanguageServer(input: InputStream, output: OutputStream) {
        val teeInput = TeeInputStream(input, "/tmp/in.txt")
        val teeOutput = TeeOutputStream(output, "/tmp/out.txt")
        val server = JmlLanguageServer()
        val launcher = LSPLauncher.createServerLauncher(server, teeInput, teeOutput)
        val client: LanguageClient = launcher.remoteProxy
        server.connect(client)
        launcher.startListening()
    }

    private fun runAsServer(port: Int) {
        while (true) {
            try {
                ServerSocket(port, 1, InetAddress.getLoopbackAddress()).use { serverSocket ->
                    Logger.info("Listening on {}", serverSocket.localSocketAddress)
                    val socket = serverSocket.accept()
                    launchLanguageServer(socket.getInputStream(), socket.getOutputStream())
                }
            } catch (e: Exception) {
                Logger.error(e)
            }
        }
    }
}

class TeeInputStream(private val inputStream: InputStream, logPath: String) : InputStream() {
    private val logStream = FileOutputStream(logPath)

    override fun read(): Int {
        val r = inputStream.read()
        if (r != -1) {
            logStream.write(r)
        }
        return r
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val result = inputStream.read(b, off, len)
        if (result > 0) {
            logStream.write(b, off, result)
        }
        return result
    }

    override fun available(): Int = inputStream.available()

    override fun close() {
        inputStream.close()
        logStream.close()
    }
}

class TeeOutputStream(private val outputStream: OutputStream, logPath: String) : OutputStream() {
    private val logStream = FileOutputStream(logPath)

    override fun write(b: Int) {
        outputStream.write(b)
        logStream.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        outputStream.write(b, off, len)
        logStream.write(b, off, len)
    }

    override fun flush() {
        outputStream.flush()
        logStream.flush()
    }

    override fun close() {
        outputStream.close()
        logStream.close()
    }
}
