/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution;

/**
 * This exception is thrown when a symbol cannot be resolved.
 *
 * @author Federico Tomassetti
 */
public class UnsolvedSymbolException extends RuntimeException {

    /**
     * The name of the symbol that could not be resolved.
     */
    private String name;

    /**
     * Additional information that provides more details on the context that a symbol could not be resolved in, or
     * {@code null} if there is no contextual information, or if the contextual information is unknown.
     */
    private String context;

    /**
     * The throwable that caused this {@code UnsolvedSymbolException} to get thrown, or {@code null} if this
     * {@code UnsolvedSymbolException} was not caused by another throwable, or if the causative throwable is unknown.
     */
    private Throwable cause;

    public UnsolvedSymbolException(String name) {
        this(name, null, null);
    }

    public UnsolvedSymbolException(String name, String context) {
        this(name, context, null);
    }

    public UnsolvedSymbolException(String name, Throwable cause) {
        this(name, null, cause);
    }

    public UnsolvedSymbolException(String name, String context, Throwable cause) {
        super("Unsolved symbol" + (context != null ? " in " + context : "") + " : " + name, cause);
        this.name = name;
        this.context = context;
        this.cause = cause;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "UnsolvedSymbolException{" + "context='" + context + "'" + ", name='" + name + "'" + ", cause='" + cause + "'" + "}";
    }
}
