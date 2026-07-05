/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.promotion;

import com.github.javaparser.resolution.promotion.ConditionalExprHandler;
import com.github.javaparser.resolution.types.ResolvedType;

/*
 * Boolean conditional expressions are standalone expressions
 * The type of a boolean conditional expression is determined as follows:
 * If the second and third operands are both of type Boolean, the conditional expression has type Boolean.
 * Otherwise, the conditional expression has type boolean.
 */
public class BooleanConditionalExprHandler implements ConditionalExprHandler {

    ResolvedType thenExpr;

    ResolvedType elseExpr;

    public BooleanConditionalExprHandler(ResolvedType thenExpr, ResolvedType elseExpr) {
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public ResolvedType resolveType() {
        if (thenExpr.isReferenceType() && elseExpr.isReferenceType()) {
            return thenExpr.asReferenceType();
        }
        return thenExpr.isPrimitive() ? thenExpr : elseExpr;
    }
}
