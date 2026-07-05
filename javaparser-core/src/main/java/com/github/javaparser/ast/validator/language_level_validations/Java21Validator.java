/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 21 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/21/">https://openjdk.java.net/projects/jdk/21/</a>
 */
public class Java21Validator extends Java20Validator {

    public Java21Validator() {
        super();
        remove(noSwitchNullDefault);
        remove(noSwitchPatterns);
        remove(noRecordPatterns);
    }
}
