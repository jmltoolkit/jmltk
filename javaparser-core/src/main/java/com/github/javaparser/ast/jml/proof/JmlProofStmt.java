/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.proof;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofStmtMetaModel;

/**
 * A single statement of a JML/KeY proof script, i.e. of the {@code \by} script attached to
 * an assertion. A script is a list of such statements; the concrete kinds are
 *
 * <ul>
 *   <li>{@link JmlProofCmdStmt} &mdash; a named proof command with optional arguments,
 *   <li>{@link JmlProofObtainStmt} &mdash; an {@code obtain} command, and
 *   <li>{@link JmlProofCaseStmt} &mdash; a {@code case}/{@code default} branch.
 * </ul>
 *
 * <p>For example:
 * <pre>{@code
 *     //@ assert f() \by { oss; obtain int x \from_goal; case "a": auto; };
 * }</pre>
 */
public abstract class JmlProofStmt extends Node {

    @AllFieldsConstructor
    public JmlProofStmt() {
        this(null);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofStmt(TokenRange tokenRange) {
        super(tokenRange);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofStmt clone() {
        return (JmlProofStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofStmtMetaModel;
    }
}
