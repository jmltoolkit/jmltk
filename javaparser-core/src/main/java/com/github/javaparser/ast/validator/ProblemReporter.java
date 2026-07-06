/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator;

import com.github.javaparser.Problem;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.nodeTypes.NodeWithTokenRange;
import com.github.javaparser.ast.validator.language_level_validations.UpgradeJavaMessage;
import java.util.function.Consumer;
import static com.github.javaparser.utils.CodeGenerationUtils.f;

/**
 * A simple interface where validators can report found problems.
 */
public class ProblemReporter {

    private final Consumer<Problem> problemConsumer;

    public ProblemReporter(Consumer<Problem> problemConsumer) {
        this.problemConsumer = problemConsumer;
    }

    /**
     * Report a problem.
     *
     * @param message description of the problem
     * @param node    the node in which the problem occurred, used to find the Range of the problem.
     */
    public void report(final NodeWithTokenRange<?> node, final UpgradeJavaMessage message, final Object... args) {
        this.report(node.getTokenRange().orElse(null), message.toString(), args);
    }

    /**
     * Report a problem.
     *
     * @param message description of the problem
     * @param node    the node in which the problem occurred, used to find the Range of the problem.
     */
    public void report(NodeWithTokenRange<?> node, String message, Object... args) {
        this.report(node.getTokenRange().orElse(null), message, args);
    }

    public void report(TokenRange range, String message, Object... args) {
        problemConsumer.accept(new Problem(f(message, args), range, null));
    }
}
