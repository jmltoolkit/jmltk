/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.metamodel.NodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import java.util.List;
import static com.github.javaparser.utils.Utils.assertNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Outputs a YAML file containing the AST meant for inspecting it.
 */
public class YamlPrinter {

    private static final int NUM_SPACES_FOR_INDENT = 4;

    private final boolean outputNodeType;

    public YamlPrinter(boolean outputNodeType) {
        this.outputNodeType = outputNodeType;
    }

    public String output(Node node) {
        StringBuilder output = new StringBuilder();
        output.append("---");
        output(node, "root", 0, output);
        output.append(System.lineSeparator() + "...");
        return output.toString();
    }

    public void output(Node node, String name, int level, StringBuilder builder) {
        assertNotNull(node);
        NodeMetaModel metaModel = node.getMetaModel();
        List<PropertyMetaModel> allPropertyMetaModels = metaModel.getAllPropertyMetaModels();
        List<PropertyMetaModel> attributes = allPropertyMetaModels.stream().filter(PropertyMetaModel::isAttribute).filter(PropertyMetaModel::isSingular).collect(toList());
        List<PropertyMetaModel> subNodes = allPropertyMetaModels.stream().filter(PropertyMetaModel::isNode).filter(PropertyMetaModel::isSingular).collect(toList());
        List<PropertyMetaModel> subLists = allPropertyMetaModels.stream().filter(PropertyMetaModel::isNodeList).collect(toList());
        if (outputNodeType)
            builder.append(System.lineSeparator() + indent(level) + name + "(Type=" + metaModel.getTypeName() + "): ");
        else
            builder.append(System.lineSeparator() + indent(level) + name + ": ");
        level++;
        for (PropertyMetaModel a : attributes) {
            builder.append(System.lineSeparator() + indent(level) + a.getName() + ": " + escapeValue(a.getValue(node).toString()));
        }
        for (PropertyMetaModel sn : subNodes) {
            Node nd = (Node) sn.getValue(node);
            if (nd != null)
                output(nd, sn.getName(), level, builder);
        }
        for (PropertyMetaModel sl : subLists) {
            NodeList<? extends Node> nl = (NodeList<? extends Node>) sl.getValue(node);
            if (nl != null && nl.isNonEmpty()) {
                builder.append(System.lineSeparator() + indent(level) + sl.getName() + ": ");
                String slName = sl.getName();
                slName = slName.endsWith("s") ? slName.substring(0, sl.getName().length() - 1) : slName;
                for (Node nd : nl) output(nd, "- " + slName, level + 1, builder);
            }
        }
    }

    private String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) for (int j = 0; j < NUM_SPACES_FOR_INDENT; j++) sb.append(" ");
        return sb.toString();
    }

    private String escapeValue(String value) {
        return "\"" + value.replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\b", "\\b").replace("\t", "\\t") + "\"";
    }

    public static void print(Node node) {
        System.out.println(new YamlPrinter(true).output(node));
    }
}
