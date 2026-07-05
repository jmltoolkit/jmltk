/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 17 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/17/">https://openjdk.java.net/projects/jdk/17/</a>
 */
public class Java17PreviewValidator extends Java17Validator {

    public Java17PreviewValidator() {
        super();
        // Incubator
        // No new incubator language features added in Java 17
        // Preview
        // No new preview language features added in Java 17
        // 2nd Preview
        // No new 2nd preview language features added in Java 17
    }
}
