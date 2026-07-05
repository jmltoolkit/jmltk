/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.MarkdownComment;
import com.github.javaparser.ast.comments.TraditionalJavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import java.util.Optional;

/**
 * A node that can be documented with a Javadoc comment.
 */
public interface NodeWithJavadoc<N extends Node> {

    Optional<Comment> getComment();

    Node setComment(Comment comment);

    /**
     * Gets the JavadocComment for this node. You can set the JavadocComment by calling setJavadocComment passing a
     * JavadocComment.
     *
     * @return The JavadocComment for this node wrapped in an optional as it may be absent.
     */
    default Optional<JavadocComment> getJavadocComment() {
        return getComment().filter(comment -> comment instanceof JavadocComment).map(comment ->
                (JavadocComment) comment);
    }

    /**
     * Gets the Javadoc for this node. You can set the Javadoc by calling setJavadocComment passing a Javadoc.
     *
     * @return The Javadoc for this node wrapped in an optional as it may be absent.
     */
    default Optional<Javadoc> getJavadoc() {
        return getJavadocComment().map(JavadocComment::parse);
    }

    /**
     * Set a JavadocComment for this node
     */
    @SuppressWarnings("unchecked")
    default N setJavadocComment(String comment, boolean isMarkdownComment) {
        JavadocComment javadocComment =
                isMarkdownComment ? new MarkdownComment(comment) : new TraditionalJavadocComment(comment);
        return setJavadocComment(javadocComment);
    }

    /**
     * Set a JavadocComment for this node
     */
    default N setJavadocComment(String comment) {
        return setJavadocComment(comment, false);
    }

    default N setJavadocComment(JavadocComment comment) {
        setComment(comment);
        return (N) this;
    }

    default N setJavadocComment(String indentation, Javadoc javadoc) {
        return setJavadocComment(javadoc.toComment(indentation));
    }

    default N setJavadocComment(Javadoc javadoc) {
        return setJavadocComment(javadoc.toComment());
    }

    default boolean removeJavaDocComment() {
        return hasJavaDocComment() && getComment().get().remove();
    }

    default boolean hasJavaDocComment() {
        return getComment().isPresent() && getComment().get() instanceof JavadocComment;
    }
}
