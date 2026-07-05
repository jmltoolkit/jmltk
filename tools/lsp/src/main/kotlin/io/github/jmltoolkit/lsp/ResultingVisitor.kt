/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp

import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/**
 * Defines a visitor which produces a result of type `T`.
 * @author Alexander Weigl
 * @version 1 (20.07.22)
 */
abstract class ResultingVisitor<T> : VoidVisitorAdapter<Unit?>() {
    abstract val result: T
}
