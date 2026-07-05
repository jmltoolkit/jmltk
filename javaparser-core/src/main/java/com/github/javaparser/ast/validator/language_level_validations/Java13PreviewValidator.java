/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations;

/**
 * This validator validates according to Java 13 syntax rules -- including incubator/preview/second preview features.
 *
 * @see <a href="https://openjdk.java.net/projects/jdk/13/">https://openjdk.java.net/projects/jdk/13/</a>
 */
public class Java13PreviewValidator extends Java13Validator {

    public Java13PreviewValidator() {
        super();
        // Incubator
        // No new incubator language features added within Java 13
        // Preview
        // Text Block Literals - first preview within Java 13 - https://openjdk.java.net/jeps/355
        remove(noTextBlockLiteral);
        // 2nd Preview
        {
            /*
             * Switch Expressions (2nd Preview) - 2nd Preview within Java 13 - https://openjdk.java.net/jeps/354
             * <ul>
             *     <li>Switch permissions are added within this preview.</li>
             *     <li>Multiple labels is NOT YET PERMITTED -- introduced within Java 14 release.</li>
             *     <li>Yield keyword -- introduced within Java 13 preview.</li>
             * </ul>
             */
            remove(noSwitchExpressions);
            remove(onlyOneLabelInSwitchCase);
            remove(noYield);
        }
    }
}
