/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.proof;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.jml.JmlKeyword;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofObtainStmtMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * An {@code obtain} command inside a {@code \by} proof script, introducing an auxiliary
 * variable used by the proof, e.g. {@code obtain int x \from_goal;},
 * {@code obtain int y = foo();} or {@code obtain int z \such_that z > 0;}.
 *
 * <p>{@link #getType()} and {@link #getVar()} declare the variable, {@link #getKind()}
 * the way its value is obtained:
 *
 * <ul>
 *   <li>{@link ObtainKind#EQUAL} &mdash; the variable is bound to {@link #getValue()},
 *   <li>{@link ObtainKind#SUCH_THAT} &mdash; the variable is constrained by {@link #getValue()}, and
 *   <li>{@link ObtainKind#FROM_GOAL} &mdash; the value is read off from the current proof goal.
 * </ul>
 *
 * <p>{@link #getSuffix()} holds the statements of a nested {@code \by} script that follows
 * the command (empty if the command is terminated by {@code ;}).
 *
 * <p>For example:
 * <pre>{@code
 *     //@ assert X \by { obtain int x \from_goal; };
 *     //@ assert Y \by { obtain int y = y0; };
 *     //@ assert Z \by { obtain int z \such_that z > 0; };
 * }</pre>
 */
public class JmlProofObtainStmt extends JmlProofStmt {

    public enum ObtainKind implements JmlKeyword {
        EQUAL("="),
        SUCH_THAT("\\such_that"),
        FROM_GOAL("\\from_goal");

        private final String jmlSymbol;

        ObtainKind(String jmlSymbol) {
            this.jmlSymbol = jmlSymbol;
        }

        public String jmlSymbol() {
            return jmlSymbol;
        }
    }

    private Type type;

    private SimpleName var;

    private ObtainKind kind;

    @OptionalProperty
    private Expression value;

    private NodeList<JmlProofStmt> suffix;

    @AllFieldsConstructor
    public JmlProofObtainStmt(
            Type type, SimpleName var, ObtainKind kind, Expression value, NodeList<JmlProofStmt> suffix) {
        this(null, type, var, kind, value, suffix);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofObtainStmt(
            TokenRange tokenRange,
            Type type,
            SimpleName var,
            ObtainKind kind,
            Expression value,
            NodeList<JmlProofStmt> suffix) {
        super(tokenRange);
        setType(type);
        setVar(var);
        setKind(kind);
        setValue(value);
        setSuffix(suffix);
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
    public Type getType() {
        return type;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public SimpleName getVar() {
        return var;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public ObtainKind getKind() {
        return kind;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Expression> getValue() {
        return Optional.ofNullable(value);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofStmt> getSuffix() {
        return suffix;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofObtainStmt setType(final @NonNull() Type type) {
        assertNotNull(type);
        if (type == this.type) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.TYPE, this.type, type);
        if (this.type != null) this.type.setParentNode(null);
        this.type = type;
        setAsParentNodeOf(type);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofObtainStmt setVar(final @NonNull() SimpleName var) {
        assertNotNull(var);
        if (var == this.var) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.VAR, this.var, var);
        if (this.var != null) this.var.setParentNode(null);
        this.var = var;
        setAsParentNodeOf(var);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofObtainStmt setKind(final @NonNull() ObtainKind kind) {
        assertNotNull(kind);
        if (kind == this.kind) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.KIND, this.kind, kind);
        this.kind = kind;
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofObtainStmt setValue(final @Nullable() Expression value) {
        if (value == this.value) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.VALUE, this.value, value);
        if (this.value != null) this.value.setParentNode(null);
        this.value = value;
        setAsParentNodeOf(value);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < suffix.size(); i++) {
            if (suffix.get(i) == node) {
                suffix.set(i, (JmlProofStmt) replacementNode);
                return true;
            }
        }
        if (node == type) {
            setType((Type) replacementNode);
            return true;
        }
        if (value != null) {
            if (node == value) {
                setValue((Expression) replacementNode);
                return true;
            }
        }
        if (node == var) {
            setVar((SimpleName) replacementNode);
            return true;
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofObtainStmt clone() {
        return (JmlProofObtainStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofObtainStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofObtainStmtMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < suffix.size(); i++) {
            if (suffix.get(i) == node) {
                suffix.remove(i);
                return true;
            }
        }
        if (value != null) {
            if (node == value) {
                removeValue();
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofObtainStmt removeType() {
        return setType((Type) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofObtainStmt removeVar() {
        return setVar((SimpleName) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofObtainStmt removeValue() {
        return setValue((Expression) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() Type type() {
        return Objects.requireNonNull(type);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() SimpleName var() {
        return Objects.requireNonNull(var);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() ObtainKind kind() {
        return Objects.requireNonNull(kind);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Expression value() {
        return value;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofStmt> suffix() {
        return Objects.requireNonNull(suffix);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofObtainStmt setSuffix(final @NonNull() NodeList<JmlProofStmt> suffix) {
        assertNotNull(suffix);
        if (suffix == this.suffix) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.SUFFIX, this.suffix, suffix);
        if (this.suffix != null) this.suffix.setParentNode(null);
        this.suffix = suffix;
        setAsParentNodeOf(suffix);
        return this;
    }
}
