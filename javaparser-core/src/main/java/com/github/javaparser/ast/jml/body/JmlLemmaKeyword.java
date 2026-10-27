/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.body;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Jmlish;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.jml.JmlKeyword;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlLemmaKeywordMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * The keyword connecting a lemma name with its body in a
 * {@link JmlLemmaDeclaration}, wrapped in a node so that its source position
 * is preserved. This mirrors how {@link com.github.javaparser.ast.Modifier}
 * wraps a keyword enum in a node.
 *
 * <p>The three body forms of a lemma are:
 * <ul>
 *   <li>{@link Kind#DEFINITION} &mdash; {@code lemma P(int i) = i > 0;}
 *   <li>{@link Kind#ASSERTION} &mdash; {@code lemma P(int i) assert i > 0 [\by {…}];}
 *   <li>{@link Kind#SCRIPT} &mdash; {@code lemma P(\seq s) \by { auto; };}
 * </ul>
 * A bare {@code lemma P();} (axiom-style) has no connector keyword; then the
 * declaration carries no {@code JmlLemmaKeyword} at all.
 */
public class JmlLemmaKeyword extends Node implements Jmlish, JmlKeyword {

    public enum Kind implements JmlKeyword {
        DEFINITION("="),
        ASSERTION("assert"),
        SCRIPT("\\by"),
        NONE("");

        private final String jmlSymbol;

        Kind(String jmlSymbol) {
            this.jmlSymbol = jmlSymbol;
        }

        @Override
        public String jmlSymbol() {
            return jmlSymbol;
        }
    }

    private Kind keyword;

    public JmlLemmaKeyword() {
        this(null, Kind.NONE);
    }

    @AllFieldsConstructor
    public JmlLemmaKeyword(Kind keyword) {
        this(null, keyword);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlLemmaKeyword(TokenRange tokenRange, Kind keyword) {
        super(tokenRange);
        setKeyword(keyword);
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
    public Kind getKeyword() {
        return keyword;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlLemmaKeyword setKeyword(final @NonNull() Kind keyword) {
        assertNotNull(keyword);
        if (keyword == this.keyword) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.KEYWORD, this.keyword, keyword);
        this.keyword = keyword;
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlLemmaKeyword clone() {
        return (JmlLemmaKeyword) accept(new CloneVisitor(), null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() Kind keyword() {
        return Objects.requireNonNull(keyword);
    }

    @Override
    public String jmlSymbol() {
        return keyword.jmlSymbol();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlLemmaKeywordMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlLemmaKeywordMetaModel;
    }
}
