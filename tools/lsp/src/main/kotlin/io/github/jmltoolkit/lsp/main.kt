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
import com.google.gson.GsonBuilder
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.tinylog.Logger
import org.tinylog.configuration.Configuration
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

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
    private val executorService: ExecutorService = ForkJoinPool.commonPool()

    private val traceEnabled by option("--trace")

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
        socket.tcpNoDelay = true
        socket.keepAlive = true
        launchLanguageServer(socket.getInputStream(), socket.getOutputStream())
    }

    private fun launchLanguageServer(input: InputStream, output: OutputStream) {
        val server = JmlLanguageServer()
        val launcher = createServerLauncher(server, input, output)
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

                    socket.tcpNoDelay = true
                    socket.keepAlive = true

                    launchLanguageServer(socket.getInputStream(), socket.getOutputStream())
                }
            } catch (e: Exception) {
                Logger.error(e)
            }
        }
    }

    fun createServerLauncher(
        server: LanguageServer,
        input: InputStream,
        output: OutputStream
    ): Launcher<LanguageClient> {
        val l = LSPLauncher.Builder<LanguageClient>()
            .setLocalService(server)
            .setRemoteInterface(LanguageClient::class.java)
            .setInput(input)
            .setOutput(output)
            .setExecutorService(executorService)
            .validateMessages(true)
            // .wrapMessages(wrapper)
            .configureGson(JmlLspCommand::configureJson)
            .setClassLoader(javaClass.classLoader)

        traceEnabled?.let {
            Logger.info("Tracing enabled: {}", it)
            if (it == "-") {
                l.traceMessages(PrintWriter(System.err))
            } else {
                l.traceMessages(PrintWriter(it))
            }
        }

        return l.create()
    }

    companion object {
        @JvmStatic
        fun configureJson(builder: GsonBuilder): GsonBuilder = builder
    }
}
