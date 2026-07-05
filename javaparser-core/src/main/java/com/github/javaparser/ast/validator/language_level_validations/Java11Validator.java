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
 * This validator validates according to Java 11 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/11/">https://openjdk.java.net/projects/jdk/11/</a>
 */
public class Java11Validator extends Java10Validator {

    final Validator varAlsoInLambdaParameters = new SingleNodeTypeValidator<>(VarType.class, new VarValidator(true));

    public Java11Validator() {
        super();
        {
            /*
             * Java 10 released local variable type inference in for and try-with (JEP286).
             * Java 11 released local variable type inference for lambda parameters also (JEP323)
             */
            replace(varOnlyOnLocalVariableDefinitionAndForAndTry, varAlsoInLambdaParameters);
        }
    }
}
