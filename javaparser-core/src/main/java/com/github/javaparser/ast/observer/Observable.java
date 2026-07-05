/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.observer;

/**
 * Observable element.
 */
public interface Observable {

    /**
     * Register an observer.
     */
    void register(AstObserver observer);

    /**
     * Unregister an observer. If the given observer was not registered there are no effects.
     */
    void unregister(AstObserver observer);

    /**
     * Was this observer registered?
     * Note that equals is used to determine if the given observer was registered.
     */
    boolean isRegistered(AstObserver observer);
}
