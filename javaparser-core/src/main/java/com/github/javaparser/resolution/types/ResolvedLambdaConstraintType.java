/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.types;

public class ResolvedLambdaConstraintType implements ResolvedType {

    private ResolvedType bound;

    private ResolvedLambdaConstraintType(ResolvedType bound) {
        this.bound = bound;
    }

    @Override
    public String describe() {
        return "? super " + bound.describe();
    }

    public ResolvedType getBound() {
        return bound;
    }

    @Override
    public boolean isConstraint() {
        return true;
    }

    @Override
    public ResolvedLambdaConstraintType asConstraintType() {
        return this;
    }

    public static ResolvedLambdaConstraintType bound(ResolvedType bound) {
        return new ResolvedLambdaConstraintType(bound);
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        return bound.isAssignableBy(other);
    }

    @Override
    public String toString() {
        return "LambdaConstraintType{" + "bound=" + bound + '}';
    }
}
