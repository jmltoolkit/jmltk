/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.promotion;

import com.github.javaparser.resolution.promotion.ConditionalExprHandler;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typeinference.TypeHelper;

import java.util.Arrays;
import java.util.HashSet;

public class ReferenceConditionalExprHandler implements ConditionalExprHandler {

    ResolvedType thenExpr;

    ResolvedType elseExpr;

    public ReferenceConditionalExprHandler(ResolvedType thenExpr, ResolvedType elseExpr) {
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public ResolvedType resolveType() {
        // If one of the second and third operands is of the null type and the type of the other is a reference type,
        // then the type of the conditional expression is that reference type.
        if (thenExpr.isNull()) {
            return elseExpr;
        }
        if (elseExpr.isNull()) {
            return thenExpr;
        }
        return TypeHelper.leastUpperBound(new HashSet<>(Arrays.asList(thenExpr, elseExpr)));
    }
}
