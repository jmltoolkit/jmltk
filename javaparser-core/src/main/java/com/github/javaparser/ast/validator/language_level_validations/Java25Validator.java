/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * Validator for Java 25 language features:
 * - Module imports {@see https://openjdk.org/jeps/511}
 * - Compact class declarations (WIP) {@see https://openjdk.org/jeps/512}
 * - Flexible constructor bodies (WIP) {@see https://openjdk.org/jeps/513}
 */
public class Java25Validator extends Java24Validator {

    public Java25Validator() {
        super();
        remove(noModuleImports);
        remove(explicitConstructorInvocationMustBeFirstStatement);
    }
}
