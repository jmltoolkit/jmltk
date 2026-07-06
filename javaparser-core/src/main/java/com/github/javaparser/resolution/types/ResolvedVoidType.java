/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.types;

/**
 * The special type void.
 *
 * @author Federico Tomassetti
 */
public class ResolvedVoidType implements ResolvedType {

    public static final ResolvedType INSTANCE = new ResolvedVoidType();

    private ResolvedVoidType() {
    }

    @Override
    public String describe() {
        return "void";
    }

    @Override
    public boolean isAssignableBy(ResolvedType other) {
        // According to https://docs.oracle.com/javase/specs/jls/se16/html/jls-14.html#jls-14.8:
        // """
        // Note that the Java programming language does not allow a "cast to void" - void is not a type - so the
        // traditional C trick of writing an expression statement such as:
        //
        // (void)... ;  // incorrect!
        //
        // does not work.
        // """
        //
        // In short, nothing can be assign to "void".
        return false;
    }

    @Override
    public boolean isVoid() {
        return true;
    }

    @Override
    public String toDescriptor() {
        return "V";
    }
}
