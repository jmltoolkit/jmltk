/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 15 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/15/">https://openjdk.java.net/projects/jdk/15/</a>
 */
public class Java15Validator extends Java14Validator {

    public Java15Validator() {
        super();
        // Released Language Features
        // Text Block Literals - released within Java 15 - https://openjdk.java.net/jeps/378
        remove(noTextBlockLiteral);
    }
}
