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
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.SourceRoot;

public class NoCommentEqualsVisitorGenerator extends VisitorGenerator {

    public NoCommentEqualsVisitorGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.ast.visitor", "NoCommentEqualsVisitor", "Boolean", "Visitable", true);
    }

    @Override
    protected void generateVisitMethodBody(
            BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        visitMethod.getParameters().forEach(p -> p.setFinal(true));

        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();

        if (!(node.equals(JavaParserMetaModel.lineCommentMetaModel)
                || node.equals(JavaParserMetaModel.blockCommentMetaModel)
                || node.equals(JavaParserMetaModel.traditionalJavadocCommentMetaModel))) {

            body.addStatement(f("final %s n2 = (%s) arg;", node.getTypeName(), node.getTypeName()));

            for (PropertyMetaModel field : node.getAllPropertyMetaModels()) {
                final String getter = field.getGetterMethodName() + "()";
                if (field.equals(JavaParserMetaModel.nodeMetaModel.commentPropertyMetaModel)) continue;
                if (field.getNodeReference().isPresent()) {
                    if (field.isNodeList()) {
                        body.addStatement(f("if (!nodesEquals(n.%s, n2.%s)) return false;", getter, getter));
                    } else {
                        body.addStatement(f("if (!nodeEquals(n.%s, n2.%s)) return false;", getter, getter));
                    }
                } else {
                    body.addStatement(f("if (!objEquals(n.%s, n2.%s)) return false;", getter, getter));
                }
            }
            if (body.getStatements().size() == 1) {
                // Only the cast line was added, but nothing is using it, so remove it again.
                body.getStatements().clear();
            }
        }
        body.addStatement("return true;");
    }
}
