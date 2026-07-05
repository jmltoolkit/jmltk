/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

import java.util.Objects;

/**
 * Simply a pair of objects.
 *
 * @param <A> type of object a.
 * @param <B> type of object b.
 */
public class Pair<A, B> {

    public final A a;

    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        if (!Objects.equals(a, pair.a)) return false;
        if (!Objects.equals(b, pair.b)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        return 31 * result + (b != null ? b.hashCode() : 0);
    }

    @Override
    public String toString() {
        return f("<%s, %s>", a, b);
    }
}
