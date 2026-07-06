/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.NameExprMetaModel;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jspecify.annotations.NonNull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * Whenever a SimpleName is used in an expression, it is wrapped in NameExpr.
 * <br>In {@code int x = a + 3;} a is a SimpleName inside a NameExpr.
 *
 * @author Julio Vilmar Gesser
 */
public class NameExpr extends Expression implements NodeWithSimpleName<NameExpr>, Resolvable<ResolvedValueDeclaration> {

    private SimpleName name;

    public NameExpr() {
        this(null, new SimpleName());
    }

    public NameExpr(final String name) {
        this(null, new SimpleName(name));
    }

    @AllFieldsConstructor
    public NameExpr(final SimpleName name) {
        this(name.getTokenRange().orElse(null), name);
        setRange(name.getRange().orElse(null));
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public NameExpr(TokenRange tokenRange, SimpleName name) {
        super(tokenRange);
        setName(name);
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
    public SimpleName getName() {
        return name;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NameExpr setName(final @NonNull() SimpleName name) {
        assertNotNull(name);
        if (name == this.name) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null)
            this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public NameExpr clone() {
        return (NameExpr) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public NameExprMetaModel getMetaModel() {
        return JavaParserMetaModel.nameExprMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (node == name) {
            setName((SimpleName) replacementNode);
            return true;
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isNameExpr() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public NameExpr asNameExpr() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifNameExpr(Consumer<NameExpr> action) {
        action.accept(this);
    }

    /**
     * Attempts to resolve the declaration corresponding to the accessed name. If successful, a
     * {@link ResolvedValueDeclaration} representing the declaration of the value accessed by this {@code NameExpr} is
     * returned. Otherwise, an {@link UnsolvedSymbolException} is thrown.
     *
     * @return a {@link ResolvedValueDeclaration} representing the declaration of the accessed value.
     * @throws UnsolvedSymbolException if the declaration corresponding to the name expression could not be resolved.
     * @see FieldAccessExpr#resolve()
     * @see MethodCallExpr#resolve()
     * @see ObjectCreationExpr#resolve()
     * @see ExplicitConstructorInvocationStmt#resolve()
     */
    @Override
    public ResolvedValueDeclaration resolve() {
        return getSymbolResolver().resolveDeclaration(this, ResolvedValueDeclaration.class);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<NameExpr> toNameExpr() {
        return Optional.of(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() SimpleName name() {
        return Objects.requireNonNull(name);
    }
}
