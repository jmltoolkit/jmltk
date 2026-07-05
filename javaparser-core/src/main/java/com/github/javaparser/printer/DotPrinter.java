/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import static com.github.javaparser.utils.Utils.assertNotNull;
import static java.util.stream.Collectors.toList;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.metamodel.NodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.LineSeparator;
import java.util.List;

/**
 * Outputs a Graphviz diagram of the AST.
 */
public class DotPrinter {

    private int nodeCount;

    private final boolean outputNodeType;

    public DotPrinter(boolean outputNodeType) {
        this.outputNodeType = outputNodeType;
    }

    public String output(Node node) {
        nodeCount = 0;
        StringBuilder output = new StringBuilder();
        output.append("digraph {");
        output(node, null, "root", output);
        output.append(LineSeparator.SYSTEM + "}");
        return output.toString();
    }

    public void output(Node node, String parentNodeName, String name, StringBuilder builder) {
        assertNotNull(node);
        NodeMetaModel metaModel = node.getMetaModel();
        List<PropertyMetaModel> allPropertyMetaModels = metaModel.getAllPropertyMetaModels();
        List<PropertyMetaModel> attributes = allPropertyMetaModels.stream()
                .filter(PropertyMetaModel::isAttribute)
                .filter(PropertyMetaModel::isSingular)
                .collect(toList());
        List<PropertyMetaModel> subNodes = allPropertyMetaModels.stream()
                .filter(PropertyMetaModel::isNode)
                .filter(PropertyMetaModel::isSingular)
                .collect(toList());
        List<PropertyMetaModel> subLists = allPropertyMetaModels.stream()
                .filter(PropertyMetaModel::isNodeList)
                .collect(toList());
        String ndName = nextNodeName();
        if (outputNodeType)
            builder.append(LineSeparator.SYSTEM + ndName + " [label=\"" + escape(name) + " (" + metaModel.getTypeName()
                    + ")\"];");
        else builder.append(LineSeparator.SYSTEM + ndName + " [label=\"" + escape(name) + "\"];");
        if (parentNodeName != null) builder.append(LineSeparator.SYSTEM + parentNodeName + " -> " + ndName + ";");
        for (PropertyMetaModel a : attributes) {
            String attrName = nextNodeName();
            builder.append(LineSeparator.SYSTEM + attrName + " [label=\"" + escape(a.getName()) + "='"
                    + escape(a.getValue(node).toString()) + "'\"];");
            builder.append(LineSeparator.SYSTEM + ndName + " -> " + attrName + ";");
        }
        for (PropertyMetaModel sn : subNodes) {
            Node nd = (Node) sn.getValue(node);
            if (nd != null) output(nd, ndName, sn.getName(), builder);
        }
        for (PropertyMetaModel sl : subLists) {
            NodeList<? extends Node> nl = (NodeList<? extends Node>) sl.getValue(node);
            if (nl != null && nl.isNonEmpty()) {
                String ndLstName = nextNodeName();
                builder.append(LineSeparator.SYSTEM + ndLstName + " [label=\"" + escape(sl.getName()) + "\"];");
                builder.append(LineSeparator.SYSTEM + ndName + " -> " + ndLstName + ";");
                String slName = sl.getName().substring(0, sl.getName().length() - 1);
                for (Node nd : nl) output(nd, ndLstName, slName, builder);
            }
        }
    }

    private String nextNodeName() {
        return "n" + (nodeCount++);
    }

    private static String escape(String value) {
        return value.replace("\"", "\\\"");
    }
}
