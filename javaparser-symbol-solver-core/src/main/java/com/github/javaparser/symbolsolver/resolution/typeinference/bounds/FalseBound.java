/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution.typeinference.bounds;

import com.github.javaparser.symbolsolver.resolution.typeinference.Bound;
import com.github.javaparser.symbolsolver.resolution.typeinference.InferenceVariable;
import com.github.javaparser.symbolsolver.resolution.typeinference.InferenceVariableSubstitution;

import java.util.Collections;
import java.util.Set;

/**
 * No valid choice of inference variables exists.
 *
 * @author Federico Tomassetti
 */
public class FalseBound extends Bound {

    private static FalseBound INSTANCE = new FalseBound();

    private FalseBound() {}

    public static FalseBound getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "FalseBound{}";
    }

    @Override
    public boolean isSatisfied(InferenceVariableSubstitution inferenceVariableSubstitution) {
        return false;
    }

    @Override
    public Set<InferenceVariable> usedInferenceVariables() {
        return Collections.emptySet();
    }
}
