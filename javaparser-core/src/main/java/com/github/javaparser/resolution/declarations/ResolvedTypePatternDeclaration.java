/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import com.github.javaparser.ast.expr.TypePatternExpr;

/**
 * Declaration of a type pattern expression.
 * <p>
 * WARNING: Implemented fairly blindly. Unsure if required or even appropriate. Use with extreme caution.
 *
 * @author Roger Howell
 * @see TypePatternExpr
 */
public interface ResolvedTypePatternDeclaration extends ResolvedValueDeclaration {

    @Override
    default boolean isTypePattern() {
        return true;
    }

    @Override
    default ResolvedTypePatternDeclaration asTypePattern() {
        return this;
    }

    @Override
    default boolean hasName() {
        return true;
    }

    default String describeType() {
        return getType().describe();
    }
}
