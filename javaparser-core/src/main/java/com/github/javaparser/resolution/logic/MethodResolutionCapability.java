/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.logic;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;

public interface MethodResolutionCapability {

    SymbolReference<ResolvedMethodDeclaration> solveMethod(
            String name,
            List<ResolvedType> argumentsTypes,
            boolean staticOnly,
            ResolvedReferenceTypeDeclaration invocationContext);
}
