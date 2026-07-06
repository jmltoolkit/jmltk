/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.ArrayCreationLevelMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * In {@code new int[1][2];} there are two ArrayCreationLevel objects,
 * the first one contains the expression "1",
 * the second the expression "2".
 */
public class ArrayCreationLevel extends Node implements NodeWithAnnotations<ArrayCreationLevel> {

    @OptionalProperty
    private Expression dimension;

    private NodeList<AnnotationExpr> annotations = new NodeList<>();

    public ArrayCreationLevel() {
        this(null, null, new NodeList<>());
    }

    public ArrayCreationLevel(int dimension) {
        this(null, new IntegerLiteralExpr("" + dimension), new NodeList<>());
    }

    public ArrayCreationLevel(Expression dimension) {
        this(null, dimension, new NodeList<>());
    }

    @AllFieldsConstructor
    public ArrayCreationLevel(Expression dimension, NodeList<AnnotationExpr> annotations) {
        this(null, dimension, annotations);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public ArrayCreationLevel(TokenRange tokenRange, Expression dimension, NodeList<AnnotationExpr> annotations) {
        super(tokenRange);
        setDimension(dimension);
        setAnnotations(annotations);
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

    /**
     * Sets the dimension
     *
     * @param dimension the dimension, can be null
     * @return this, the ArrayCreationLevel
     */
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public ArrayCreationLevel setDimension(final @Nullable() Expression dimension) {
        if (dimension == this.dimension) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.DIMENSION, this.dimension, dimension);
        if (this.dimension != null)
            this.dimension.setParentNode(null);
        this.dimension = dimension;
        setAsParentNodeOf(dimension);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Expression> getDimension() {
        return Optional.ofNullable(dimension);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<AnnotationExpr> getAnnotations() {
        return annotations;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public ArrayCreationLevel setAnnotations(final @NonNull() NodeList<AnnotationExpr> annotations) {
        assertNotNull(annotations);
        if (annotations == this.annotations) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.ANNOTATIONS, this.annotations, annotations);
        if (this.annotations != null)
            this.annotations.setParentNode(null);
        this.annotations = annotations;
        setAsParentNodeOf(annotations);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public ArrayCreationLevel removeDimension() {
        return setDimension((Expression) null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < annotations.size(); i++) {
            if (annotations.get(i) == node) {
                annotations.remove(i);
                return true;
            }
        }
        if (dimension != null) {
            if (node == dimension) {
                removeDimension();
                return true;
            }
        }
        return super.remove(node);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public ArrayCreationLevel clone() {
        return (ArrayCreationLevel) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public ArrayCreationLevelMetaModel getMetaModel() {
        return JavaParserMetaModel.arrayCreationLevelMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < annotations.size(); i++) {
            if (annotations.get(i) == node) {
                annotations.set(i, (AnnotationExpr) replacementNode);
                return true;
            }
        }
        if (dimension != null) {
            if (node == dimension) {
                setDimension((Expression) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<AnnotationExpr> annotations() {
        return Objects.requireNonNull(annotations);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Expression dimension() {
        return dimension;
    }
}
