/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.body;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.jml.JmlKeywordNode;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * @author Alexander Weigl
 * @version 1 (15.08.26)
 */
public class JmlBodyClauseKind extends JmlKeywordNode<JmlBodyClauseKind0> {
    public JmlBodyClauseKind(JmlBodyClauseKind0 kind) {
        super(kind);
    }

    public JmlBodyClauseKind(TokenRange tokenRange, JmlBodyClauseKind0 kind) {
        super(tokenRange, kind);
    }

    public JmlBodyClauseKind(JavaToken begin) {
        super(new TokenRange(begin, begin), JmlBodyClauseKind0.getKindByToken(begin));
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {

    }
}