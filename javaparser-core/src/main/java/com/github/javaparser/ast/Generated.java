/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Copied from javax.annotation.Generated and reduced a bit.
 * <p>
 * Indicates a part of the code that was generated,
 * and will be overwritten the next time the generators are run.
 */
@Retention(SOURCE)
@Target({PACKAGE, TYPE, ANNOTATION_TYPE, METHOD, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, PARAMETER})
public @interface Generated {

    /**
     * The value element must have the name of the code generator.
     * The recommended convention is to use the fully qualified name of the
     * code generator. For example: {@code com.acme.generator.CodeGen}.
     */
    String[] value();
}
