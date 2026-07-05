/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.resolution.UnsolvedSymbolException;

/**
 * @author Alexander Weigl
 * @version 1 (08.07.22)
 */
public class JavaRefersToJmlException extends UnsolvedSymbolException {
    public JavaRefersToJmlException(String name) {
        super(name);
    }

    public JavaRefersToJmlException(String name, String context) {
        super(name, context);
    }

    public JavaRefersToJmlException(String name, Throwable cause) {
        super(name, cause);
    }

    public JavaRefersToJmlException(String name, String context, Throwable cause) {
        super(name, context, cause);
    }
}
