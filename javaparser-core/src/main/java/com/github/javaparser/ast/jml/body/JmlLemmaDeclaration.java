/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.body;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.jml.proof.JmlProofStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlLemmaDeclarationMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A JML class-body {@code lemma} declaration, e.g.
 *
 * <pre>{@code
 * //@ instance lemma Pred(int i) = i > 0;
 * //@ public static no_state lemma nospace(\seq s) \by { oss; auto; };
 * //@ lemma P(int i) assert i > 0 \by { auto; };
 * //@ lemma helper();
 * }</pre>
 *
 * <p>The body form is captured by {@link #getKind()}: a {@link JmlLemmaKeyword}
 * node whose {@link JmlLemmaKeyword.Kind} discriminates
 * <ul>
 *   <li>{@link JmlLemmaKeyword.Kind#DEFINITION} &mdash; {@code = expr} (defines the
 *       lemma name as a shorthand for the predicate, also stored in
 *       {@link #getExpression()}),
 *   <li>{@link JmlLemmaKeyword.Kind#ASSERTION} &mdash; {@code assert expr [\by {…}]},
 *   <li>{@link JmlLemmaKeyword.Kind#SCRIPT} &mdash; {@code \by {…}} with no
 *       expression, and
 *   <li>no connector at all (axiom-style {@code lemma helper();}), in which case
 *       {@link #getKind()} is empty.
 * </ul>
 *
 * <p>Note: {@code lemma} is also used on the KeY side as a modifier on method
 * declarations; here it is a grammar keyword of this declaration form, not a
 * {@link Modifier}.
 */
public class JmlLemmaDeclaration extends JmlClassLevelDeclaration<JmlLemmaDeclaration>
        implements NodeWithModifiers<JmlLemmaDeclaration>, NodeWithAccessModifiers<JmlLemmaDeclaration> {

    private NodeList<SimpleName> jmlTags;

    private NodeList<Modifier> modifiers;

    private SimpleName name;

    private NodeList<Parameter> parameters;

    @OptionalProperty
    private JmlLemmaKeyword kind;

    @OptionalProperty
    private Expression expression;

    @OptionalProperty
    private NodeList<JmlProofStmt> script;

    public JmlLemmaDeclaration() {
        super(null);
    }

    @AllFieldsConstructor
    public JmlLemmaDeclaration(
            NodeList<SimpleName> jmlTags,
            NodeList<Modifier> modifiers,
            SimpleName name,
            NodeList<Parameter> parameters,
            JmlLemmaKeyword kind,
            Expression expression,
            NodeList<JmlProofStmt> script) {
        this(null, jmlTags, modifiers, name, parameters, kind, expression, script);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlLemmaDeclaration(
            TokenRange tokenRange,
            NodeList<SimpleName> jmlTags,
            NodeList<Modifier> modifiers,
            SimpleName name,
            NodeList<Parameter> parameters,
            JmlLemmaKeyword kind,
            Expression expression,
            NodeList<JmlProofStmt> script) {
        super(tokenRange);
        setJmlTags(jmlTags);
        setModifiers(modifiers);
        setName(name);
        setParameters(parameters);
        setKind(kind);
        setExpression(expression);
        setScript(script);
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

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        if (expression != null) {
            if (node == expression) {
                removeExpression();
                return true;
            }
        }
        for (int i = 0; i < jmlTags.size(); i++) {
            if (jmlTags.get(i) == node) {
                jmlTags.remove(i);
                return true;
            }
        }
        if (kind != null) {
            if (node == kind) {
                removeKind();
                return true;
            }
        }
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i) == node) {
                modifiers.remove(i);
                return true;
            }
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) == node) {
                parameters.remove(i);
                return true;
            }
        }
        if (script != null) {
            for (int i = 0; i < script.size(); i++) {
                if (script.get(i) == node) {
                    script.remove(i);
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
        if (expression != null) {
            if (node == expression) {
                setExpression((Expression) replacementNode);
                return true;
            }
        }
        for (int i = 0; i < jmlTags.size(); i++) {
            if (jmlTags.get(i) == node) {
                jmlTags.set(i, (SimpleName) replacementNode);
                return true;
            }
        }
        if (kind != null) {
            if (node == kind) {
                setKind((JmlLemmaKeyword) replacementNode);
                return true;
            }
        }
        for (int i = 0; i < modifiers.size(); i++) {
            if (modifiers.get(i) == node) {
                modifiers.set(i, (Modifier) replacementNode);
                return true;
            }
        }
        if (node == name) {
            setName((SimpleName) replacementNode);
            return true;
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) == node) {
                parameters.set(i, (Parameter) replacementNode);
                return true;
            }
        }
        if (script != null) {
            for (int i = 0; i < script.size(); i++) {
                if (script.get(i) == node) {
                    script.set(i, (JmlProofStmt) replacementNode);
                    return true;
                }
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlLemmaDeclaration clone() {
        return (JmlLemmaDeclaration) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<SimpleName> getJmlTags() {
        return jmlTags;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setJmlTags(final @NonNull() NodeList<SimpleName> jmlTags) {
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
    public NodeList<Modifier> getModifiers() {
        return modifiers;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setModifiers(final @NonNull() NodeList<Modifier> modifiers) {
        assertNotNull(modifiers);
        if (modifiers == this.modifiers) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.MODIFIERS, this.modifiers, modifiers);
        if (this.modifiers != null) this.modifiers.setParentNode(null);
        this.modifiers = modifiers;
        setAsParentNodeOf(modifiers);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public SimpleName getName() {
        return name;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setName(final @NonNull() SimpleName name) {
        assertNotNull(name);
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
    public NodeList<Parameter> getParameters() {
        return parameters;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setParameters(final @NonNull() NodeList<Parameter> parameters) {
        assertNotNull(parameters);
        if (parameters == this.parameters) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.PARAMETERS, this.parameters, parameters);
        if (this.parameters != null) this.parameters.setParentNode(null);
        this.parameters = parameters;
        setAsParentNodeOf(parameters);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<JmlLemmaKeyword> getKind() {
        return Optional.ofNullable(kind);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setKind(final @Nullable() JmlLemmaKeyword kind) {
        if (kind == this.kind) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.KIND, this.kind, kind);
        if (this.kind != null) this.kind.setParentNode(null);
        this.kind = kind;
        setAsParentNodeOf(kind);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlLemmaDeclaration removeKind() {
        return setKind((JmlLemmaKeyword) null);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<Expression> getExpression() {
        return Optional.ofNullable(expression);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setExpression(final @Nullable() Expression expression) {
        if (expression == this.expression) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.EXPRESSION, this.expression, expression);
        if (this.expression != null) this.expression.setParentNode(null);
        this.expression = expression;
        setAsParentNodeOf(expression);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlLemmaDeclaration removeExpression() {
        return setExpression((Expression) null);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Optional<NodeList<JmlProofStmt>> getScript() {
        return Optional.ofNullable(script);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaDeclaration setScript(final @Nullable() NodeList<JmlProofStmt> script) {
        if (script == this.script) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.SCRIPT, this.script, script);
        if (this.script != null) this.script.setParentNode(null);
        this.script = script;
        setAsParentNodeOf(script);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlLemmaDeclaration removeScript() {
        return setScript((NodeList<JmlProofStmt>) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<SimpleName> jmlTags() {
        return Objects.requireNonNull(jmlTags);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<Modifier> modifiers() {
        return Objects.requireNonNull(modifiers);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() SimpleName name() {
        return Objects.requireNonNull(name);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<Parameter> parameters() {
        return Objects.requireNonNull(parameters);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() JmlLemmaKeyword kind() {
        return kind;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() Expression expression() {
        return expression;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() NodeList<JmlProofStmt> script() {
        return script;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isJmlLemmaDeclaration() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public JmlLemmaDeclaration asJmlLemmaDeclaration() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<JmlLemmaDeclaration> toJmlLemmaDeclaration() {
        return Optional.of(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifJmlLemmaDeclaration(Consumer<JmlLemmaDeclaration> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlLemmaDeclarationMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlLemmaDeclarationMetaModel;
    }
}
