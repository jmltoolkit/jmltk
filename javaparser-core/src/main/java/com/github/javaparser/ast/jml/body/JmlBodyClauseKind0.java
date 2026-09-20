package com.github.javaparser.ast.jml.body;

import com.github.javaparser.GeneratedJavaParserConstants;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.jml.JmlKeyword;

/**
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
public enum JmlBodyClauseKind0 implements JmlKeyword {
    CONSTRAINT(GeneratedJavaParserConstants.CONSTRAINT),
    CONSTRAINT_REDUNDANTLY(GeneratedJavaParserConstants.CONSTRAINT_REDUNDANTLY),
    AXIOM(GeneratedJavaParserConstants.AXIOM),
    INITIALLY(GeneratedJavaParserConstants.INITIALLY),
    INVARIANT_FREE(GeneratedJavaParserConstants.INVARIANT_FREE),
    INVARIANT(GeneratedJavaParserConstants.INVARIANT),
    INVARIANT_REDUNDANTLY(GeneratedJavaParserConstants.INVARIANT_REDUNDANTLY);

    public final String jmlSymbol;

    private final int tokenType;

    JmlBodyClauseKind0(int tokenType) {
        this.tokenType = tokenType;
        jmlSymbol = name().toLowerCase();
    }

    JmlBodyClauseKind0(String jmlSymbol, int tokenType) {
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

    public static JmlBodyClauseKind0 getKindByToken(JavaToken token) {
        for (JmlBodyClauseKind0 it : JmlBodyClauseKind0.values()) {
            if (it.jmlSymbol.equals(token.getText())) {
                return it;
            }
        }
        throw new IllegalArgumentException("Could not find clause kind for: " + token.getText());
    }
}
