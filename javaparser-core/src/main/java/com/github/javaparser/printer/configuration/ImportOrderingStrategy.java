/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.configuration;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;

import java.util.List;

public interface ImportOrderingStrategy {

    /**
     * Sort the list of imports into groups.
     *
     * <p>
     * <p>
     * Consider that we have the following list of imports as argument:
     * <br>
     * <pre>
     * import java.util.List;
     * import com.github.javaparser.ast.NodeList;
     * import com.github.javaparser.ast.ImportDeclaration;
     * </pre>
     *
     * <p>
     * <p>
     * And we want the imports to look like this: (Note the spacing between imports)
     * <pre>
     * import java.util.List;
     *
     * import com.github.javaparser.ast.NodeList;
     * import com.github.javaparser.ast.ImportDeclaration;
     * </pre>
     *
     * <p>
     * <p>
     * In this case, we have two groups of imports. The first group contains only import for java.util.List,
     * while the second group contains NodeList and ImportDeclaration.
     * <p>
     * For this example this method should return 2 groups in the list, and the first group should have
     * exactly 1 import, while the second group should have 2 imports.
     *
     * @param imports The imports to be ordered.
     * @return The group of sorted imports.
     */
    List<NodeList<ImportDeclaration>> sortImports(NodeList<ImportDeclaration> imports);

    void setSortImportsAlphabetically(boolean sortAlphabetically);

    boolean isSortImportsAlphabetically();
}
