/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static com.github.javaparser.GeneratedJavaParserConstants.GT;

/**
 * Base class for the generated {@link Token}
 */
abstract class TokenBase {
    /**
     * For tracking the >> >>> ambiguity.
     */
    int realKind = GT;

    /**
     * This is the link to the token that JavaParser presents to the user
     */
    JavaToken javaToken = null;
}
