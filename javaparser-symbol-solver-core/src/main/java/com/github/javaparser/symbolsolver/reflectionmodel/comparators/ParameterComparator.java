/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.reflectionmodel.comparators;

import java.lang.reflect.Parameter;
import java.util.Comparator;

/**
 * @author Federico Tomassetti
 */
public class ParameterComparator implements Comparator<Parameter> {

    @Override
    public int compare(Parameter o1, Parameter o2) {
        int compareName = o1.getName().compareTo(o2.getName());
        if (compareName != 0) return compareName;
        int compareType = new ClassComparator().compare(o1.getType(), o2.getType());
        if (compareType != 0) return compareType;
        return 0;
    }
}
