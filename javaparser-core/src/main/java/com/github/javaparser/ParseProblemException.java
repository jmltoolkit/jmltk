/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.utils.LineSeparator;

import java.util.List;

import static com.github.javaparser.utils.Utils.assertNotNull;
import static java.util.Collections.singletonList;

/**
 * Thrown when parsing problems occur during parsing with the static methods on JavaParser.
 */
public class ParseProblemException extends RuntimeException {

    /**
     * The problems that were encountered during parsing
     */
    private final List<Problem> problems;

    public ParseProblemException(List<Problem> problems) {
        super(createMessage(assertNotNull(problems)));
        this.problems = problems;
    }

    public ParseProblemException(Throwable throwable) {
        this(singletonList(new Problem(throwable.getMessage(), null, throwable)));
    }

    private static String createMessage(List<Problem> problems) {
        StringBuilder message = new StringBuilder();
        for (Problem problem : problems) {
            message.append(problem.toString()).append(LineSeparator.SYSTEM);
        }
        return message.toString();
    }

    public List<Problem> getProblems() {
        return problems;
    }
}
