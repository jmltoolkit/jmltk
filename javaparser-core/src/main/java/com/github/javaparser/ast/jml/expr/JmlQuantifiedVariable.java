/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlQuantifiedVariableMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A binding of a generalized quantifier: a {@link VariableDeclarator} that may be
 * bound to an iterable domain after {@code ':'}.
 *
 * <p>The core of the element-oriented quantification syntax, e.g.</p>
 * <pre>{@code
 * (\forall int e : a; e > 0)                        // elements of an array
 * (\exists T e : c; e.isEmpty())                    // elements of an Iterable/SequencedCollection
 * (\sum int e : a; e * e)                           // sum of squares
 * (\num_of String s : words; s.length() > 3)        // count words longer than 3
 * (\forall int i : 0..a.length-1; a[i] <= a[i+1])   // index iteration via a range
 * }</pre>
 *
 * <p>The {@code int e : a} part is a {@code JmlQuantifiedVariable} whose type is
 * {@code int}, whose name is {@code e}, and whose {@link #getIterable()} is the
 * domain expression {@code a}. Bindings without an iterable ({@code iterable ==
 * null}) are plain classic quantified variables and are represented as ordinary
 * {@link VariableDeclarator}s by the parser.</p>
 *
 * @author Alexander Weigl
 */
public class JmlQuantifiedVariable extends VariableDeclarator {

    @OptionalProperty
    private Expression iterable;

    public JmlQuantifiedVariable() {
        this(null, new ClassOrInterfaceType(), new SimpleName(), null, null);
    }

    public JmlQuantifiedVariable(Type type, SimpleName name) {
        this(null, type, name, null, null);
    }

    public JmlQuantifiedVariable(Type type, SimpleName name, Expression iterable) {
        this(null, type, name, null, iterable);
    }

    /**
     * Creates a quantified-variable binding.
     *
     * @param type the declared type of the variable
     * @param name the name of the variable
     * @param initializer an initializer expression; must be {@code null} for
     *     quantifier bindings (checked by the validator), kept for the shared
     *     {@link VariableDeclarator} constructor signature
     * @param iterable the domain the variable ranges over (an array, an
     *     {@code Iterable}, a range {@code lo..hi}, …) or {@code null} for a classic binding
     */
    @AllFieldsConstructor
    public JmlQuantifiedVariable(Type type, SimpleName name, Expression initializer, Expression iterable) {
        this(null, type, name, initializer, iterable);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlQuantifiedVariable(
            TokenRange tokenRange, Type type, SimpleName name, Expression initializer, Expression iterable) {
        super(tokenRange, type, name, initializer);
        setIterable(iterable);
        customInitialization();
    }

    @Override
    protected void customInitialization() {
        // VariableDeclarator.customInitialization() registers an observer that keeps the
        // enclosing NodeWithVariables' MAXIMUM_COMMON_TYPE in sync. That machinery is only
        // needed for Java-style shared-type declarator lists; iterable-bound bindings always
        // print their own type (see ConcreteSyntaxModel). Overriding it also avoids
        // registering the observer twice (both the super-class and this class invoke
        // customInitialization() from their main constructors).
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
     * @return the iterable domain this variable is bound to, if present
     */
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Expression> getIterable() {
        return Optional.ofNullable(iterable);
    }

    /**
     * Sets the iterable domain of this binding.
     *
     * @param iterable the domain expression (array, {@code Iterable}, range, …), can be null
     * @return this, the JmlQuantifiedVariable
     */
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlQuantifiedVariable setIterable(final @Nullable() Expression iterable) {
        if (iterable == this.iterable) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.ITERABLE, this.iterable, iterable);
        if (this.iterable != null) this.iterable.setParentNode(null);
        this.iterable = iterable;
        setAsParentNodeOf(iterable);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        if (iterable != null) {
            if (node == iterable) {
                removeIterable();
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlQuantifiedVariable removeIterable() {
        return setIterable((Expression) null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (iterable != null) {
            if (node == iterable) {
                setIterable((Expression) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlQuantifiedVariable clone() {
        return (JmlQuantifiedVariable) accept(new CloneVisitor(), null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Expression iterable() {
        return iterable;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlQuantifiedVariableMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlQuantifiedVariableMetaModel;
    }
}
