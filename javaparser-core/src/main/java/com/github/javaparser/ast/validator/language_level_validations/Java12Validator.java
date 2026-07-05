/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 12 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/12/">https://openjdk.java.net/projects/jdk/12/</a>
 */
public class Java12Validator extends Java11Validator {

    public Java12Validator() {
        super();
        // Released Language Features
        // No new released language features added within Java 12
    }
}
