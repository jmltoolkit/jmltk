/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.validator.SingleNodeTypeValidator;
import com.github.javaparser.ast.validator.Validator;
import com.github.javaparser.ast.validator.language_level_validations.chunks.ModifierValidator;

/**
 * This validator validates according to Java 8 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk8/">https://openjdk.java.net/projects/jdk8/</a>
 * @see <a href="https://openjdk.java.net/projects/jdk8/features">https://openjdk.java.net/projects/jdk8/features</a>
 */
public class Java8Validator extends Java7Validator {

    final Validator modifiersWithoutPrivateInterfaceMethods = new ModifierValidator(true, true, false);

    final Validator defaultMethodsInInterface = new SingleNodeTypeValidator<>(ClassOrInterfaceDeclaration.class, (n, reporter) -> {
        if (n.isInterface()) {
            n.getMethods().forEach(m -> {
                if (m.isDefault() && !m.getBody().isPresent()) {
                    reporter.report(m, "'default' methods must have a body.");
                }
            });
        }
    });

    public Java8Validator() {
        super();
        replace(modifiersWithoutDefaultAndStaticInterfaceMethodsAndPrivateInterfaceMethods, modifiersWithoutPrivateInterfaceMethods);
        add(defaultMethodsInInterface);
        remove(noLambdas);
        // TODO validate more annotation locations http://openjdk.java.net/jeps/104
        // TODO validate repeating annotations http://openjdk.java.net/jeps/120
    }
}
