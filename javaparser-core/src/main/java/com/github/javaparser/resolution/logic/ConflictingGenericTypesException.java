/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.logic;

import com.github.javaparser.resolution.types.ResolvedType;

/**
 * @author Federico Tomassetti
 */
public class ConflictingGenericTypesException extends RuntimeException {

    public ConflictingGenericTypesException(ResolvedType formalType, ResolvedType actualType) {
        super(String.format("No matching between %s (formal) and %s (actual)", formalType, actualType));
    }
}
