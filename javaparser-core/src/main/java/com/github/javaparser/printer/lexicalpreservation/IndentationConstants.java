/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

/**
 * Constants related to indentation management in lexical preservation.
 *
 * This class centralizes all indentation-related constants to avoid duplication
 * and ensure consistency across IndentationContext and IndentationCalculator.
 */
public final class IndentationConstants {

    /**
     * Standard indentation size in spaces.
     * This is the number of spaces added or removed when increasing/decreasing indentation.
     */
    public static final int STANDARD_INDENTATION_SIZE = 4;

    /**
     * Private constructor to prevent instantiation.
     * This is a constants class and should not be instantiated.
     */
    private IndentationConstants() {
        throw new AssertionError("IndentationConstants is a constants class and should not be instantiated");
    }
}
