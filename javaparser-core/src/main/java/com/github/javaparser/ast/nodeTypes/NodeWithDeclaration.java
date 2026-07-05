/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

/**
 * Node with a declaration representable as a String.
 *
 * @author Federico Tomassetti
 * @since July 2014
 */
public interface NodeWithDeclaration {

    /**
     * As {@link NodeWithDeclaration#getDeclarationAsString(boolean, boolean, boolean)} including
     * the modifiers, the throws clause and the parameters with both type and name.
     *
     * @return String representation of declaration
     */
    default String getDeclarationAsString() {
        return getDeclarationAsString(true, true, true);
    }

    /**
     * As {@link NodeWithDeclaration#getDeclarationAsString(boolean, boolean, boolean)} including
     * the parameters with both type and name. The method declaration is returned without comment.
     *
     * @param includingModifiers flag to include the modifiers (if present) in the string produced
     * @param includingThrows    flag to include the throws clause (if present) in the string produced
     * @return String representation of declaration based on parameter flags
     */
    default String getDeclarationAsString(boolean includingModifiers, boolean includingThrows) {
        return getDeclarationAsString(includingModifiers, includingThrows, true);
    }

    /**
     * A simple representation of the element declaration.
     * It should fit one string.
     *
     * @param includingModifiers     flag to include the modifiers (if present) in the string produced
     * @param includingThrows        flag to include the throws clause (if present) in the string produced
     * @param includingParameterName flag to include the parameter name (while the parameter type is always included) in
     *                               the string produced
     * @return String representation of declaration based on parameter flags
     */
    String getDeclarationAsString(boolean includingModifiers, boolean includingThrows, boolean includingParameterName);
}
