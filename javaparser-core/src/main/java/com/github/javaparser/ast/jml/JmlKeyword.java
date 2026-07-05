/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml;

import com.github.javaparser.printer.Stringable;

/**
 * @author Alexander Weigl
 * @version 1 (3/20/21)
 */
public interface JmlKeyword extends Stringable {

    String jmlSymbol();

    @Override
    default String asString() {
        return jmlSymbol();
    }
}
