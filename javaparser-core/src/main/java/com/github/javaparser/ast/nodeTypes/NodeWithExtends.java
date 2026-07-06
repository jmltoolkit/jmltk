/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;

/**
 * A node that explicitly extends other types, using the {@code extends} keyword.
 */
public interface NodeWithExtends<N extends Node> {

    /**
     * @return All extended types that have been explicitly added (thus exist within the AST).
     *   Note that this can contain more than one item if this is an interface.
     *   Note that this will not include {@code java.lang.Object} unless it is explicitly added (e.g. {@code class X extends Object {}})
     *   If you want the implicitly extended types, you will need a resolved reference.
     */
    NodeList<ClassOrInterfaceType> getExtendedTypes();

    void tryAddImportToParentCompilationUnit(Class<?> clazz);

    default ClassOrInterfaceType getExtendedTypes(int i) {
        return getExtendedTypes().get(i);
    }

    N setExtendedTypes(NodeList<ClassOrInterfaceType> extendsList);

    @SuppressWarnings("unchecked")
    default N setExtendedType(int i, ClassOrInterfaceType extend) {
        getExtendedTypes().set(i, extend);
        return (N) this;
    }

    @SuppressWarnings("unchecked")
    default N addExtendedType(ClassOrInterfaceType extend) {
        getExtendedTypes().add(extend);
        return (N) this;
    }

    /**
     * @deprecated use addExtendedType
     */
    @Deprecated
    default N addExtends(Class<?> clazz) {
        return addExtendedType(clazz);
    }

    /**
     * @deprecated use addExtendedType
     */
    @Deprecated
    default N addExtends(String name) {
        return addExtendedType(name);
    }

    /**
     * Add an "extends" to this and automatically add the import
     *
     * @param clazz the class to extend from
     * @return this
     */
    default N addExtendedType(Class<?> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addExtendedType(clazz.getSimpleName());
    }

    /**
     * Add an "extends" to this
     *
     * @param name the name of the type to extends from
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addExtendedType(String name) {
        getExtendedTypes().add(parseClassOrInterfaceType(name));
        return (N) this;
    }
}
