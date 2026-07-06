/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.LiteralStringValueExprMetaModel;
import org.jspecify.annotations.NonNull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.utils.Utils.assertNotNull;
import com.github.javaparser.ast.Node;

/**
 * Any literal value that is stored internally as a String.
 */
public abstract class LiteralStringValueExpr extends LiteralExpr {

    protected String value;

    @AllFieldsConstructor
    public LiteralStringValueExpr(final String value) {
        this(null, value);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public LiteralStringValueExpr(TokenRange tokenRange, String value) {
        super(tokenRange);
        setValue(value);
        customInitialization();
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public String getValue() {
        return value;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public LiteralStringValueExpr setValue(final @NonNull() String value) {
        assertNotNull(value);
        if (value.equals(this.value)) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.VALUE, this.value, value);
        this.value = value;
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public LiteralStringValueExpr clone() {
        return (LiteralStringValueExpr) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public LiteralStringValueExprMetaModel getMetaModel() {
        return JavaParserMetaModel.literalStringValueExprMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isLiteralStringValueExpr() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public LiteralStringValueExpr asLiteralStringValueExpr() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifLiteralStringValueExpr(Consumer<LiteralStringValueExpr> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<LiteralStringValueExpr> toLiteralStringValueExpr() {
        return Optional.of(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() String value() {
        return Objects.requireNonNull(value);
    }
}
