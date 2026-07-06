/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.jml.clauses.JmlContract;
import com.github.javaparser.jml.JmlUtility;

import java.util.List;

/**
 * @author Alexander Weigl
 * @version 1 (12/9/21)
 */
public interface NodeWithContracts<N extends Node> {

    NodeList<JmlContract> getContracts();

    N setContracts(NodeList<JmlContract> contracts);

    default void addContract(JmlContract contracts) {
        final var jmlContracts = getContracts();
        jmlContracts.add(contracts);
        if (jmlContracts.size() == 1) JmlUtility.fixRangeContracts(this);
    }

    default void addContracts(JmlContract... contracts) {
        final var jmlContracts = getContracts();
        jmlContracts.addAll(List.of(contracts));
        if (jmlContracts.size() == 1) JmlUtility.fixRangeContracts(this);
    }
}
