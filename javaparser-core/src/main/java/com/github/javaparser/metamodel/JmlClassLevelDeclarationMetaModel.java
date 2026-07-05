/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.metamodel;

import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.jml.body.JmlClassLevelDeclaration;
import java.util.Optional;

/**
 * This file, class, and its contents are completely generated based on:
 * <ul>
 *     <li>The contents and annotations within the package `com.github.javaparser.ast`, and</li>
 *     <li>`ALL_NODE_CLASSES` within the class `com.github.javaparser.generator.metamodel.MetaModelGenerator`.</li>
 * </ul>
 *
 * For this reason, any changes made directly to this file will be overwritten the next time generators are run.
 */
@Generated("com.github.javaparser.generator.metamodel.NodeMetaModelGenerator")
public class JmlClassLevelDeclarationMetaModel extends BodyDeclarationMetaModel {

    @Generated("com.github.javaparser.generator.metamodel.NodeMetaModelGenerator")
    JmlClassLevelDeclarationMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
                superBaseNodeMetaModel,
                JmlClassLevelDeclaration.class,
                "JmlClassLevelDeclaration",
                "com.github.javaparser.ast.jml.body",
                true,
                true);
    }

    @Generated("com.github.javaparser.generator.metamodel.NodeMetaModelGenerator")
    protected JmlClassLevelDeclarationMetaModel(
            Optional<BaseNodeMetaModel> superNodeMetaModel,
            Class<? extends Node> type,
            String name,
            String packageName,
            boolean isAbstract,
            boolean hasWildcard) {
        super(superNodeMetaModel, type, name, packageName, isAbstract, hasWildcard);
    }
}
