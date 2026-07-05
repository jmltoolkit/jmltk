/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 14 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/14/">https://openjdk.java.net/projects/jdk/14/</a>
 */
public class Java14PreviewValidator extends Java14Validator {

    public Java14PreviewValidator() {
        super();
        // Incubator
        // No new incubator language features added within Java 14
        // Preview
        // Pattern Matching for instanceof - first preview within Java 14 - https://openjdk.java.net/jeps/305
        remove(noPatternMatchingInstanceOf);
        {
            // first preview within Java 14 - https://openjdk.java.net/jeps/359
            remove(noRecordDeclaration);
            add(recordAsTypeIdentifierNotAllowed);
            add(recordDeclarationValidator);
        }
        // 2nd Preview
        // Text Block Literals - 2nd preview within Java 14 - https://openjdk.java.net/jeps/378
        remove(noTextBlockLiteral);
    }
}
