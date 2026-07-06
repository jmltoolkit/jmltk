/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.validator.SimpleValidator;
import com.github.javaparser.ast.validator.Validator;

/**
 * This validator validates according to Java 17 syntax rules.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/17/">https://openjdk.java.net/projects/jdk/17/</a>
 */
public class Java17Validator extends Java16Validator {

    final Validator sealedNotAllowedAsIdentifier = new SimpleValidator<>(
            ClassOrInterfaceDeclaration.class,
            n -> n.getName().getIdentifier().equals("sealed"),
            (n, reporter) -> reporter.report(
                    n,
                    new UpgradeJavaMessage(
                            "'sealed' identifier is not authorised in this context.",
                            ParserConfiguration.LanguageLevel.JAVA_17)));

    final Validator permitsNotAllowedAsIdentifier = new SimpleValidator<>(
            ClassOrInterfaceDeclaration.class,
            n -> n.getName().getIdentifier().equals("permits"),
            (n, reporter) -> reporter.report(
                    n,
                    new UpgradeJavaMessage(
                            "'permits' identifier is not authorised in this context.",
                            ParserConfiguration.LanguageLevel.JAVA_17)));

    public Java17Validator() {
        super();
        // Released Language Features
        // Sealed Classes - https://openjdk.java.net/jeps/409
        add(sealedNotAllowedAsIdentifier);
        add(permitsNotAllowedAsIdentifier);
        remove(noSealedClasses);
        remove(noPermitsListInClasses);
    }
}
