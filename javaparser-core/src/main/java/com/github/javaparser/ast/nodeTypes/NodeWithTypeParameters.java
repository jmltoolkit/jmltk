/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.TypeParameter;

import static com.github.javaparser.StaticJavaParser.parseTypeParameter;

/**
 * A node that can have type parameters.
 * <pre>
 *     class X {}        --&gt; typeParameters == []
 *     class X&lt;&gt; {}      --&gt; does not occur.
 *     class X&lt;C,D&gt; {}   --&gt; typeParameters = [C,D]
 * </pre>
 */
public interface NodeWithTypeParameters<N extends Node> {

    NodeList<TypeParameter> getTypeParameters();

    default TypeParameter getTypeParameter(int i) {
        return getTypeParameters().get(i);
    }

    @SuppressWarnings("unchecked")
    default N setTypeParameter(int i, TypeParameter typeParameter) {
        getTypeParameters().set(i, typeParameter);
        return (N) this;
    }

    @SuppressWarnings("unchecked")
    default N addTypeParameter(TypeParameter typeParameter) {
        getTypeParameters().add(typeParameter);
        return (N) this;
    }

    /**
     * Adds a type parameter like {@code X extends Serializable}
     */
    default N addTypeParameter(String typeParameter) {
        return addTypeParameter(parseTypeParameter(typeParameter));
    }

    N setTypeParameters(NodeList<TypeParameter> typeParameters);

    default boolean isGeneric() {
        return getTypeParameters().size() > 0;
    }
}
