/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

public interface SymbolResolver {

    /**
     * For a reference it would find the corresponding declaration.
     */
    <T> T resolveDeclaration(Node node, Class<T> resultClass);

    /**
     * For types it would find the corresponding resolved types.
     */
    <T> T toResolvedType(Type javaparserType, Class<T> resultClass);

    /**
     * For an expression it would find the corresponding resolved type.
     */
    ResolvedType calculateType(Expression expression);

    /**
     * For a node it would find the corresponding reference type declaration.
     */
    ResolvedReferenceTypeDeclaration toTypeDeclaration(Node node);
}
