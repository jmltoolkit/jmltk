/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ast.validator.ReservedKeywordValidator;
import com.github.javaparser.ast.validator.Validator;
import com.github.javaparser.ast.validator.language_level_validations.chunks.ModifierValidator;

/**
 * This validator validates according to Java 1.2 syntax rules.
 */
public class Java1_2Validator extends Java1_1Validator {

    final Validator modifiersWithoutDefaultAndStaticInterfaceMethodsAndPrivateInterfaceMethods = new ModifierValidator(true, false, false);

    final Validator strictfpNotAllowed = new ReservedKeywordValidator("strictfp");

    public Java1_2Validator() {
        super();
        replace(modifiersWithoutStrictfpAndDefaultAndStaticInterfaceMethodsAndPrivateInterfaceMethods, modifiersWithoutDefaultAndStaticInterfaceMethodsAndPrivateInterfaceMethods);
        add(strictfpNotAllowed);
    }
}
