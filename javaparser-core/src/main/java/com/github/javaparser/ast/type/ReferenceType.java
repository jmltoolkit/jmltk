/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.type;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.ReferenceTypeMetaModel;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base class for reference types.
 *
 * @author Julio Vilmar Gesser
 */
public abstract class ReferenceType extends Type {

    public ReferenceType() {
        this(null, new NodeList<>());
    }

    @AllFieldsConstructor
    public ReferenceType(NodeList<AnnotationExpr> annotations) {
        this(null, annotations);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public ReferenceType(TokenRange tokenRange, NodeList<AnnotationExpr> annotations) {
        super(tokenRange, annotations);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public ReferenceType clone() {
        return (ReferenceType) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public ReferenceTypeMetaModel getMetaModel() {
        return JavaParserMetaModel.referenceTypeMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isReferenceType() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public ReferenceType asReferenceType() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifReferenceType(Consumer<ReferenceType> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<ReferenceType> toReferenceType() {
        return Optional.of(this);
    }

    public abstract String toDescriptor();
}
