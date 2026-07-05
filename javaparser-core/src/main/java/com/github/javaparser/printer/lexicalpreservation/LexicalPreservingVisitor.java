/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import java.io.StringWriter;

public class LexicalPreservingVisitor {

    private StringWriter writer;

    public LexicalPreservingVisitor() {
        this(new StringWriter());
    }

    public LexicalPreservingVisitor(StringWriter writer) {
        this.writer = writer;
    }

    public void visit(ChildTextElement child) {
        child.accept(this);
    }

    public void visit(TokenTextElement token) {
        writer.append(token.getText());
    }

    @Override
    public String toString() {
        return writer.toString();
    }
}
