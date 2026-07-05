/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.javadoc.description;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavadocInlineTagTest {

    @Test
    void javadocInlineTagShouldPersistCustomTagNames() {
        String tag = JavadocInlineTag.fromText("{@foo something}").toText();

        assertEquals(tag, "{@foo something}");
    }
}
