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
 * A node that implements other types.
 */
public interface NodeWithImplements<N extends Node> {

    NodeList<ClassOrInterfaceType> getImplementedTypes();

    default ClassOrInterfaceType getImplementedTypes(int i) {
        return getImplementedTypes().get(i);
    }

    N setImplementedTypes(NodeList<ClassOrInterfaceType> implementsList);

    void tryAddImportToParentCompilationUnit(Class<?> clazz);

    @SuppressWarnings("unchecked")
    default N setImplementedType(int i, ClassOrInterfaceType implement) {
        getImplementedTypes().set(i, implement);
        return (N) this;
    }

    @SuppressWarnings("unchecked")
    default N addImplementedType(ClassOrInterfaceType implement) {
        getImplementedTypes().add(implement);
        return (N) this;
    }

    /**
     * @deprecated use addImplementedType instead
     */
    default N addImplements(String name) {
        return addImplementedType(name);
    }

    /**
     * @deprecated use addImplementedType instead
     */
    default N addImplements(Class<?> clazz) {
        return addImplementedType(clazz);
    }

    /**
     * Add an implements to this
     *
     * @param name the name of the type to extends from
     * @return this
     */
    @SuppressWarnings("unchecked")
    default N addImplementedType(String name) {
        getImplementedTypes().add(parseClassOrInterfaceType(name));
        return (N) this;
    }

    /**
     * Add an implements to this and automatically add the import
     *
     * @param clazz the type to implements from
     * @return this
     */
    default N addImplementedType(Class<?> clazz) {
        tryAddImportToParentCompilationUnit(clazz);
        return addImplementedType(clazz.getSimpleName());
    }
}
