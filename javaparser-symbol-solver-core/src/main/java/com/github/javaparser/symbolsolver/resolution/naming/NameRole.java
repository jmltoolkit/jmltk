/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.naming;

/**
 * Each Name can be part either of a Declaration or a Reference to a Declaration.
 */
public enum NameRole {
    DECLARATION,
    REFERENCE
}
