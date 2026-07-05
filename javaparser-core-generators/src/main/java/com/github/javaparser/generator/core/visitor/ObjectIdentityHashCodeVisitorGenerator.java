/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.utils.SourceRoot;

/**
 * Generates JavaParser's ObjectIdentityHashCodeVisitor.
 */
public class ObjectIdentityHashCodeVisitorGenerator extends VisitorGenerator {
    public ObjectIdentityHashCodeVisitorGenerator(SourceRoot sourceRoot) {
        super(
                sourceRoot,
                "com.github.javaparser.ast.visitor",
                "ObjectIdentityHashCodeVisitor",
                "Integer",
                "Void",
                true);
    }

    @Override
    protected void generateVisitMethodBody(
            BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        visitMethod.getParameters().forEach(p -> p.setFinal(true));

        final BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();
        body.addStatement("return n.hashCode();");
    }
}
