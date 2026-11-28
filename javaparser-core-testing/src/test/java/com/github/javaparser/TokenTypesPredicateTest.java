/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pins the fast kind predicates to the general category classification. */
class TokenTypesPredicateTest {

    @Test
    void predicatesMatchCategoryClassificationForEveryKind() {
        for (int kind = 0; kind < GeneratedJavaParserConstants.tokenImage.length; kind++) {
            JavaToken.Category category = TokenTypes.getCategory(kind);
            assertEquals(category.isWhitespace(), TokenTypes.isWhitespace(kind), "isWhitespace kind " + kind);
            assertEquals(category.isEndOfLine(), TokenTypes.isEndOfLineToken(kind), "isEndOfLineToken kind " + kind);
            assertEquals(
                    category.isWhitespaceButNotEndOfLine(),
                    TokenTypes.isWhitespaceButNotEndOfLine(kind),
                    "isWhitespaceButNotEndOfLine kind " + kind);
            assertEquals(category.isComment(), TokenTypes.isComment(kind), "isComment kind " + kind);
        }
    }
}
