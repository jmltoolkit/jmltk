/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.jml.impl;

import com.github.javaparser.ast.Modifier;

import java.util.HashMap;
import java.util.Map;

public class JmlAnnotationConfiguration {

    public static final String NOT_NULL = "org.jetbrains.annotations.NotNull";

    public static final String NULLABLE = "org.jetbrains.annotations.Nullable";

    public static final String CONTRACT = "org.jetbrains.annotations.Contract";

    private final Map<String, Modifier.DefaultKeyword> annotationToModifier = new HashMap<>();

    public Map<String, Modifier.DefaultKeyword> getAnnotationToModifier() {
        return annotationToModifier;
    }

    public static JmlAnnotationConfiguration createDefault() {
        JmlAnnotationConfiguration c = new JmlAnnotationConfiguration();
        c.annotationToModifier.put(NOT_NULL, Modifier.DefaultKeyword.JML_NON_NULL);
        c.annotationToModifier.put(NULLABLE, Modifier.DefaultKeyword.JML_NULLABLE);
        c.annotationToModifier.put("javax.validation.constraints.NotNull", Modifier.DefaultKeyword.JML_NON_NULL);
        c.annotationToModifier.put("javax.validation.constraints.Null", Modifier.DefaultKeyword.JML_NULLABLE);
        return c;
    }
}
