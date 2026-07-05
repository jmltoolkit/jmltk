/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.doc;

import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.NodeList;

/**
 * @author Alexander Weigl
 * @version 1 (26.05.22)
 */
public interface JmlDocContainer {

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    NodeList<JmlDoc> getJmlComments();
}
