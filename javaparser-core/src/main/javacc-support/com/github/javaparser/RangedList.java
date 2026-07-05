/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

/**
 * Helper class for {@link GeneratedJavaParser}
 */
class RangedList<T extends Node> {
    /* A ranged list MUST be set to a begin and end,
    or these temporary values will leak out */
    TokenRange range = new TokenRange(JavaToken.INVALID, JavaToken.INVALID);
    NodeList<T> list;

    RangedList(NodeList<T> list) {
        this.list = list;
    }

    void beginAt(JavaToken begin) {
        range = range.withBegin(begin);
    }

    void endAt(JavaToken end) {
        range = range.withEnd(end);
    }

    void add(T t) {
        if (list == null) {
            list = new NodeList<>();
        }
        list.add(t);
    }
}
