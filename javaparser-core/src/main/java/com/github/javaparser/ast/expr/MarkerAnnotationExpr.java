/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import static com.github.javaparser.StaticJavaParser.parseName;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.MarkerAnnotationExprMetaModel;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An annotation that uses only the annotation type name.
 * <br>{@code @Override}
 *
 * @author Julio Vilmar Gesser
 */
public class MarkerAnnotationExpr extends AnnotationExpr {

    public MarkerAnnotationExpr() {
        this(null, new Name());
    }

    public MarkerAnnotationExpr(final String name) {
        this(null, parseName(name));
    }

    @AllFieldsConstructor
    public MarkerAnnotationExpr(final Name name) {
        this(null, name);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public MarkerAnnotationExpr(TokenRange tokenRange, Name name) {
        super(tokenRange, name);
        customInitialization();
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
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public MarkerAnnotationExpr clone() {
        return (MarkerAnnotationExpr) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public MarkerAnnotationExprMetaModel getMetaModel() {
        return JavaParserMetaModel.markerAnnotationExprMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isMarkerAnnotationExpr() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public MarkerAnnotationExpr asMarkerAnnotationExpr() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifMarkerAnnotationExpr(Consumer<MarkerAnnotationExpr> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<MarkerAnnotationExpr> toMarkerAnnotationExpr() {
        return Optional.of(this);
    }
}
