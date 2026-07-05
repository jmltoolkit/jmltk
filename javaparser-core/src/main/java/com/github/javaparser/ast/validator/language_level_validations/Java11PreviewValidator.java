/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 11 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/11/">https://openjdk.java.net/projects/jdk/11/</a>
 */
public class Java11PreviewValidator extends Java11Validator {

    public Java11PreviewValidator() {
        super();
        // Incubator
        // No incubator language features added within Java 11
        // Preview
        // No preview language features added within Java 11
        // 2nd Preview
        // No 2nd preview language features added within Java 11
    }
}
