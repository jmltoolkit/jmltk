/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNonEmpty;

/**
 * A node with a name.
 * <p>
 * The main reason for this interface is to permit users to manipulate homogeneously all nodes with a getName method.
 */
@NullMarked
public interface NodeWithOptionalSimpleName<N extends Node> {

    Optional<SimpleName> getName();

    N setName(@Nullable SimpleName name);

    @SuppressWarnings("unchecked")
    default N setName(@Nullable String name) {
        assertNonEmpty(name);
        return setName(new SimpleName(name));
    }

    default Optional<String> getNameAsString() {
        return getName().map(SimpleName::getIdentifier);
    }

    default Optional<NameExpr> getNameAsExpression() {
        return getName().map(NameExpr::new);
    }

    @Nullable
    SimpleName name();

    @Nullable
    default String nameAsString() {
        return name() != null ? name().getIdentifier() : null;
    }

    @Nullable
    default NameExpr nameAsExpression() {
        return name() != null ? new NameExpr(name()) : null;
    }
}
