/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.SimpleName;

/**
 * @author Alexander Weigl
 * @version 1 (30.05.22)
 */
public interface NodeWithJmlTags<R extends Node> {

    NodeList<SimpleName> getJmlTags();

    R setJmlTags(NodeList<SimpleName> tags);
}
