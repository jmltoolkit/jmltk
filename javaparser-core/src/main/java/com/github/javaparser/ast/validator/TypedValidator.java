/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Processor;
import com.github.javaparser.ast.Node;
import java.util.function.BiConsumer;

/**
 * A validator that validates a known node type.
 */
public interface TypedValidator<N extends Node> extends BiConsumer<N, ProblemReporter> {

    /**
     * @param node            the node that wants to be validated
     * @param problemReporter when found, validation errors can be reported here
     */
    void accept(N node, ProblemReporter problemReporter);

    @SuppressWarnings("unchecked")
    default Processor processor() {
        return new Processor() {

            @Override
            public void postProcess(ParseResult<? extends Node> result, ParserConfiguration configuration) {
                result.getResult()
                        .ifPresent(node -> accept((N) node, new ProblemReporter(problem -> result.getProblems()
                                .add(problem))));
            }
        };
    }
}
