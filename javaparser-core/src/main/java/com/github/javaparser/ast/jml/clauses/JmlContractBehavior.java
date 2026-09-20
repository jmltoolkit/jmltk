/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.clauses;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.jml.JmlKeywordNode;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * @author Alexander Weigl
 * @version 1 (3/14/21)
 */
public class JmlContractBehavior extends JmlKeywordNode<JmlBehaviorKind> {

    @AllFieldsConstructor
    public JmlContractBehavior(JmlBehaviorKind kind) {
        this(null, kind);
    }

    public JmlContractBehavior(TokenRange range, JmlBehaviorKind kind) {
        super(range, kind);
    }

    public JmlContractBehavior(JavaToken token) {
        super(new TokenRange(token, token), JmlBehaviorKind.getByToken(token));
    }


    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
    }
}


