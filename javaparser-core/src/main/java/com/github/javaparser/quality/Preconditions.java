/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.quality;

public final class Preconditions {

    private Preconditions() {
        // This constructor hide the public one.
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression.
     * @param message    the exception message to use if the check fails;
     *                   will be converted to a string using String.valueOf(Object)
     * @throws IllegalArgumentException if expression is false.
     */
    public static void checkArgument(boolean expression, Object message) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(message));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression.
     * @throws IllegalArgumentException if expression is false.
     */
    public static void checkArgument(boolean expression) {
        checkArgument(expression, "Invalid argument provided.");
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference.
     * @param message   the exception message to use if the check fails;
     *                  will be converted to a string using String.valueOf(Object)
     * @throws IllegalArgumentException if reference is {@code null}.
     */
    public static void checkNotNull(Object reference, Object message) {
        checkArgument(reference != null, message);
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference.
     * @throws IllegalArgumentException if reference is {@code null}.
     */
    public static void checkNotNull(Object reference) {
        checkNotNull(reference, "A null value is not allowed here.");
    }
}
