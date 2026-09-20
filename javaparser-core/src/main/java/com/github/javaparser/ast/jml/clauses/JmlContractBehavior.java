/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.clauses;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.jml.JmlKeywordNode;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlContractBehaviorMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * AST node holding the behaviour keyword ({@link JmlBehaviorKind}) that introduces a contract,
 * e.g. {@code behavior} or {@code normal_behavior}.
 *
 * For example: <pre>{@code behavior}</pre>
 *
 * @author Alexander Weigl
 * @version 1 (3/14/21)
 */
public class JmlContractBehavior extends JmlKeywordNode<JmlBehaviorKind, JmlContractBehavior> {

    private JmlBehaviorKind value;

    @AllFieldsConstructor
    public JmlContractBehavior(JmlBehaviorKind value) {
        this(null, value);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlContractBehavior(TokenRange tokenRange, JmlBehaviorKind value) {
        super(tokenRange);
        setValue(value);
        customInitialization();
    }

    public JmlContractBehavior(JavaToken token) {
        this(new TokenRange(token, token), JmlBehaviorKind.getByToken(token));
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlBehaviorKind getValue() {
        return value;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlContractBehavior setValue(final @NonNull() JmlBehaviorKind value) {
        assertNotNull(value);
        if (value == this.value) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.VALUE, this.value, value);
        this.value = value;
        return this;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() JmlBehaviorKind value() {
        return Objects.requireNonNull(value);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlContractBehavior clone() {
        return (JmlContractBehavior) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlContractBehaviorMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlContractBehaviorMetaModel;
    }
}
