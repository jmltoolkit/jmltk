/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Iterate over all the nodes in (a part of) the AST. In contrast to the visit methods in Node, these methods are
 * implemented in a simple recursive way which should be more efficient. A disadvantage is that they cannot be quit in
 * the middle of their traversal.
 */
public abstract class TreeVisitor {

    public void visitLeavesFirst(Node node) {
        for (Node child : node.getChildNodes()) {
            visitLeavesFirst(child);
        }
        process(node);
    }

    /**
     * Performs a pre-order node traversal starting with a given node. When each node is visited, {@link #process(Node)}
     * is called for further processing.
     *
     * @param node The node at which the traversal begins.
     * @see <a href="https://en.wikipedia.org/wiki/Depth-first_search#Vertex_orderings">Pre-order traversal</a>
     */
    public void visitPreOrder(Node node) {
        process(node);
        new ArrayList<>(node.getChildNodes()).forEach(this::visitPreOrder);
    }

    /**
     * Performs a post-order node traversal starting with a given node. When each node is visited, {@link
     * #process(Node)} is called for further processing.
     *
     * @param node The node at which the traversal begins.
     * @see <a href="https://en.wikipedia.org/wiki/Depth-first_search#Vertex_orderings">Post-order traversal</a>
     */
    public void visitPostOrder(Node node) {
        new ArrayList<>(node.getChildNodes()).forEach(this::visitPostOrder);
        process(node);
    }

    /**
     * https://en.wikipedia.org/wiki/Breadth-first_search
     *
     * @param node the start node, and the first one that is passed to process(node).
     */
    public void visitBreadthFirst(Node node) {
        final Queue<Node> queue = new LinkedList<>();
        queue.offer(node);
        while (queue.size() > 0) {
            final Node head = queue.peek();
            for (Node child : head.getChildNodes()) {
                queue.offer(child);
            }
            process(queue.poll());
        }
    }

    /**
     * Process the given node.
     *
     * @param node The current node to process.
     */
    public abstract void process(Node node);

    /**
     * Performs a simple traversal over all nodes that have the passed node as their parent.
     */
    public void visitDirectChildren(Node node) {
        new ArrayList<>(node.getChildNodes()).forEach(this::process);
    }
}
