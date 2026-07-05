/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 16 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/16/">https://openjdk.java.net/projects/jdk/16/</a>
 */
public class Java16PreviewValidator extends Java16Validator {

    public Java16PreviewValidator() {
        super();
        // Incubator
        // No new incubator language features added in Java 16
        // Preview
        // No new preview language features added in Java 16
        // 2nd Preview
        // TODO: remove(noSealedClasses); // Sealed Classes - 2nd preview in Java 16 - https://openjdk.java.net/jeps/397
    }
}
