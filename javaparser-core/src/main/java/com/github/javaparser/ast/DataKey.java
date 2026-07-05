/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

/**
 * A key to a piece of data associated with a {@link Node} at runtime.
 * The key contains type information that can be used to check the
 * type of any user data value for the key when the value is set. DataKey is abstract in order to
 * force the creation of a subtype. That subtype is used to test for identity when looking for the
 * user data because actual object identity would suffer from problems under serialization.
 * So, the correct way to declare a DataKey is like this:
 * <p>
 * <pre>
 * {@code
 * public static final DataKey<Role> ROLE = new DataKey<Role>() { };
 * }
 * </pre>
 * <p>
 * This code was taken from the <a href="http://wicket.apache.org/">Wicket project</a>.
 *
 * @param <T> The type of the object which is stored
 * @see Node#getData(DataKey)
 */
public abstract class DataKey<T> {

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }
}
