/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.body;

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
import com.github.javaparser.metamodel.JmlBodyClauseKindMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * AST node representing the keyword kind of a JML body clause (e.g. {@code invariant},
 * {@code constraint}, {@code axiom} or {@code initially}), wrapping a {@link JmlBodyClauseKeyword}.
 *
 * @author Alexander Weigl
 * @version 1 (15.08.26)
 */
public class JmlBodyClauseKind extends JmlKeywordNode<JmlBodyClauseKind> {

    private JmlBodyClauseKeyword value;

    @AllFieldsConstructor
    public JmlBodyClauseKind(JmlBodyClauseKeyword value) {
        this(null, value);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlBodyClauseKind(TokenRange tokenRange, JmlBodyClauseKeyword value) {
        super(tokenRange);
        setValue(value);
        customInitialization();
    }

    public JmlBodyClauseKind(JavaToken begin) {
        this(new TokenRange(begin, begin), JmlBodyClauseKeyword.getKindByToken(begin));
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

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlBodyClauseKeyword getValue() {
        return value;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlBodyClauseKind setValue(final @NonNull() JmlBodyClauseKeyword value) {
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
    public @NonNull() JmlBodyClauseKeyword value() {
        return Objects.requireNonNull(value);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlBodyClauseKind clone() {
        return (JmlBodyClauseKind) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlBodyClauseKindMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlBodyClauseKindMetaModel;
    }
}
