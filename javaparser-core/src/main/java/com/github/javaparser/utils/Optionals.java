/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utility methods for {@link Optional}, backporting Java 9+ operations to Java 8.
 *
 * <p>This class emulates {@code Optional.or()} chaining from Java 9+, allowing
 * lazy evaluation of alternative Optional values.
 *
 * <p><b>Migration notice:</b> Remove this class when upgrading to Java 11+.
 * Replace {@code Optionals.or()} calls with native {@code Optional.or()} chaining.
 *
 * @since 3.28.0
 */
public final class Optionals {

    private Optionals() {
        throw new AssertionError("Optionals is a utility class and should not be instantiated");
    }

    /**
     * Returns the first present Optional from the given suppliers.
     *
     * <p>Emulates Java 9+ {@code Optional.or()} chaining with varargs support:
     * <pre>{@code
     * // Java 9+
     * Optional<T> result = opt1.or(() -> opt2).or(() -> opt3);
     *
     * // Java 8 with Optionals
     * Optional<T> result = Optionals.or(() -> opt1, () -> opt2, () -> opt3);
     * }</pre>
     *
     * <p>Suppliers are evaluated lazily with short-circuit evaluation.
     *
     * @param <T> the type of the value
     * @param suppliers suppliers of Optionals to evaluate in order
     * @return the first present Optional, or empty if all are empty
     */
    @SafeVarargs
    public static <T> Optional<T> or(Supplier<Optional<T>>... suppliers) {
        for (Supplier<Optional<T>> supplier : suppliers) {
            Optional<T> result = supplier.get();
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
