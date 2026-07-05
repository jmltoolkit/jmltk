/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.generator.core.visitor;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.SeparatedItemStringBuilder;
import com.github.javaparser.utils.SourceRoot;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModifierVisitorGenerator extends VisitorGenerator {
    public ModifierVisitorGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.ast.visitor", "ModifierVisitor", "Visitable", "A", true);
    }

    @Override
    protected void generateVisitMethodBody(
            BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        visitMethod.getParameters().forEach(p -> p.setFinal(true));

        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();

        // FIXME: Bit of a hacky way to get this fixed order, and then have everything else (note this list is reversed)
        List<String> order = Arrays.asList(
                //                "comment", "name", "members", "parameters", "name",
                "modifiers", "annotations");
        List<PropertyMetaModel> sortedPropertyMetaModels = node.getAllPropertyMetaModels().stream()
                .sorted(
                        Comparator.comparingInt((PropertyMetaModel o) -> order.indexOf(o.getName()))
                                .reversed()
                        //                        .thenComparing(PropertyMetaModel::getName)
                        )
                .collect(Collectors.toList());

        //
        sortedPropertyMetaModels.forEach(property -> extracted(body, property));

        //
        if (node.is(BinaryExpr.class)) {
            body.addStatement("if (left == null) return right;");
            body.addStatement("if (right == null) return left;");
        } else {
            final SeparatedItemStringBuilder collapseCheck =
                    new SeparatedItemStringBuilder("if(", "||", ") return null;");
            sortedPropertyMetaModels.forEach(property -> {
                if (property.isRequired() && property.isNode()) {
                    if (property.isNodeList()) {
                        if (property.isNonEmpty()) {
                            collapseCheck.append(f("%s.isEmpty()", property.getName()));
                        }
                    } else {
                        collapseCheck.append(f("%s==null", property.getName()));
                    }
                }
            });
            if (collapseCheck.hasItems()) {
                body.addStatement(collapseCheck.toString());
            }
        }

        //
        sortedPropertyMetaModels.forEach(property -> {
            if (property.isNode()) {
                body.addStatement(f("n.%s(%s);", property.getSetterMethodName(), property.getName()));
            }
        });

        //
        body.addStatement("return n;");
    }

    private void extracted(BlockStmt body, PropertyMetaModel property) {
        if (property.isNode()) {
            if (property.isNodeList()) {
                body.addStatement(f(
                        "NodeList<%s> %s = modifyList(n.%s(), arg);",
                        property.getTypeNameGenerified(), property.getName(), property.getGetterMethodName()));
            } else if (property.isOptional()) {
                body.addStatement(f(
                        "%s %s = n.%s().map(s -> (%s) s.accept(this, arg)).orElse(null);",
                        property.getTypeNameGenerified(),
                        property.getName(),
                        property.getGetterMethodName(),
                        property.getTypeNameGenerified()));
            } else {
                body.addStatement(f(
                        "%s %s = (%s) n.%s().accept(this, arg);",
                        property.getTypeNameGenerified(),
                        property.getName(),
                        property.getTypeNameGenerified(),
                        property.getGetterMethodName()));
            }
        }
    }
}
