/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution;

/**
 * It is not possible to decide how to resolve a method invocation.
 *
 * @author Federico Tomassetti
 */
public class MethodAmbiguityException extends RuntimeException {

    /**
     * Create an instance from a description of the reason why there is ambiguity in this particular case.
     */
    public MethodAmbiguityException(String description) {
        super(description);
    }
}
