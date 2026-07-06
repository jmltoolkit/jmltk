/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.Type;
import static com.github.javaparser.StaticJavaParser.parseType;
import static com.github.javaparser.utils.Utils.assertNonEmpty;

/**
 * A node with a type.
 * <p>
 * The main reason for this interface is to permit users to manipulate homogeneously all nodes with getType/setType
 * methods
 *
 * @since 2.3.1
 */
public interface NodeWithType<N extends Node, T extends Type> {

    /**
     * Gets the type
     *
     * @return the type
     */
    T getType();

    /**
     * Sets the type
     *
     * @param type the type
     * @return this
     */
    N setType(T type);

    void tryAddImportToParentCompilationUnit(Class<?> clazz);

    /**
     * Sets this type to this class and try to import it to the {@link CompilationUnit} if needed
     *
     * @param typeClass the type
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N setType(Class<?> typeClass) {
        tryAddImportToParentCompilationUnit(typeClass);
        return setType((T) parseType(typeClass.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    default N setType(final String typeString) {
        assertNonEmpty(typeString);
        return setType((T) parseType(typeString));
    }

    default String getTypeAsString() {
        return getType().asString();
    }
}
