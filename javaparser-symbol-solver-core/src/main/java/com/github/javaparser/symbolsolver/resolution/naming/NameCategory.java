/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.naming;

/**
 * Context causes a name syntactically to fall into one of seven categories: ModuleName, PackageName, TypeName,
 * ExpressionName, MethodName, PackageOrTypeName, or AmbiguousName.
 * TypeName is less expressive than the other six categories, because it is denoted with TypeIdentifier, which excludes
 * the character sequence var (§3.8).
 * <p>
 * See JLS 6.5 (https://docs.oracle.com/javase/specs/jls/se10/html/jls-6.html#jls-6.5)
 */
public enum NameCategory {
    MODULE_NAME(false),
    PACKAGE_NAME(false),
    TYPE_NAME(false),
    EXPRESSION_NAME(false),
    METHOD_NAME(false),
    PACKAGE_OR_TYPE_NAME(true),
    AMBIGUOUS_NAME(true),
    COMPILATION_ERROR(false);

    private boolean needDisambiguation;

    NameCategory(boolean needDisambiguation) {
        this.needDisambiguation = needDisambiguation;
    }

    /**
     * Certain category include two or more unambiguous categories.
     * These ambiguous categories are recognized solely through a syntactic process. In order to disambiguate them
     * a semantic process (i.e., consider the symbols which are actually visible in a given context) is needed.
     */
    public boolean isNeedingDisambiguation() {
        return needDisambiguation;
    }

    /**
     * Is the given name acceptable for the given category?
     */
    public boolean isNameAcceptable(String name) {
        return this != TYPE_NAME || !name.equals("var");
    }

    public boolean isValid() {
        return this != COMPILATION_ERROR;
    }
}
