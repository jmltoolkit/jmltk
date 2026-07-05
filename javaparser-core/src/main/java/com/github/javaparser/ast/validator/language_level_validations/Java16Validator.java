/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 16 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/16/">https://openjdk.java.net/projects/jdk/16/</a>
 */
public class Java16Validator extends Java15Validator {

    public Java16Validator() {
        super();
        // Released Language Features
        // Pattern Matching for instanceof released within Java 16 - https://openjdk.java.net/jeps/305
        remove(noPatternMatchingInstanceOf);
        {
            // Records released within Java 16 - https://openjdk.java.net/jeps/395
            remove(noRecordDeclaration);
            // local interface released within Java 16 -
            // https://docs.oracle.com/javase/specs/jls/se16/html/jls-14.html#jls-14.3
            remove(innerClasses);
            add(recordAsTypeIdentifierNotAllowed);
            add(recordDeclarationValidator);
        }
    }
}
