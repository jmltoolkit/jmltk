/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.MethodUsageResolutionCapability;

import java.util.List;
import java.util.Optional;

/**
 * @author Federico Tomassetti
 */
public class ContextHelper {

    private ContextHelper() {
        // prevent instantiation
    }

    public static Optional<MethodUsage> solveMethodAsUsage(
            ResolvedTypeDeclaration typeDeclaration,
            String name,
            List<ResolvedType> argumentsTypes,
            Context invokationContext,
            List<ResolvedType> typeParameters,
            ResolvedReferenceTypeDeclaration callContext) {

        if (typeDeclaration instanceof MethodUsageResolutionCapability) {
            return ((MethodUsageResolutionCapability) typeDeclaration)
                    .solveMethodAsUsage(name, argumentsTypes, invokationContext, typeParameters, callContext);
        }
        throw new UnsupportedOperationException(typeDeclaration.toString());
    }
}
