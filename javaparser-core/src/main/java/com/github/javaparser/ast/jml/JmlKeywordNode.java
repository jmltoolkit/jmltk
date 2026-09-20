/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlKeywordNodeMetaModel;

/**
 * Base class for AST nodes that represent a single JML keyword, where the keyword itself is
 * modelled by a {@link JmlKeyword} value (type parameter {@code T}).
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
public abstract class JmlKeywordNode<T extends JmlKeyword, S extends Node> extends Node implements Jmlish {

    @AllFieldsConstructor
    public JmlKeywordNode() {
        super(null);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlKeywordNode(TokenRange tokenRange) {
        super(tokenRange);
        customInitialization();
    }

    public abstract T getValue();

    public abstract S setValue(T value);

    public T getKind() {
        return getValue();
    }

    public S setKind(T kind) {
        return setValue(kind);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlKeywordNode<?,?> clone() {
        return (JmlKeywordNode<?,?>) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlKeywordNodeMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlKeywordNodeMetaModel;
    }
}
