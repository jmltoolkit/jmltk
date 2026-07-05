/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel.comparators;

import java.util.Comparator;

/**
 * @author Federico Tomassetti
 */
public class ClassComparator implements Comparator<Class<?>> {

    @Override
    public int compare(Class<?> o1, Class<?> o2) {
        int subCompare;
        subCompare = o1.getCanonicalName().compareTo(o2.getCanonicalName());
        if (subCompare != 0) return subCompare;
        subCompare = Boolean.compare(o1.isAnnotation(), o2.isAnnotation());
        if (subCompare != 0) return subCompare;
        subCompare = Boolean.compare(o1.isArray(), o2.isArray());
        if (subCompare != 0) return subCompare;
        subCompare = Boolean.compare(o1.isEnum(), o2.isEnum());
        if (subCompare != 0) return subCompare;
        subCompare = Boolean.compare(o1.isInterface(), o2.isInterface());
        if (subCompare != 0) return subCompare;
        return 0;
    }
}
