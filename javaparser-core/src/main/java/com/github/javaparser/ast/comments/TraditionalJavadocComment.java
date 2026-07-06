/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.comments;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.TraditionalJavadocCommentMetaModel;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.StaticJavaParser.parseJavadoc;
import com.github.javaparser.ast.Node;

/**
 * A Javadoc comment. {@code /∗∗ a comment ∗/}
 *
 * @author Julio Vilmar Gesser
 */
public class TraditionalJavadocComment extends JavadocComment {

    public TraditionalJavadocComment() {
        this(null, "empty");
    }

    @AllFieldsConstructor
    public TraditionalJavadocComment(String content) {
        this(null, content);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public TraditionalJavadocComment(TokenRange tokenRange, String content) {
        super(tokenRange, content);
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
    public Javadoc parse() {
        return parseJavadoc(getContent(), false);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public TraditionalJavadocComment clone() {
        return (TraditionalJavadocComment) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public TraditionalJavadocCommentMetaModel getMetaModel() {
        return JavaParserMetaModel.traditionalJavadocCommentMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isTraditionalJavadocComment() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public TraditionalJavadocComment asTraditionalJavadocComment() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifTraditionalJavadocComment(Consumer<TraditionalJavadocComment> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<TraditionalJavadocComment> toTraditionalJavadocComment() {
        return Optional.of(this);
    }

    @Override
    public String getHeader() {
        return "/**";
    }

    @Override
    public String getFooter() {
        return "*/";
    }
}
