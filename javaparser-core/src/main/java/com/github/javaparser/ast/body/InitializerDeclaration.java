/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.body;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.InitializerDeclarationMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import org.jspecify.annotations.NonNull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A (possibly static) initializer body. "static { a=3; }" in this example: {@code class X { static { a=3; }  } }
 *
 * @author Julio Vilmar Gesser
 */
public class InitializerDeclaration extends BodyDeclaration<InitializerDeclaration> implements NodeWithJavadoc<InitializerDeclaration>, NodeWithBlockStmt<InitializerDeclaration> {

    private boolean isStatic;

    private BlockStmt body;

    public InitializerDeclaration() {
        this(null, false, new BlockStmt());
    }

    @AllFieldsConstructor
    public InitializerDeclaration(boolean isStatic, BlockStmt body) {
        this(null, isStatic, body);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public InitializerDeclaration(TokenRange tokenRange, boolean isStatic, BlockStmt body) {
        super(tokenRange);
        setStatic(isStatic);
        setBody(body);
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
    public BlockStmt getBody() {
        return body;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public boolean isStatic() {
        return Objects.requireNonNull(isStatic);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public InitializerDeclaration setBody(final @NonNull() BlockStmt body) {
        assertNotNull(body);
        if (body == this.body) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.BODY, this.body, body);
        if (this.body != null)
            this.body.setParentNode(null);
        this.body = body;
        setAsParentNodeOf(body);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public InitializerDeclaration setStatic(final boolean isStatic) {
        if (isStatic == this.isStatic) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.STATIC, this.isStatic, isStatic);
        this.isStatic = isStatic;
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public InitializerDeclaration clone() {
        return (InitializerDeclaration) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public InitializerDeclarationMetaModel getMetaModel() {
        return JavaParserMetaModel.initializerDeclarationMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (node == body) {
            setBody((BlockStmt) replacementNode);
            return true;
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isInitializerDeclaration() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public InitializerDeclaration asInitializerDeclaration() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifInitializerDeclaration(Consumer<InitializerDeclaration> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<InitializerDeclaration> toInitializerDeclaration() {
        return Optional.of(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() BlockStmt body() {
        return Objects.requireNonNull(body);
    }
}
