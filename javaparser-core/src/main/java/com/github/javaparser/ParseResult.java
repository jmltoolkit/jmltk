/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.utils.LineSeparator;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The results given when parsing with an instance of JavaParser.
 */
public class ParseResult<T> {

    private final T result;

    private final List<Problem> problems;

    private final CommentsCollection commentsCollection;

    /**
     * Optional source path associated with this parse operation. This may be set by higher-level utilities
     * (e.g., {@code SourceRoot}) to allow callers to correlate parse problems with the originating file path
     * even when no {@code CompilationUnit} is produced.
     * <p>
     * Contract:
     * - If present, it's expected to be an absolute {@link Path} to the source input used for this parse.
     * - This value should be set immediately after parsing is performed and treated as effectively immutable thereafter.
     */
    private Path sourcePath;

    /**
     * General constructor.
     *
     * @param result the AST, or empty if it wasn't created.
     * @param problems a list of encountered parsing problems.
     */
    public ParseResult(T result, List<Problem> problems, CommentsCollection commentsCollection) {
        this.commentsCollection = commentsCollection;
        this.result = result;
        this.problems = problems;
    }

    /**
     * @return if parsing was successful, meaning no errors of any kind were encountered.
     */
    public boolean isSuccessful() {
        return problems.isEmpty() && result != null;
    }

    /**
     * Calls the consumer with the result if parsing was succesful.
     */
    public void ifSuccessful(Consumer<T> consumer) {
        if (isSuccessful()) {
            consumer.accept(result);
        }
    }

    /**
     * Associates a source path with this parse result. Returns {@code this} for chaining.
     * <p>
     * Notes:
     * - Intended for use by infrastructure (e.g., {@code SourceRoot}) to publish the originating file path.
     * - Should be called immediately after parsing and not modified afterwards to avoid surprises in concurrent contexts.
     * - The provided path is expected to be absolute.
     */
    public ParseResult<T> setSourcePath(Path sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    /**
     * @return the list of encountered parsing problems. Empty when no problems were encountered.
     */
    public List<Problem> getProblems() {
        return problems;
    }

    /**
     * @return the {@code i}'th encountered parsing problem. May throw <code>IndexOutOfBoundsException</code>.
     */
    public Problem getProblem(int i) {
        return getProblems().get(i);
    }

    /**
     * @return the complete collection of comments encountered while parsing.
     */
    public Optional<CommentsCollection> getCommentsCollection() {
        return Optional.ofNullable(commentsCollection);
    }

    /**
     * @return the AST of the parsed source code, or empty if parsing failed completely.
     */
    public Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * @return the absolute path to the source file this parse result originates from, or empty if no file parsing.
     */
    public Optional<Path> getSourcePath() {
        return Optional.ofNullable(this.sourcePath);
    }

    @Override
    public String toString() {
        if (isSuccessful()) {
            return "Parsing successful";
        }
        StringBuilder message = new StringBuilder("Parsing failed");
        if (sourcePath != null) {
            message.append(" for ").append(sourcePath);
        }
        message.append(":").append(LineSeparator.SYSTEM);
        for (Problem problem : problems) {
            message.append(problem.toString()).append(LineSeparator.SYSTEM);
        }
        return message.toString();
    }
}
