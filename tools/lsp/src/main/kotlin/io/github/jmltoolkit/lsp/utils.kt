package io.github.jmltoolkit.lsp

import org.eclipse.lsp4j.jsonrpc.messages.Either

fun <L, R> L?.asLeft() = Either.forLeft<L, R>(this)
fun <L, R> R?.asRight() = Either.forRight<L, R>(this)
