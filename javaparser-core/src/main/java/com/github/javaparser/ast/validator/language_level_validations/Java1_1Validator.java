/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.validator.SingleNodeTypeValidator;
import com.github.javaparser.ast.validator.Validator;

/**
 * This validator validates according to Java 1.1 syntax rules.
 */
public class Java1_1Validator extends Java1_0Validator {

    final Validator innerClasses =
            new SingleNodeTypeValidator<>(ClassOrInterfaceDeclaration.class, (n, reporter) -> n.getParentNode()
                    .ifPresent(p -> {
                        if (p instanceof LocalClassDeclarationStmt && n.isInterface())
                            reporter.report(
                                    n,
                                    new UpgradeJavaMessage(
                                            "There is no such thing as a local interface.",
                                            ParserConfiguration.LanguageLevel.JAVA_16));
                    }));

    public Java1_1Validator() {
        super();
        replace(noInnerClasses, innerClasses);
        remove(noReflection);
    }
}
