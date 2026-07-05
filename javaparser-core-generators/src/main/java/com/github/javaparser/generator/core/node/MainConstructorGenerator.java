/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.node;

import static com.github.javaparser.StaticJavaParser.parseExplicitConstructorInvocationStmt;
import static com.github.javaparser.utils.CodeGenerationUtils.f;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.NodeGenerator;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.SeparatedItemStringBuilder;
import com.github.javaparser.utils.SourceRoot;

public class MainConstructorGenerator extends NodeGenerator {
    public MainConstructorGenerator(SourceRoot sourceRoot) {
        super(sourceRoot);
    }

    @Override
    protected void generateNode(
            BaseNodeMetaModel nodeMetaModel, CompilationUnit nodeCu, ClassOrInterfaceDeclaration nodeCoid) {
        if (nodeMetaModel.is(Node.class)) {
            return;
        }
        ConstructorDeclaration constructor = new ConstructorDeclaration()
                .setPublic(true)
                .setName(nodeCoid.getNameAsString())
                .addParameter(TokenRange.class, "tokenRange")
                .setJavadocComment("\n     * This constructor is used by the parser and is considered private.\n     ");

        BlockStmt body = constructor.getBody().get(); // only non-existing in KeY world.

        SeparatedItemStringBuilder superCall = new SeparatedItemStringBuilder("super(", ", ", ");");
        superCall.append("tokenRange");
        for (PropertyMetaModel parameter : nodeMetaModel.getConstructorParameters()) {
            constructor.addParameter(parameter.getTypeNameForSetter(), parameter.getName());
            if (nodeMetaModel.getDeclaredPropertyMetaModels().contains(parameter)) {
                body.addStatement(f("%s(%s);", parameter.getSetterMethodName(), parameter.getName()));
            } else {
                superCall.append(parameter.getName());
            }
        }

        body.getStatements().addNFirst(parseExplicitConstructorInvocationStmt(superCall.toString()));

        body.addStatement("customInitialization();");

        addOrReplaceWhenSameSignature(nodeCoid, constructor);
        nodeCu.addImport(TokenRange.class);
    }
}
