/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.clauses;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.jml.JmlKeywordNode;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

public class JmlClauseKind extends JmlKeywordNode<JmlClauseKind0> {
    public JmlClauseKind(JmlClauseKind0 kind) {
        this(null, kind);
    }

    public JmlClauseKind(TokenRange tokenRange, JmlClauseKind0 kind) {
        super(tokenRange, kind);
    }

    public JmlClauseKind(JavaToken kind) {
        this(new TokenRange(kind, kind), JmlClauseKind0.getKindByToken(kind));
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
    }
}