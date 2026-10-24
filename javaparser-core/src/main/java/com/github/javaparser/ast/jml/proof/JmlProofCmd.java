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
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofCmdMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A proof command inside a JML proof script ({@code \by}).
 * Either an {@code obtain} command (then {@link #getName()} is {@code null} and the fields
 * {@link #getType()}, {@link #getVar()}, {@link #getKind()} and {@link #getValue()} are set),
 * or a general proof command (then {@link #getName()} and {@link #getArgs()} are set).
 */
public class JmlProofCmd extends Node {

    public enum ObtainKind {
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

    @OptionalProperty
    private SimpleName name;

    private NodeList<JmlProofArg> args;

    @OptionalProperty
    private Type type;

    @OptionalProperty
    private SimpleName var;

    @OptionalProperty
    private ObtainKind kind;

    @OptionalProperty
    private Expression value;

    @OptionalProperty
    private JmlProofScript suffix;

    @AllFieldsConstructor
    public JmlProofCmd(
            SimpleName name,
            NodeList<JmlProofArg> args,
            Type type,
            SimpleName var,
            ObtainKind kind,
            Expression value,
            JmlProofScript suffix) {
        this(null, name, args, type, var, kind, value, suffix);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofCmd(
            TokenRange tokenRange,
            SimpleName name,
            NodeList<JmlProofArg> args,
            Type type,
            SimpleName var,
            ObtainKind kind,
            Expression value,
            JmlProofScript suffix) {
        super(tokenRange);
        setName(name);
        setArgs(args);
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
    public Optional<SimpleName> getName() {
        return Optional.ofNullable(name);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofArg> getArgs() {
        return args;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Type> getType() {
        return Optional.ofNullable(type);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<SimpleName> getVar() {
        return Optional.ofNullable(var);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<ObtainKind> getKind() {
        return Optional.ofNullable(kind);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Expression> getValue() {
        return Optional.ofNullable(value);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<JmlProofScript> getSuffix() {
        return Optional.ofNullable(suffix);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmd setName(final @Nullable() SimpleName name) {
        if (name == this.name) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null) this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmd setArgs(final @NonNull() NodeList<JmlProofArg> args) {
        assertNotNull(args);
        if (args == this.args) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.ARGS, this.args, args);
        if (this.args != null) this.args.setParentNode(null);
        this.args = args;
        setAsParentNodeOf(args);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmd setType(final @Nullable() Type type) {
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
    public JmlProofCmd setVar(final @Nullable() SimpleName var) {
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
    public JmlProofCmd setKind(final @Nullable() ObtainKind kind) {
        if (kind == this.kind) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.KIND, this.kind, kind);
        this.kind = kind;
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmd setValue(final @Nullable() Expression value) {
        if (value == this.value) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.VALUE, this.value, value);
        if (this.value != null) this.value.setParentNode(null);
        this.value = value;
        setAsParentNodeOf(value);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmd setSuffix(final @Nullable() JmlProofScript suffix) {
        if (suffix == this.suffix) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.SUFFIX, this.suffix, suffix);
        if (this.suffix != null) this.suffix.setParentNode(null);
        this.suffix = suffix;
        setAsParentNodeOf(suffix);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == node) {
                args.set(i, (JmlProofArg) replacementNode);
                return true;
            }
        }
        if (name != null) {
            if (node == name) {
                setName((SimpleName) replacementNode);
                return true;
            }
        }
        if (suffix != null) {
            if (node == suffix) {
                setSuffix((JmlProofScript) replacementNode);
                return true;
            }
        }
        if (type != null) {
            if (node == type) {
                setType((Type) replacementNode);
                return true;
            }
        }
        if (value != null) {
            if (node == value) {
                setValue((Expression) replacementNode);
                return true;
            }
        }
        if (var != null) {
            if (node == var) {
                setVar((SimpleName) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofCmd clone() {
        return (JmlProofCmd) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofCmdMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofCmdMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == node) {
                args.remove(i);
                return true;
            }
        }
        if (name != null) {
            if (node == name) {
                removeName();
                return true;
            }
        }
        if (suffix != null) {
            if (node == suffix) {
                removeSuffix();
                return true;
            }
        }
        if (type != null) {
            if (node == type) {
                removeType();
                return true;
            }
        }
        if (value != null) {
            if (node == value) {
                removeValue();
                return true;
            }
        }
        if (var != null) {
            if (node == var) {
                removeVar();
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmd removeName() {
        return setName((SimpleName) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmd removeType() {
        return setType((Type) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmd removeVar() {
        return setVar((SimpleName) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmd removeValue() {
        return setValue((Expression) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmd removeSuffix() {
        return setSuffix((JmlProofScript) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() SimpleName name() {
        return name;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofArg> args() {
        return Objects.requireNonNull(args);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Type type() {
        return type;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() SimpleName var() {
        return var;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() ObtainKind kind() {
        return kind;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Expression value() {
        return value;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() JmlProofScript suffix() {
        return suffix;
    }
}
