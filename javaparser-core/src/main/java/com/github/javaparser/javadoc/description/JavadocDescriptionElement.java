/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.javadoc.description;

/**
 * An element of a description: either an inline tag or a piece of text.
 * <p>
 * So for example {@code a text} or <code>{@link String}</code> could be valid description elements.
 */
public interface JavadocDescriptionElement {

    String toText();
}
