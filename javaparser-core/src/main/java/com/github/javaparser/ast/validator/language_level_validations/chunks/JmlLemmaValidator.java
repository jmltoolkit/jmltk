/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.validator.language_level_validations.chunks;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
import com.github.javaparser.ast.jml.proof.JmlProofArg;
import com.github.javaparser.ast.jml.proof.JmlProofCmdStmt;
import com.github.javaparser.ast.validator.ProblemReporter;
import com.github.javaparser.ast.validator.VisitorValidator;

/**
 * Validates JML class-body {@code lemma} declarations and the {@code use_lemma}
 * proof command, e.g.
 *
 * <pre>{@code
 * //@ lemma nospace(\seq s) \by { use_lemma nospace(s); auto; };
 * }</pre>
 *
 * <p>Only the KeY-style proof-script form ({@code lemma Name(params) \by {…};})
 * exists; the {@code \by} script is mandatory. The checks are defensive: the
 * grammar only builds consistent trees, so a violation here means the tree was
 * assembled by hand or by an earlier version of the parser. All violations are
 * reported as {@link ProblemReporter} problems &mdash; never thrown.
 */
public class JmlLemmaValidator extends VisitorValidator {

    @Override
    public void visit(JmlLemmaDeclaration n, ProblemReporter reporter) {
        if (n.getScript().isEmpty()) {
            reporter.report(n, "A lemma requires a non-empty proof script after '\\by'.");
        }
        n.getParameters().forEach(p -> {
            if (p.isVarArgs()) {
                reporter.report(p, "Lemma parameters must be plain (no varargs).");
            }
        });
        super.visit(n, reporter);
    }

    @Override
    public void visit(JmlProofCmdStmt n, ProblemReporter reporter) {
        if (n.getName().getIdentifier().equals("use_lemma")) {
            for (JmlProofArg arg : n.getArgs()) {
                if (!(arg.getExpr() instanceof MethodCallExpr)) {
                    reporter.report(arg.getExpr(), "The operand of 'use_lemma' must be a method call expression.");
                }
            }
        }
        super.visit(n, reporter);
    }
}
