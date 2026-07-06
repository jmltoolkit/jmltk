/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.metamodel;

import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.jml.stmt.JmlExpressionStmt;

import java.util.Optional;

/**
 * This file, class, and its contents are completely generated based on:
 * <ul>
 *     <li>The contents and annotations within the package `com.github.javaparser.ast`, and</li>
 *     <li>`ALL_NODE_CLASSES` within the class `com.github.javaparser.generator.metamodel.MetaModelGenerator`.</li>
 * </ul>
 * <p>
 * For this reason, any changes made directly to this file will be overwritten the next time generators are run.
 */
@Generated("com.github.javaparser.generator.metamodel.NodeMetaModelGenerator")
public class JmlStmtWithExpressionMetaModel extends JmlStatementMetaModel {

    @Generated("com.github.javaparser.generator.metamodel.NodeMetaModelGenerator")
    JmlStmtWithExpressionMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
                superBaseNodeMetaModel,
                JmlExpressionStmt.class,
                "JmlStmtWithExpression",
                "com.github.javaparser.ast.jml.stmt",
                false,
                false);
    }

    public PropertyMetaModel expressionPropertyMetaModel;

    public PropertyMetaModel kindPropertyMetaModel;
}
