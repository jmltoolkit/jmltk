/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.ast.observer.AstObserverAdapter;
import com.github.javaparser.ast.type.UnknownType;
import java.util.IdentityHashMap;
import java.util.Map;
import static java.util.Collections.synchronizedMap;

/**
 * We want to recognize and ignore "phantom" nodes, like the fake type of variable in FieldDeclaration
 * @deprecated This class is no longer used phantom node are now an attribute of each node
 */
@Deprecated
public class PhantomNodeLogic {

    private static final int LEVELS_TO_EXPLORE = 3;

    private static final Map<Node, Boolean> isPhantomNodeCache = synchronizedMap(new IdentityHashMap<>());

    private static final AstObserver cacheCleaner = new AstObserverAdapter() {

        @Override
        public void parentChange(Node observedNode, Node previousParent, Node newParent) {
            isPhantomNodeCache.remove(observedNode);
        }
    };

    static boolean isPhantomNode(Node node) {
        if (isPhantomNodeCache.containsKey(node)) {
            return isPhantomNodeCache.get(node);
        }
        if (node instanceof UnknownType) {
            return true;
        }
        boolean res = (node.getParentNode().isPresent() && node.getParentNode().get().hasRange() && node.hasRange() && !node.getParentNode().get().getRange().get().contains(node.getRange().get()) || inPhantomNode(node, LEVELS_TO_EXPLORE));
        isPhantomNodeCache.put(node, res);
        node.register(cacheCleaner);
        return res;
    }

    /**
     * A node contained in a phantom node is also a phantom node. We limit how many levels up we check just for performance reasons.
     */
    private static boolean inPhantomNode(Node node, int levels) {
        return node.getParentNode().isPresent() && (isPhantomNode(node.getParentNode().get()) || inPhantomNode(node.getParentNode().get(), levels - 1));
    }

    /**
     * Clean up the cache used by the LexicalPreserving logic. This should only be used once you're done printing all parsed data with
     * a JavaParser's configuration setLexicalPreservationEnabled=true.
     */
    public static void cleanUpCache() {
        isPhantomNodeCache.clear();
    }
}
