/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.postprocessors;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Processor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VarType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Processes the generic AST into a Java 10 AST and validates it.
 */
public class Java10PostProcessor extends PostProcessors {

    // List of parent contexts in which a var type must not be detected.
    // for example: in this statement var.class.getCanonicalName(), var must not be considered as a VarType
    private static List<Class> FORBIDEN_PARENT_CONTEXT_TO_DETECT_POTENTIAL_VAR_TYPE = new ArrayList<>();

    static {
        FORBIDEN_PARENT_CONTEXT_TO_DETECT_POTENTIAL_VAR_TYPE.addAll(Arrays.asList(ClassExpr.class));
    }

    protected final Processor varNodeCreator = new Processor() {

        @Override
        public void postProcess(ParseResult<? extends Node> result, ParserConfiguration configuration) {
            result.getResult().ifPresent(node -> {
                node.findAll(ClassOrInterfaceType.class).forEach(n -> {
                    if ("var".equals(n.getNameAsString()) && !matchForbiddenContext(n)) {
                        n.replace(new VarType(n.getTokenRange().orElse(null)));
                    }
                });
            });
        }

        private boolean matchForbiddenContext(ClassOrInterfaceType cit) {
            return cit.getParentNode().isPresent()
                    && FORBIDEN_PARENT_CONTEXT_TO_DETECT_POTENTIAL_VAR_TYPE.stream()
                            .anyMatch(cl -> cl.isInstance(cit.getParentNode().get()));
        }
    };

    public Java10PostProcessor() {
        add(varNodeCreator);
    }
}
