/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.model;

import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Any type of value.
 *
 * @author Federico Tomassetti
 */
public class Value {

    private ResolvedType type;

    private String name;

    public Value(ResolvedType type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Create a Value from a ValueDeclaration.
     */
    public static Value from(ResolvedValueDeclaration decl) {
        ResolvedType type = decl.getType();
        return new Value(type, decl.getName());
    }

    @Override
    public String toString() {
        return "Value{" + "type=" + type + ", name='" + name + '\'' + '}';
    }

    public String getName() {
        return name;
    }

    public ResolvedType getType() {
        return type;
    }
}
