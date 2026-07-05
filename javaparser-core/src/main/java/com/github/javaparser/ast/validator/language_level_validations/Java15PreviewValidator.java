/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 15 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/15/">https://openjdk.java.net/projects/jdk/15/</a>
 */
public class Java15PreviewValidator extends Java15Validator {

    public Java15PreviewValidator() {
        super();
        // Incubator
        // No new incubator language features added within Java 15
        // Preview
        // remove(noSealedClasses); // Sealed Classes - first preview within Java 15 - https://openjdk.java.net/jeps/360
        // 2nd Preview
        // Pattern Matching for instanceof - 2nd preview in Java 15 - https://openjdk.java.net/jeps/305
        remove(noPatternMatchingInstanceOf);
        {
            // Records - 2nd preview within Java 15 - https://openjdk.java.net/jeps/384
            remove(noRecordDeclaration);
            add(recordAsTypeIdentifierNotAllowed);
            add(recordDeclarationValidator);
        }
    }
}
