/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ReferenceType;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

/**
 * A node that declares the types of exception it throws.
 */
public interface NodeWithThrownExceptions<N extends Node> {

    N setThrownExceptions(NodeList<ReferenceType> thrownExceptions);

    NodeList<ReferenceType> getThrownExceptions();

    void tryAddImportToParentCompilationUnit(Class<?> clazz);

    default ReferenceType getThrownException(int i) {
        return getThrownExceptions().get(i);
    }

    /**
     * Adds this type to the throws clause
     *
     * @param throwType the exception type
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addThrownException(ReferenceType throwType) {
        getThrownExceptions().add(throwType);
        return (N) this;
    }

    /**
     * Adds this class to the throws clause
     *
     * @param clazz the exception class
     * @return this
     */
    default N addThrownException(Class<? extends Throwable> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addThrownException(parseClassOrInterfaceType(clazz.getSimpleName()));
    }

    /**
     * Check whether this elements throws this exception class.
     * Note that this is simply a text compare of the simple name of the class,
     * no actual type resolution takes place.
     *
     * @param clazz the class of the exception
     * @return true if found in throws clause, false if not
     */
    default boolean isThrown(Class<? extends Throwable> clazz) {
        return isThrown(clazz.getSimpleName());
    }

    /**
     * Check whether this elements throws this exception class
     * Note that this is simply a text compare,
     * no actual type resolution takes place.
     *
     * @param throwableName the class of the exception
     * @return true if found in throws clause, false if not
     */
    default boolean isThrown(String throwableName) {
        return getThrownExceptions().stream().anyMatch(t -> t.toString().equals(throwableName));
    }
}
