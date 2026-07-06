/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.comments;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JavadocCommentMetaModel;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.StaticJavaParser.parseJavadoc;
import com.github.javaparser.ast.Node;

public abstract class JavadocComment extends Comment {

    public JavadocComment() {
        this(null, "empty");
    }

    @AllFieldsConstructor
    public JavadocComment(String content) {
        this(null, content);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JavadocComment(TokenRange tokenRange, String content) {
        super(tokenRange, content);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isJavadocComment() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public JavadocComment asJavadocComment() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<JavadocComment> toJavadocComment() {
        return Optional.of(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifJavadocComment(Consumer<JavadocComment> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JavadocComment clone() {
        return (JavadocComment) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JavadocCommentMetaModel getMetaModel() {
        return JavaParserMetaModel.javadocCommentMetaModel;
    }

    public Javadoc parse() {
        return parseJavadoc(getContent(), this.isMarkdownComment());
    }
}
