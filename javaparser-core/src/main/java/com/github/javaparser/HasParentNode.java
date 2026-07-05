/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.Observable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * An object that can have a parent node.
 */
public interface HasParentNode<T> extends Observable {

    /**
     * Returns true if the parent has a parent
     */
    default boolean hasParentNode() {
        return getParentNode().isPresent();
    }

    /**
     * Returns the parent node, or {@code Optional.empty} if no parent is set.
     */
    Optional<Node> getParentNode();

    /**
     * Sets the parent node.
     *
     * @param parentNode the parent node, or {@code null} to set no parent.
     * @return {@code this}
     */
    T setParentNode(Node parentNode);

    /**
     * Returns the parent node from the perspective of the children of this node.
     * <p>
     * That is, this method returns {@code this} for everything except {@code NodeList}. A {@code NodeList} returns its
     * parent node instead. This is because a {@code NodeList} sets the parent of all its children to its own parent
     * node (see {@link com.github.javaparser.ast.NodeList} for details).
     */
    Node getParentNodeForChildren();

    /**
     * Walks the parents of this node and returns the first node of type {@code type}, or {@code empty()} if none is
     * found. The given type may also be an interface type, such as one of the {@code NodeWith...} interface types.
     */
    default <N> Optional<N> findAncestor(Class<N>... types) {
        return findAncestor(x -> true, types);
    }

    /**
     * Walks the parents of this node and returns the first node of type {@code type} that matches {@code predicate}, or
     * {@code empty()} if none is found. The given type may also be an interface type, such as one of the
     * {@code NodeWith...} interface types.
     * @deprecated
     * This method is no longer acceptable to find ancestor.
     * <p> Use {@link findAncestor(Predicate, Class)} instead.
     */
    @Deprecated
    default <N> Optional<N> findAncestor(Class<N> type, Predicate<N> predicate) {
        return findAncestor(predicate, type);
    }

    /**
     * Walks the parents of this node and returns the first node that matches one of types {@code types}, or
     * {@code empty()} if none is found. The given type may also be an interface type, such as one of the
     * {@code NodeWith...} interface types.
     * @param <N>
     */
    default <N> Optional<N> findAncestor(Predicate<N> predicate, Class<N>... types) {
        if (!hasParentNode()) return Optional.empty();
        Node parent = getParentNode().get();
        for (Class<N> type : types) {
            if (type.isAssignableFrom(parent.getClass()) && predicate.test(type.cast(parent))) {
                return Optional.of(type.cast(parent));
            }
        }
        return parent.findAncestor(predicate, types);
    }

    /**
     * Determines whether this {@code HasParentNode} node is a descendant of the given node. A node is <i>not</i> a
     * descendant of itself.
     *
     * @param ancestor the node for which to determine whether it has this node as an ancestor.
     * @return {@code true} if this node is a descendant of the given node, and {@code false} otherwise.
     * @see Node#isAncestorOf(Node)
     */
    default boolean isDescendantOf(Node ancestor) {
        return findAncestor(n -> n == ancestor, Node.class).isPresent();
    }
}
