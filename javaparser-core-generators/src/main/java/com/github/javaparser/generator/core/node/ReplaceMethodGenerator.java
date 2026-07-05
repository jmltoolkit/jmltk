/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.node;

import static com.github.javaparser.StaticJavaParser.parseBodyDeclaration;
import static com.github.javaparser.utils.CodeGenerationUtils.f;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.NodeGenerator;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.SourceRoot;

public class ReplaceMethodGenerator extends NodeGenerator {
    public ReplaceMethodGenerator(SourceRoot sourceRoot) {
        super(sourceRoot);
    }

    @Override
    protected void generateNode(
            BaseNodeMetaModel nodeMetaModel, CompilationUnit nodeCu, ClassOrInterfaceDeclaration nodeCoid) {
        MethodDeclaration replaceNodeMethod =
                (MethodDeclaration) parseBodyDeclaration("public boolean replace(Node node, Node replacementNode) {}");
        nodeCu.addImport(Node.class);
        annotateWhenOverridden(nodeMetaModel, replaceNodeMethod);

        final BlockStmt body = replaceNodeMethod.getBody().get();

        body.addStatement("if (node == null) { return false; }");

        int numberPropertiesDeclared = 0;
        for (PropertyMetaModel property : nodeMetaModel.getDeclaredPropertyMetaModels()) {
            if (!property.isNode()) {
                continue;
            }
            String check;
            if (property.isNodeList()) {
                check = nodeListCheck(property);
            } else {
                check = attributeCheck(property, property.getSetterMethodName());
            }
            if (property.isOptional()) {
                check = f("if (%s != null) { %s }", property.getName(), check);
            }
            body.addStatement(check);
            numberPropertiesDeclared++;
        }
        if (nodeMetaModel.getSuperNodeMetaModel().isPresent()) {
            body.addStatement("return super.replace(node, replacementNode);");
        } else {
            body.addStatement("return false;");
        }

        if (!nodeMetaModel.isRootNode() && numberPropertiesDeclared == 0) {
            removeMethodWithSameSignature(nodeCoid, replaceNodeMethod);
        } else {
            addOrReplaceWhenSameSignature(nodeCoid, replaceNodeMethod);
        }
    }

    private String attributeCheck(PropertyMetaModel property, String attributeSetterName) {
        return f(
                "if (node == %s) {" + "    %s((%s) replacementNode);" + "    return true;\n" + "}",
                property.getName(), attributeSetterName, property.getTypeName());
    }

    private String nodeListCheck(PropertyMetaModel property) {
        return f(
                "for (int i = 0; i < %s.size(); i++) {" + "  if (%s.get(i) == node) {"
                        + "    %s.set(i, (%s) replacementNode);"
                        + "    return true;"
                        + "  }"
                        + "}",
                property.getName(), property.getName(), property.getName(), property.getTypeName());
    }
}
