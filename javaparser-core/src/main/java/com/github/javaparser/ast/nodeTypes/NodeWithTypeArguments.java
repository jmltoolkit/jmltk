/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.ast.NodeList.nodeList;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.DerivedProperty;
import java.util.Optional;

/**
 * A node that can have type arguments.
 * <p>
 * <pre>
 *     new X();        --&gt; typeArguments == Optional is empty
 *     new X&lt;&gt;();      --&gt; typeArguments = [], diamondOperator = true
 *     new X&lt;C,D&gt;();   --&gt; typeArguments = [C,D], diamondOperator = false
 * </pre>
 * Only ObjectCreationExpr uses the diamond operator.
 * On other nodes it is treated the same as the first case.
 */
public interface NodeWithTypeArguments<N extends Node> {

    /**
     * @return the types that can be found in the type arguments: {@code  <String, Integer>}.
     */
    Optional<NodeList<Type>> getTypeArguments();

    /**
     * Allows you to set the generic arguments
     *
     * @param typeArguments The list of types of the generics, can be null
     */
    N setTypeArguments(NodeList<Type> typeArguments);

    /**
     * @return whether the type arguments look like {@code <>}.
     */
    @DerivedProperty
    default boolean isUsingDiamondOperator() {
        return getTypeArguments().isPresent() && getTypeArguments().get().isEmpty();
    }

    /**
     * Sets the type arguments to {@code <>}.
     */
    @SuppressWarnings("unchecked")
    default N setDiamondOperator() {
        return setTypeArguments(new NodeList<>());
    }

    /**
     * Removes all type arguments, including the surrounding {@code <>}.
     */
    @SuppressWarnings("unchecked")
    default N removeTypeArguments() {
        return setTypeArguments((NodeList<Type>) null);
    }

    @SuppressWarnings("unchecked")
    default N setTypeArguments(Type... typeArguments) {
        return setTypeArguments(nodeList(typeArguments));
    }
}
