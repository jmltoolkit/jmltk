/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.SimpleName;

/**
 * Represents an AST node that carries JML tags, i.e. the {@code public}, {@code protected} or
 * {@code private} visibility tags that make a Java element visible to JML specifications.
 *
 * @author Alexander Weigl
 * @version 1 (9/8/21)
 */
public interface HasJmlTags<N extends Node> {

    N setJmlTags(NodeList<SimpleName> jmlTags);

    NodeList<SimpleName> getJmlTags();
}
