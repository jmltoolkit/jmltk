/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.visitor;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.SourceRoot;

/**
 * Generates JavaParser's VoidVisitorAdapter.
 */
public class GenericVisitorAdapterGenerator extends VisitorGenerator {
    public GenericVisitorAdapterGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.ast.visitor", "GenericVisitorAdapter", "R", "A", true);
    }

    @Override
    protected void generateVisitMethodBody(
            BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        visitMethod.getParameters().forEach(p -> p.setFinal(true));

        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();

        body.addStatement("R result;");

        final String resultCheck = "if (result != null) return result;";

        for (PropertyMetaModel field : node.getAllPropertyMetaModels()) {
            final String getter = field.getGetterMethodName() + "()";
            if (field.getNodeReference().isPresent()) {
                if (field.isOptional()) {
                    body.addStatement(f(
                            "if (n.%s.isPresent()) {" + "   result = n.%s.get().accept(this, arg);" + "   %s" + "}",
                            getter, getter, resultCheck));
                } else {
                    body.addStatement(f("{ result = n.%s.accept(this, arg); %s }", getter, resultCheck));
                }
            }
        }
        body.addStatement("return null;");
    }
}
