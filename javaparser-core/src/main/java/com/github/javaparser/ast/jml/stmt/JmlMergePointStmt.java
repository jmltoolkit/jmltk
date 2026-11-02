/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.stmt;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.jml.NodeWithJmlTags;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlMergePointStmtMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A JML {@code merge_point} statement, used to insert a state-merging/abstraction point
 * into a Java method, e.g.
 * <pre>{@code
 * //@ merge_point \\by "MergeByPredicateAbstraction"
 * //@     \\lattice conjunctive
 * //@     \\predicate (int x) : x >= 0, x == \\old(b) || x == -\\old(b);
 * }</pre>
 *
 * <p>The statement is used by KeY-style state merging: {@code \\by} names the abstraction
 * strategy (or the procedure implementing it, given as a string literal or identifier),
 * {@code \\lattice} names the lattice, and {@code \\predicate} binds a variable {@code (type name)}
 * and lists the abstraction predicates separating it by commas.</p>
 *
 * @author Alexander Weigl
 */
public class JmlMergePointStmt extends JmlStatement implements NodeWithJmlTags<JmlMergePointStmt> {

    private NodeList<SimpleName> jmlTags;

    /**
     * The abstraction procedure/property of {@code \\by}, e.g. {@code "MergeByPredicateAbstraction"}.
     */
    @OptionalProperty
    private StringLiteralExpr by;

    /**
     * The lattice name given by {@code \\lattice}, e.g. {@code conjunctive}.
     */
    @OptionalProperty
    private StringLiteralExpr lattice;

    /**
     * The variable bound by the {@code \\predicate} option, e.g. {@code (int x)}.
     */
    @OptionalProperty
    private Parameter predicateVariable;

    /**
     * The abstraction predicates listed after the bound variable, separated by commas,
     * e.g. {@code x >= 0, x == \\old(b) || x == -\\old(b)}.
     */
    @OptionalProperty
    private NodeList<Expression> predicates;

    @AllFieldsConstructor
    public JmlMergePointStmt(
            NodeList<SimpleName> jmlTags,
            StringLiteralExpr by,
            StringLiteralExpr lattice,
            Parameter predicateVariable,
            NodeList<Expression> predicates) {
        this(null, jmlTags, by, lattice, predicateVariable, predicates);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlMergePointStmt(
            TokenRange tokenRange,
            NodeList<SimpleName> jmlTags,
            StringLiteralExpr by,
            StringLiteralExpr lattice,
            Parameter predicateVariable,
            NodeList<Expression> predicates) {
        super(tokenRange);
        setJmlTags(jmlTags);
        setBy(by);
        setLattice(lattice);
        setPredicateVariable(predicateVariable);
        setPredicates(predicates);
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
    public NodeList<SimpleName> getJmlTags() {
        return jmlTags;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlMergePointStmt setJmlTags(final @NonNull() NodeList<SimpleName> jmlTags) {
        assertNotNull(jmlTags);
        if (jmlTags == this.jmlTags) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.JML_TAGS, this.jmlTags, jmlTags);
        if (this.jmlTags != null) this.jmlTags.setParentNode(null);
        this.jmlTags = jmlTags;
        setAsParentNodeOf(jmlTags);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlMergePointStmt setBy(final @Nullable() StringLiteralExpr by) {
        if (by == this.by) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.BY, this.by, by);
        if (this.by != null) this.by.setParentNode(null);
        this.by = by;
        setAsParentNodeOf(by);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlMergePointStmt setLattice(final @Nullable() StringLiteralExpr lattice) {
        if (lattice == this.lattice) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.LATTICE, this.lattice, lattice);
        if (this.lattice != null) this.lattice.setParentNode(null);
        this.lattice = lattice;
        setAsParentNodeOf(lattice);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlMergePointStmt setPredicateVariable(final @Nullable() Parameter predicateVariable) {
        if (predicateVariable == this.predicateVariable) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.PREDICATE_VARIABLE, this.predicateVariable, predicateVariable);
        if (this.predicateVariable != null) this.predicateVariable.setParentNode(null);
        this.predicateVariable = predicateVariable;
        setAsParentNodeOf(predicateVariable);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlMergePointStmt setPredicates(final @Nullable() NodeList<Expression> predicates) {
        if (predicates == this.predicates) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.PREDICATES, this.predicates, predicates);
        if (this.predicates != null) this.predicates.setParentNode(null);
        this.predicates = predicates;
        setAsParentNodeOf(predicates);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isJmlMergePointStmt() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public JmlMergePointStmt asJmlMergePointStmt() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<JmlMergePointStmt> toJmlMergePointStmt() {
        return Optional.of(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifJmlMergePointStmt(Consumer<JmlMergePointStmt> action) {
        action.accept(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<SimpleName> jmlTags() {
        return Objects.requireNonNull(jmlTags);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<StringLiteralExpr> getLattice() {
        return Optional.ofNullable(lattice);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() StringLiteralExpr lattice() {
        return lattice;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Parameter> getPredicateVariable() {
        return Optional.ofNullable(predicateVariable);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Parameter predicateVariable() {
        return predicateVariable;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<NodeList<Expression>> getPredicates() {
        return Optional.ofNullable(predicates);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() NodeList<Expression> predicates() {
        return predicates;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<StringLiteralExpr> getBy() {
        return Optional.ofNullable(by);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() StringLiteralExpr by() {
        return by;
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlMergePointStmt removeLattice() {
        return setLattice((StringLiteralExpr) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlMergePointStmt removePredicateVariable() {
        return setPredicateVariable((Parameter) null);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlMergePointStmt removeBy() {
        return setBy((StringLiteralExpr) null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        if (by != null) {
            if (node == by) {
                removeBy();
                return true;
            }
        }
        for (int i = 0; i < jmlTags.size(); i++) {
            if (jmlTags.get(i) == node) {
                jmlTags.remove(i);
                return true;
            }
        }
        if (lattice != null) {
            if (node == lattice) {
                removeLattice();
                return true;
            }
        }
        if (predicateVariable != null) {
            if (node == predicateVariable) {
                removePredicateVariable();
                return true;
            }
        }
        if (predicates != null) {
            for (int i = 0; i < predicates.size(); i++) {
                if (predicates.get(i) == node) {
                    predicates.remove(i);
                    return true;
                }
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
        if (by != null) {
            if (node == by) {
                setBy((StringLiteralExpr) replacementNode);
                return true;
            }
        }
        for (int i = 0; i < jmlTags.size(); i++) {
            if (jmlTags.get(i) == node) {
                jmlTags.set(i, (SimpleName) replacementNode);
                return true;
            }
        }
        if (lattice != null) {
            if (node == lattice) {
                setLattice((StringLiteralExpr) replacementNode);
                return true;
            }
        }
        if (predicateVariable != null) {
            if (node == predicateVariable) {
                setPredicateVariable((Parameter) replacementNode);
                return true;
            }
        }
        if (predicates != null) {
            for (int i = 0; i < predicates.size(); i++) {
                if (predicates.get(i) == node) {
                    predicates.set(i, (Expression) replacementNode);
                    return true;
                }
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlMergePointStmt clone() {
        return (JmlMergePointStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlMergePointStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlMergePointStmtMetaModel;
    }
}
