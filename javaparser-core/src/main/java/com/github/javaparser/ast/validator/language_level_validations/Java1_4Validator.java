/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 1.4 syntax rules.
 */
public class Java1_4Validator extends Java1_3Validator {

    public Java1_4Validator() {
        super();
        remove(noAssertKeyword);
        add(noAssertIdentifer);
    }
}
