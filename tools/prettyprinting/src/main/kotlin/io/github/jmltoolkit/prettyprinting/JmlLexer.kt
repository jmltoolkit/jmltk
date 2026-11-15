/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/** Lightweight lexer for JML expressions and clause bodies. */
class JmlLexer {
    fun lex(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> {
                    val start = i
                    while (i < text.length && text[i].isWhitespace()) i++
                    spans.add(Span(text.substring(start, i), StyleClass.TEXT))
                }

                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> {
                    val start = i
                    i += 2
                    while (i < text.length && text[i] != '\n') i++
                    spans.add(Span(text.substring(start, i), StyleClass.COMMENT_SINGLE))
                }

                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> {
                    val start = i
                    i += 2
                    while (i + 1 < text.length && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i = if (i + 1 < text.length) i + 2 else text.length
                    spans.add(Span(text.substring(start, i), StyleClass.COMMENT_MULTILINE))
                }

                c == '"' -> i = lexString(text, i, '"', StyleClass.STRING, spans)

                c == '\'' -> i = lexString(text, i, '\'', StyleClass.STRING_CHAR, spans)

                c.isDigit() -> {
                    val (end, style) = scanNumber(text, i)
                    spans.add(Span(text.substring(i, end), style))
                    i = end
                }

                isIdentifierStart(c) -> {
                    val start = i
                    if (c == '\\') {
                        i++
                        while (i < text.length && isIdentifierPart(text[i])) i++
                        spans.add(Span(text.substring(start, i), StyleClass.KEYWORD))
                    } else {
                        i++
                        while (i < text.length && isIdentifierPart(text[i])) i++
                        spans.add(Span(text.substring(start, i), classifyWord(text.substring(start, i))))
                    }
                }

                else -> {
                    val operator = operators.firstOrNull { text.startsWith(it, i) }
                    if (operator != null) {
                        spans.add(Span(operator, StyleClass.OPERATOR))
                        i += operator.length
                    } else if (c in punctuation) {
                        spans.add(Span(c.toString(), StyleClass.PUNCTUATION))
                        i++
                    } else if (c == '[' || c == ']') {
                        spans.add(Span(c.toString(), StyleClass.OPERATOR))
                        i++
                    } else {
                        spans.add(Span(c.toString(), StyleClass.TEXT))
                        i++
                    }
                }
            }
        }
        return spans
    }

    private fun lexString(
        text: String,
        start: Int,
        quote: Char,
        style: StyleClass,
        spans: MutableList<Span>,
    ): Int {
        var i = start + 1
        var runStart = start
        while (i < text.length) {
            val c = text[i]
            if (c == '\\') {
                if (i > runStart) spans.add(Span(text.substring(runStart, i), style))
                var end = i + 1
                if (end < text.length && text[end] == 'u') {
                    end++
                    var digits = 0
                    while (end < text.length && digits < 4 && isHexDigit(text[end])) {
                        end++
                        digits++
                    }
                } else if (end < text.length) {
                    end++
                }
                spans.add(Span(text.substring(i, end), StyleClass.STRING_ESCAPE))
                i = end
                runStart = i
                continue
            }
            if (c == quote) {
                i++
                break
            }
            i++
        }
        if (i > runStart) spans.add(Span(text.substring(runStart, i), style))
        return i
    }

    private fun scanNumber(text: String, start: Int): Pair<Int, StyleClass> {
        var i = start
        if (i + 1 < text.length && text[i] == '0' && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
            i += 2
            while (i < text.length && (isHexDigit(text[i]) || text[i] == '_')) i++
            var isFloat = false
            if (i < text.length && text[i] == '.') {
                isFloat = true
                i++
                while (i < text.length && (isHexDigit(text[i]) || text[i] == '_')) i++
            }
            if (i < text.length && (text[i] == 'p' || text[i] == 'P')) {
                isFloat = true
                i++
                if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
                while (i < text.length && (text[i].isDigit() || text[i] == '_')) i++
            }
            if (i < text.length && text[i] in "lLfFdD") i++
            return i to if (isFloat) StyleClass.NUMBER_FLOAT else StyleClass.NUMBER_HEX
        }

        if (i + 1 < text.length && text[i] == '0' && (text[i + 1] == 'b' || text[i + 1] == 'B')) {
            i += 2
            while (i < text.length && (text[i] == '0' || text[i] == '1' || text[i] == '_')) i++
            if (i < text.length && (text[i] == 'l' || text[i] == 'L')) i++
            return i to StyleClass.NUMBER_BINARY
        }

        while (i < text.length && (text[i].isDigit() || text[i] == '_')) i++
        var isFloat = false
        if (i < text.length && text[i] == '.' && i + 1 < text.length && text[i + 1].isDigit()) {
            isFloat = true
            i++
            while (i < text.length && (text[i].isDigit() || text[i] == '_')) i++
        }
        if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
            isFloat = true
            i++
            if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
            while (i < text.length && (text[i].isDigit() || text[i] == '_')) i++
        }
        if (i < text.length && text[i] in "fFdD") {
            isFloat = true
            i++
        }
        if (isFloat) return i to StyleClass.NUMBER_FLOAT

        val suffix = if (i < text.length && (text[i] == 'l' || text[i] == 'L')) {
            i++
            true
        } else {
            false
        }
        val digits = text.substring(start, if (suffix) i - 1 else i)
        val isOctal =
            digits.length > 1 &&
                digits[0] == '0' &&
                digits.drop(1).all { it in '0'..'7' || it == '_' }
        return i to if (isOctal) StyleClass.NUMBER_OCT else StyleClass.NUMBER_INTEGER
    }

    private fun classifyWord(word: String): StyleClass =
        when (word) {
            "true", "false", "null" -> StyleClass.KEYWORD_CONSTANT
            in clauseKeywords -> StyleClass.KEYWORD_DECLARATION
            in logicalKeywords -> StyleClass.KEYWORD
            in typeKeywords -> StyleClass.KEYWORD_TYPE
            else -> StyleClass.NAME
        }

    private fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '$' || c == '\\'

    private fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

    private fun isHexDigit(c: Char): Boolean = c.isDigit() || c in 'a'..'f' || c in 'A'..'F'

    private companion object {
        val operators =
            listOf(
                "<==>",
                "==>",
                "<:",
                "->",
                "..",
                "==",
                "!=",
                "<=",
                ">=",
                "&&",
                "||",
                "+=",
                "-=",
                "*=",
                "/=",
                "%=",
                "+",
                "-",
                "*",
                "/",
                "%",
                "!",
                "?",
                ":",
                "=",
                "<",
                ">",
                ".",
                "|",
                "&",
                "^",
                "~",
            )
        val punctuation = setOf('{', '}', '(', ')', ';', ',', '@')
        val typeKeywords = setOf("boolean", "byte", "char", "double", "float", "int", "long", "short", "void")
        val logicalKeywords =
            setOf(
                "forall",
                "exists",
                "old",
                "result",
                "fresh",
                "nothing",
                "everything",
                "sum",
                "product",
                "max",
                "min",
                "num_of",
                "such_that",
                "reach",
                "typeof",
                "type",
                "elemtype",
                "nonnullelements",
                "strictly_nothing",
            )
        val clauseKeywords =
            setOf(
                "requires",
                "ensures",
                "assignable",
                "modifies",
                "accessible",
                "measured_by",
                "captures",
                "drains",
                "duration",
                "working_space",
                "when",
                "signals",
                "diverges",
                "decreases",
                "continues",
                "breaks",
                "returns",
                "also",
                "behavior",
                "normal_behavior",
                "exceptional_behavior",
                "private_behavior",
                "public_behavior",
                "invariant",
                "constraint",
                "represents",
                "initially",
                "loop_invariant",
                "maintaining",
                "loop_decreases",
                "loop_when",
                "assert",
                "assume",
                "set",
                "model",
                "ghost",
                "helper",
                "pure",
                "spec_public",
                "spec_protected",
            )
    }
}
