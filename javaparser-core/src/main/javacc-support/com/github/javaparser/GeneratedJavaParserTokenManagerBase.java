/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser;

import static com.github.javaparser.GeneratedJavaParserConstants.*;

import com.github.javaparser.ast.comments.*;
import java.util.ArrayDeque;

/**
 * Base class for {@link com.github.javaparser.GeneratedJavaParserTokenManager}
 */
abstract class GeneratedJavaParserTokenManagerBase {
    /**
     * Create a TokenRange that spans exactly one token
     */
    private static TokenRange tokenRange(Token token) {
        JavaToken javaToken = token.javaToken;
        return new TokenRange(javaToken, javaToken);
    }

    static boolean isMarkdownCommentLineCandidate(Token token) {
        return token.kind == SINGLE_LINE_COMMENT && token.image.startsWith("///");
    }

    static MarkdownComment createMarkdownCommentFromTokenList(ArrayDeque<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Cannot create markdown comment from empty token list");
        }

        // After the last comment token, only one EOL whitespace token should be included. Everything after that
        // should be filtered out.
        while (!tokens.isEmpty() && TokenTypes.isWhitespace(tokens.peekLast().kind)) {
            Token lastToken = tokens.removeLast();

            if (TokenTypes.isComment(lastToken.kind)) {
                tokens.addLast(lastToken);
                break;
            } else {
                if (tokens.isEmpty()) {
                    throw new IllegalArgumentException(
                            "createMarkdownCommentFromTokenList may not be called with a token list consisting only of whitespace tokens");
                }

                if (TokenTypes.isEndOfLineToken(lastToken.kind)) {
                    if (TokenTypes.isComment(tokens.peekLast().kind)) {
                        break;
                    }
                }
            }
        }

        TokenRange range = new TokenRange(tokens.peekFirst().javaToken, tokens.peekLast().javaToken);

        StringBuilder contentBuilder = new StringBuilder();

        for (Token token : tokens) {
            contentBuilder.append(token.image);
        }

        return new MarkdownComment(range, contentBuilder.toString());
    }

    /**
     * Since comments are completely captured in a single token, including their delimiters, deconstruct them here so we
     * can turn them into nodes later on.
     */
    static Comment createCommentFromToken(Token token) {
        String commentText = token.image;
        if (token.kind == JAVADOC_COMMENT) {
            return new TraditionalJavadocComment(tokenRange(token), commentText.substring(3, commentText.length() - 2));
        } else if (token.kind == MULTI_LINE_COMMENT) {
            return new BlockComment(tokenRange(token), commentText.substring(2, commentText.length() - 2));
        } else if (token.kind == SINGLE_LINE_COMMENT) {
            return new LineComment(tokenRange(token), commentText.substring(2));
        }
        throw new AssertionError("Unexpectedly got passed a non-comment token.");
    }
}
