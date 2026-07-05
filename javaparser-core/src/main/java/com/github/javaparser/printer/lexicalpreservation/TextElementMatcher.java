/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

public interface TextElementMatcher {

    boolean match(TextElement textElement);

    /**
     * This allows the combination of different TextElementMatcher instances.<br>
     * If combined, all of the TextElementMatchers have to return true.
     *
     * @param textElementMatcher TextElementMatcher to combine with this one
     * @return A new and combined TextElementMatcher of this and textElementMatcher
     */
    default TextElementMatcher and(TextElementMatcher textElementMatcher) {
        return textElement -> this.match(textElement) && textElementMatcher.match(textElement);
    }
}
