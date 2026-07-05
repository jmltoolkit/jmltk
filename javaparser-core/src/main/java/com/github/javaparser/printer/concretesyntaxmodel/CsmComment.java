/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.printer.SourcePrinter;

public class CsmComment implements CsmElement {

    static void process(Comment comment, SourcePrinter printer) {
        String content = printer.normalizeEolInTextBlock(comment.getContent());
        printer.print(comment.getHeader());
        printer.print(content);
        printer.println(comment.getFooter());
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        node.getComment().ifPresent(c -> process(c, printer));
    }
}
