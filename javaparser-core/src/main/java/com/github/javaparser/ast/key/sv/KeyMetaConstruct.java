/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.key.sv;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.KeyMetaConstructMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.javaparser.utils.Utils.assertNotNull;

///
/// @author weigl
public class KeyMetaConstruct extends Statement {

    private String kind;

    private NodeList<Node> children = new NodeList<>();

    @AllFieldsConstructor
    public KeyMetaConstruct(String kind, NodeList<Node> children) {
        this.kind = kind;
        this.children = children;
    }

    public KeyMetaConstruct(TokenRange range, String kind, Node... children) {
        super(range);
        this.kind = kind;
        for (Node child : children) {
            if (child != null) this.children.add(child);
        }
    }

    public KeyMetaConstruct(TokenRange range, JavaToken kind, Node... children) {
        this(range, kind.getText(), children);
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
    public String getKind() {
        return kind;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public KeyMetaConstruct setKind(final @NonNull() String kind) {
        assertNotNull(kind);
        if (kind.equals(this.kind)) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.KIND, this.kind, kind);
        this.kind = kind;
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<Node> getChildren() {
        return children;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public KeyMetaConstruct setChildren(final @NonNull() NodeList<Node> children) {
        assertNotNull(children);
        if (children == this.children) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.CHILDREN, this.children, children);
        if (this.children != null) this.children.setParentNode(null);
        this.children = children;
        setAsParentNodeOf(children);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == node) {
                children.remove(i);
                return true;
            }
        }
        return super.remove(node);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == node) {
                children.set(i, (Node) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public KeyMetaConstruct clone() {
        return (KeyMetaConstruct) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public KeyMetaConstructMetaModel getMetaModel() {
        return JavaParserMetaModel.keyMetaConstructMetaModel;
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public KeyMetaConstruct(TokenRange tokenRange, String kind, NodeList<Node> children) {
        super(tokenRange);
        setKind(kind);
        setChildren(children);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isKeyMetaConstruct() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public KeyMetaConstruct asKeyMetaConstruct() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<KeyMetaConstruct> toKeyMetaConstruct() {
        return Optional.of(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifKeyMetaConstruct(Consumer<KeyMetaConstruct> action) {
        action.accept(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() String kind() {
        return Objects.requireNonNull(kind);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<Node> children() {
        return Objects.requireNonNull(children);
    }
}
