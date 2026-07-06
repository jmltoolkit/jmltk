/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.javadoc.description;

/**
 * A piece of text inside a Javadoc description.
 * <p>
 * For example in <code>A class totally unrelated to {@link String}, I swear!</code> we would have two snippets: one
 * before and one after the inline tag (<code>{@link String}</code>).
 */
public class JavadocSnippet implements JavadocDescriptionElement {

    private String text;

    public JavadocSnippet(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
    }

    @Override
    public String toText() {
        return this.text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JavadocSnippet that = (JavadocSnippet) o;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "JavadocSnippet{" + "text='" + text + '\'' + '}';
    }
}
