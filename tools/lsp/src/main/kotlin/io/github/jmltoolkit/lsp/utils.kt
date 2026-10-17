/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import org.eclipse.lsp4j.jsonrpc.messages.Either

fun <L, R> L?.asLeft() = Either.forLeft<L, R>(this)
fun <L, R> R?.asRight() = Either.forRight<L, R>(this)
