/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

public interface NodeWithBody<N extends Node> {

    Statement getBody();

    N setBody(final Statement body);

    default BlockStmt createBlockStatementAsBody() {
        BlockStmt b = new BlockStmt();
        setBody(b);
        return b;
    }

    /**
     * @return true if the body is an {@link EmptyStmt} or an empty {@link BlockStmt}
     */
    default boolean hasEmptyBody() {
        Statement body = getBody();
        return body.toBlockStmt().map(bs -> bs.isEmpty()).orElse(body.isEmptyStmt());
    }
}
