/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.body;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.jml.JmlKeyword;

/**
 * @author Alexander Weigl
 * @version 1 (15.08.26)
 */
public enum JmlBodyClauseKind implements JmlKeyword {
    CONSTRAINT(GeneratedJavaParserConstants.CONSTRAINT),
    CONSTRAINT_REDUNDANTLY(GeneratedJavaParserConstants.CONSTRAINT_REDUNDANTLY),
    AXIOM(GeneratedJavaParserConstants.AXIOM),
    INITIALLY(GeneratedJavaParserConstants.INITIALLY),
    INVARIANT_FREE(GeneratedJavaParserConstants.INVARIANT_FREE),
    INVARIANT(GeneratedJavaParserConstants.INVARIANT),
    INVARIANT_REDUNDANTLY(GeneratedJavaParserConstants.INVARIANT_REDUNDANTLY);

    public final String jmlSymbol;

    private final int tokenType;

    JmlBodyClauseKind(int tokenType) {
        this.tokenType = tokenType;
        jmlSymbol = name().toLowerCase();
    }

    JmlBodyClauseKind(String jmlSymbol, int tokenType) {
        this.jmlSymbol = jmlSymbol;
        this.tokenType = tokenType;
    }

    @Override
    public String jmlSymbol() {
        return jmlSymbol;
    }

    public int getTokenType() {
        return tokenType;
    }

    public static JmlBodyClauseKind getKindByToken(JavaToken token) {
        for (JmlBodyClauseKind it : JmlBodyClauseKind.values()) {
            if (it.jmlSymbol.equals(token.getText())) {
                return it;
            }
        }
        throw new IllegalArgumentException("Could not find clause kind for: " + token.getText());
    }
}
