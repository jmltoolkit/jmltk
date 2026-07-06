/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.validator.SingleNodeTypeValidator;
import com.github.javaparser.ast.validator.Validator;
import com.github.javaparser.ast.validator.language_level_validations.chunks.VarValidator;

/**
 * This validator validates according to Java 10 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/10/">https://openjdk.java.net/projects/jdk/10/</a>
 */
public class Java10Validator extends Java9Validator {

    final Validator varOnlyOnLocalVariableDefinitionAndForAndTry = new SingleNodeTypeValidator<>(VarType.class, new VarValidator(false));

    public Java10Validator() {
        super();
        // Released Language Features
        {
            /*
             * Java 10 released local variable type inference in for and try-with (JEP286).
             * Java 11 released local variable type inference for lambda parameters also (JEP323)
             */
            add(varOnlyOnLocalVariableDefinitionAndForAndTry);
        }
    }
}
