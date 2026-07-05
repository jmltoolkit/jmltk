/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.javaparser.ast.Node;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public interface AssociableToASTTest {

    /**
     * Helper method to cast the instance to the correct {@link Class}.
     *
     * @param instance  The instance to be casted.
     * @param clazz     The expected {@link Class}.
     * @param <T>       The expected type.
     *
     * @return The instance casted as the correct type.
     */
    default <T extends AssociableToAST> T safeCast(AssociableToAST instance, Class<T> clazz) {
        if (clazz.isInstance(instance)) return clazz.cast(instance);
        throw new UnsupportedOperationException(
                String.format("Unable to cast %s into %s.", instance.getClass().getName(), clazz.getName()));
    }

    /**
     * Create a new instance of {@link AssociableToAST} to be used for testing.
     *
     * @return The created instance.
     */
    AssociableToAST createValue();

    /**
     * Get the node that can be associated with an AST.
     *
     * @param associableToAST The node that can be associated with an AST.
     *
     * @return The node being wrapped.
     */
    Optional<Node> getWrappedDeclaration(AssociableToAST associableToAST);

    @Test
    default void checkThatToASTMatchesTheCorrectWrappedNode() {
        AssociableToAST associableToAST = createValue();
        Optional<Node> wrappedNode = getWrappedDeclaration(associableToAST);
        if (wrappedNode.isPresent()) assertEquals(wrappedNode, associableToAST.toAst());
        else assertFalse(associableToAST.toAst().isPresent());
    }

    @Test
    default void checkThatToASTWithCorrectTypeMatchesTheCorrectWrappedNode() {
        AssociableToAST associableToAST = createValue();
        Optional<Node> wrappedNode = getWrappedDeclaration(associableToAST);
        if (wrappedNode.isPresent())
            assertEquals(wrappedNode, associableToAST.toAst(wrappedNode.get().getClass()));
        else assertFalse(associableToAST.toAst().isPresent());
    }
}
