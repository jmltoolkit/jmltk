/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.metamodel;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate a derived property of a Node,
 * meaning it does supply useful information,
 * but it does so by taking information from other properties.
 * (Used during generation of the meta model.)
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface DerivedProperty {}
