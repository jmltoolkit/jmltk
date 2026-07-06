/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlExpressionMetaModel;
import java.util.Optional;
import java.util.function.Consumer;
import com.github.javaparser.ast.Node;

/**
 * @author Alexander Weigl
 * @version 1 (4/6/26)
 */
public abstract class JmlExpression extends Expression implements Jmlish {

    @AllFieldsConstructor
    public JmlExpression() {
        super();
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlExpression(TokenRange tokenRange) {
        super(tokenRange);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isJmlExpression() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public JmlExpression asJmlExpression() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<JmlExpression> toJmlExpression() {
        return Optional.of(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifJmlExpression(Consumer<JmlExpression> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlExpression clone() {
        return (JmlExpression) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlExpressionMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlExpressionMetaModel;
    }
}
