/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.doc;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;

/**
 * @author Alexander Weigl
 * @version 1 (11/23/21)
 */
public class JmlDocModifier implements Modifier.Keyword, JmlDocContainer {

    private final NodeList<JmlDoc> jmlComments;

    @AllFieldsConstructor
    public JmlDocModifier(NodeList<JmlDoc> jmlComments) {
        this.jmlComments = jmlComments;
    }

    @Override
    public String asString() {
        return "[[JML modifiers]]";
    }

    @Override
    public String name() {
        return asString();
    }

    public NodeList<JmlDoc> getJmlComments() {
        return jmlComments;
    }
}
