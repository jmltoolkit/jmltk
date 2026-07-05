/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.Node;

/**
 * Printer interface defines the API for a printer.
 * A printer outputs the AST as formatted Java source code.
 */
public interface Printer {

    String print(Node node);
}
