/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.model;

import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Optional;

/**
 * Placeholder used to represent a lambda argument type while it is being
 * calculated.
 *
 * @author Federico Tomassetti
 */
public class LambdaArgumentTypePlaceholder implements ResolvedType {

    private int pos;

    private final Optional<Integer> parameterCount;

    private final Optional<Boolean> bodyBlockHasExplicitNonVoidReturn;

    private SymbolReference<? extends ResolvedMethodLikeDeclaration> method;

    public LambdaArgumentTypePlaceholder(int pos) {
        this.pos = pos;
        this.parameterCount = Optional.empty();
        this.bodyBlockHasExplicitNonVoidReturn = Optional.empty();
    }

    public LambdaArgumentTypePlaceholder(
            int pos, int parameterCount, Optional<Boolean> bodyBlockHasExplicitNonVoidReturn) {
        this.pos = pos;
        this.parameterCount = Optional.of(parameterCount);
        this.bodyBlockHasExplicitNonVoidReturn = bodyBlockHasExplicitNonVoidReturn;
    }

    public Optional<Integer> getParameterCount() {
        return parameterCount;
    }

    public Optional<Boolean> bodyBlockHasExplicitNonVoidReturn() {
        return bodyBlockHasExplicitNonVoidReturn;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isReferenceType() {
        return false;
    }

    @Override
    public String describe() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTypeVariable() {
        return false;
    }

    public void setMethod(SymbolReference<? extends ResolvedMethodLikeDeclaration> method) {
        this.method = method;
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        throw new UnsupportedOperationException();
    }
}
