/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.NameMetaModel;
import com.github.javaparser.metamodel.NonEmptyProperty;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import static com.github.javaparser.utils.Utils.assertNonEmpty;

/**
 * A name that may consist of multiple identifiers.
 * In other words: it.may.contain.dots.
 * <p>
 * The rightmost identifier is "identifier",
 * The one to the left of it is "qualifier.identifier", etc.
 * <p>
 * You can construct one from a String with the {@link com.github.javaparser.StaticJavaParser#parseName(String)} method.
 *
 * @author Julio Vilmar Gesser
 * @see SimpleName
 */
public class Name extends Node implements NodeWithIdentifier<Name> {

    @NonEmptyProperty
    private String identifier;

    @OptionalProperty
    private Name qualifier;

    public Name() {
        this(null, null, "empty");
    }

    public Name(final String identifier) {
        this(null, null, identifier);
    }

    @AllFieldsConstructor
    public Name(Name qualifier, final String identifier) {
        this(null, qualifier, identifier);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public Name(TokenRange tokenRange, Name qualifier, String identifier) {
        super(tokenRange);
        setQualifier(qualifier);
        setIdentifier(identifier);
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

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public String getIdentifier() {
        return identifier;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Name setIdentifier(final @NonNull() String identifier) {
        assertNonEmpty(identifier);
        if (identifier.equals(this.identifier)) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.IDENTIFIER, this.identifier, identifier);
        this.identifier = identifier;
        return this;
    }

    /**
     * @return the complete qualified name. Only the identifiers and the dots, so no comments or whitespace.
     */
    public String asString() {
        if (qualifier != null) {
            return qualifier.asString() + "." + identifier;
        }
        return identifier;
    }

    public boolean hasQualifier() {
        return qualifier != null;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Name> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Name setQualifier(final @Nullable() Name qualifier) {
        if (qualifier == this.qualifier) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.QUALIFIER, this.qualifier, qualifier);
        if (this.qualifier != null)
            this.qualifier.setParentNode(null);
        this.qualifier = qualifier;
        setAsParentNodeOf(qualifier);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        if (qualifier != null) {
            if (node == qualifier) {
                removeQualifier();
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public Name removeQualifier() {
        return setQualifier((Name) null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public Name clone() {
        return (Name) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public NameMetaModel getMetaModel() {
        return JavaParserMetaModel.nameMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (qualifier != null) {
            if (node == qualifier) {
                setQualifier((Name) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    /**
     * A top level name is a name that is not contained in a larger Name instance.
     */
    public boolean isTopLevel() {
        return !isInternal();
    }

    /**
     * An internal name is a name that constitutes a part of a larger Name instance.
     */
    public boolean isInternal() {
        return getParentNode().filter(parent -> parent instanceof Name).isPresent();
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() String identifier() {
        return Objects.requireNonNull(identifier);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Name qualifier() {
        return qualifier;
    }
}
