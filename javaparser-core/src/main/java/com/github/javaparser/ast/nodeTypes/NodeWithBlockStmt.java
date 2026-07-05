/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.jspecify.annotations.NullMarked;

/**
 * A node with a body that is a BlockStmt.
 */
@NullMarked
public interface NodeWithBlockStmt<N extends Node> {

    BlockStmt getBody();

    N setBody(BlockStmt block);

    default BlockStmt createBody() {
        BlockStmt block = new BlockStmt();
        setBody(block);
        return block;
    }
}
