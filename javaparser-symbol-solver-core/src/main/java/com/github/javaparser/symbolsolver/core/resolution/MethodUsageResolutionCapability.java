/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.core.resolution;

import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;
import java.util.Optional;

public interface MethodUsageResolutionCapability {
    Optional<MethodUsage> solveMethodAsUsage(
            String name,
            List<ResolvedType> argumentTypes,
            Context invocationContext,
            List<ResolvedType> typeParameters,
            ResolvedReferenceTypeDeclaration callContext);
}
