/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.model.typesystem;

import com.github.javaparser.resolution.types.ResolvedType;

/**
 * This is a virtual type used to represent null values.
 *
 * @author Federico Tomassetti
 */
public class NullType implements ResolvedType {

    public static final NullType INSTANCE = new NullType();

    private NullType() {
        // prevent instantiation
    }

    @Override
    public boolean isArray() {
        return false;
    }

    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isReferenceType() {
        return false;
    }

    @Override
    public String describe() {
        return "null";
    }

    @Override
    public boolean isTypeVariable() {
        return false;
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        throw new UnsupportedOperationException("It does not make sense to assign a value to null, it can only be assigned");
    }
}
