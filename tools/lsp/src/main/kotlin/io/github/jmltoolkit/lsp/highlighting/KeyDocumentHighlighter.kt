/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lsp.highlighting

import de.uka.ilkd.key.nparser.JavaKeYLexer
import de.uka.ilkd.key.nparser.ParsingFacade
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Token
import org.eclipse.lsp4j.SemanticTokens

class KeyDocumentHighlighter : DocumentHighlighter {
    override fun analyzeJmlToken(text: String): SemanticTokens {
        val lexer = ParsingFacade.createLexer(CharStreams.fromString(text))
        val tb = SemanticTokensBuilder()
        do {
            val token = lexer.nextToken()
            if (token.type == JavaKeYLexer.EOF) {
                break
            }
            tokenType(token)?.let { tt ->
                tb.add(token.line, token.charPositionInLine, token.text.length, tt, tokenModifier(token))
            }
        } while (token.type == JavaKeYLexer.EOF)
        return SemanticTokens(tb.data)
    }

    private fun tokenType(token: Token): Int? = when (token.type) {
            JavaKeYLexer.COMMENT -> SupportedTokenTypes.COMMENT.ordinal

            JavaKeYLexer.VARIABLE -> SupportedTokenTypes.VARIABLE.ordinal

            JavaKeYLexer.VARCOND,
            JavaKeYLexer.IF,
            JavaKeYLexer.IFEX,
            JavaKeYLexer.RULES,
            JavaKeYLexer.AXIOMS,
            JavaKeYLexer.ABSTRACT,
            JavaKeYLexer.ASSIGN,
            JavaKeYLexer.ASSUMES,
            JavaKeYLexer.ADD,
            JavaKeYLexer.FIND,
            JavaKeYLexer.FINAL,
            JavaKeYLexer.ANTECEDENTPOLARITY,
            JavaKeYLexer.SUCCEDENTPOLARITY,
            JavaKeYLexer.UPDATE,
            JavaKeYLexer.UNIQUE,
            JavaKeYLexer.FUNCTIONS,
            JavaKeYLexer.PREDICATES,
            JavaKeYLexer.SORTS,
            JavaKeYLexer.HASSORT,
            JavaKeYLexer.HAS_INVARIANT,
            JavaKeYLexer.AT,
            JavaKeYLexer.THEN,
            JavaKeYLexer.TERM,
            JavaKeYLexer.DIFFERENT,
            JavaKeYLexer.CONTRACTS,
            JavaKeYLexer.CONTAINERTYPE,
            JavaKeYLexer.ENUM_CONST,
            JavaKeYLexer.IS_LABELED,
            JavaKeYLexer.IS_ABSTRACT_OR_INTERFACE,
            JavaKeYLexer.ISCONSTANT,
            JavaKeYLexer.ONEOF,
            JavaKeYLexer.OPTIONSDECL,
            JavaKeYLexer.WITHOPTIONS,
            JavaKeYLexer.MODALITYB,
            JavaKeYLexer.MORE,
            JavaKeYLexer.MODALITY,
            JavaKeYLexer.MODIFIABLE,
            JavaKeYLexer.APPLY_UPDATE_ON_RIGID,
            JavaKeYLexer.ADDRULES,
            -> SupportedTokenTypes.KEYWORD.ordinal

            JavaKeYLexer.BIN_LITERAL,
            JavaKeYLexer.HEX_LITERAL,
            JavaKeYLexer.INT_LITERAL,
            JavaKeYLexer.CHAR_LITERAL,
            JavaKeYLexer.REAL_LITERAL,
            JavaKeYLexer.DOUBLE_LITERAL,
            JavaKeYLexer.FLOAT_LITERAL,
            JavaKeYLexer.STRING_LITERAL,
            JavaKeYLexer.QUOTED_STRING_LITERAL,
            -> SupportedTokenTypes.NUMBER.ordinal

            else -> null
        }

    private fun tokenModifier(token: Token): Int = when (token.type) {
        else -> 0
    }
}
