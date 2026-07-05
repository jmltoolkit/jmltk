/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel;

import com.github.javaparser.resolution.UnsolvedSymbolException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
 * This class allows exceptions to be handled either by casting particular exceptions
 * or by throwing new runtime exceptions.
 */
public class FailureHandler {

    private static final Map<Class<? extends Throwable>, Function<Throwable, ? extends RuntimeException>>
            FAILURE_CONVERTER = new HashMap<>();

    static {
        FAILURE_CONVERTER.put(UnsolvedSymbolException.class, (Throwable th) -> (RuntimeException) th);
    }

    public RuntimeException handle(Throwable th) {
        return handle(th, null);
    }

    public RuntimeException handle(Throwable th, String message) {
        // searching for exact mapping
        Function<Throwable, ? extends RuntimeException> converter =
                FAILURE_CONVERTER.get(findRootCause(th).getClass());
        if (converter != null) {
            return converter.apply(th);
        }
        // handle runtime exceptions
        if (RuntimeException.class.isAssignableFrom(th.getClass())) {
            return (RuntimeException) th;
        }
        return getRuntimeExceptionFrom(findRootCause(th), message);
    }

    protected final <E extends Throwable> E findRootCause(Throwable failure) {
        while (failure != null) {
            if (isRootCause(failure)) {
                return (E) failure;
            }
            failure = failure.getCause();
        }
        return null;
    }

    private boolean isRootCause(Throwable th) {
        return th.getCause() == null;
    }

    private RuntimeException getRuntimeExceptionFrom(Throwable th, String message) {
        if (message == null || message.isEmpty()) return new RuntimeException(findRootCause(th));
        return new RuntimeException(message, findRootCause(th));
    }
}
