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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates JavaParser's GenericListVisitorAdapter.
 */
public class GenericListVisitorAdapterGenerator extends VisitorGenerator {
    public GenericListVisitorAdapterGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.ast.visitor", "GenericListVisitorAdapter", "List<R>", "A", true);
    }

    @Override
    protected void generateVisitMethodBody(
            BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        visitMethod.getParameters().forEach(p -> p.setFinal(true));

        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();
        body.addStatement("List<R> result = new ArrayList<>();");
        body.addStatement("List<R> tmp;");

        final String resultCheck = "if (tmp != null) result.addAll(tmp);";

        for (PropertyMetaModel field : node.getAllPropertyMetaModels()) {
            final String getter = field.getGetterMethodName() + "()";
            if (field.getNodeReference().isPresent()) {
                if (field.isOptional()) {
                    body.addStatement(f(
                            "if (n.%s.isPresent()) {" + "   tmp = n.%s.get().accept(this, arg);" + "   %s" + "}",
                            getter, getter, resultCheck));
                } else {
                    body.addStatement(f("{ tmp = n.%s.accept(this, arg); %s }", getter, resultCheck));
                }
            }
        }
        body.addStatement("return result;");
        Arrays.stream(new Class<?>[] {List.class, ArrayList.class})
                .filter(c -> compilationUnit.getImports().stream()
                        .noneMatch(i -> c.getName().equals(i.getName().asString())))
                .forEach(compilationUnit::addImport);
    }
}
