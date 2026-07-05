/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.declarations;

import com.github.javaparser.ast.expr.Expression;

/**
 * @author Federico Tomassetti
 */
public interface ResolvedAnnotationMemberDeclaration extends ResolvedValueDeclaration {

    Expression getDefaultValue();
}
