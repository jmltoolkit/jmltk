/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.core.resolution;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;

/**
 * Allows for an implementing declaration type to support solving for field <i>(symbol)</i> usage.
 */
public interface SymbolResolutionCapability {
    /**
     * @param name Field / symbol name.
     * @param typeSolver Symbol solver to resolve type usage.
     * @return Symbol reference of the resolved value.
     */
    SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, TypeSolver typeSolver);
}
