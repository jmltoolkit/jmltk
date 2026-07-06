/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.printer.SourcePrinter;
import java.util.LinkedList;
import java.util.List;
import static com.github.javaparser.utils.PositionUtils.sortByBeginPosition;

public class CsmOrphanCommentsEnding implements CsmElement {

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        List<Node> everything = new LinkedList<>();
        everything.addAll(node.getChildNodes());
        sortByBeginPosition(everything);
        if (everything.isEmpty()) {
            return;
        }
        int commentsAtEnd = 0;
        boolean findingComments = true;
        while (findingComments && commentsAtEnd < everything.size()) {
            Node last = everything.get(everything.size() - 1 - commentsAtEnd);
            findingComments = (last instanceof Comment);
            if (findingComments) {
                commentsAtEnd++;
            }
        }
        for (int i = 0; i < commentsAtEnd; i++) {
            Comment c = (Comment) everything.get(everything.size() - commentsAtEnd + i);
            CsmComment.process(c, printer);
        }
    }
}
