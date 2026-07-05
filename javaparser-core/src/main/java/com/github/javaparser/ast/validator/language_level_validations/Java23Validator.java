/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * Validator for Java 23 language features.
 * Java 23 does not introduce new syntax changes that affect parsing,
 * so this validator simply extends Java 22.
 */
public class Java23Validator extends Java22Validator {

    public Java23Validator() {
        super();
    }
}
