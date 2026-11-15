/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.javaparser.JavaToken

/** Maps JavaParser tokens to Pygments-compatible style spans. */
class Highlighter(private val jmlLexer: JmlLexer = JmlLexer()) {
    fun highlight(parsed: ParsedSource): List<Span> {
        if (parsed.kind == ParsedKind.PLAIN || parsed.tokenRanges.isEmpty()) {
            return listOf(Span(parsed.source))
        }
        if (parsed.kind == ParsedKind.JML_EXPRESSION || parsed.kind == ParsedKind.JML_CLAUSE) {
            return jmlLexer.lex(parsed.source)
        }

        val spans = mutableListOf<Span>()
        parsed.tokenRanges.forEachIndexed { index, range ->
            if (index > 0) spans.add(Span("\n"))
            val tokens = range.iterator().asSequence().filter { it.text.isNotEmpty() }.toList()
            tokens.forEachIndexed { tokenIndex, _ ->
                spans.addAll(tokenSpans(tokens, tokenIndex))
            }
        }
        return if (parsed.stripWrapperNewlines) stripWrapperNewlines(spans) else spans
    }

    private fun tokenSpans(
        tokens: List<JavaToken>,
        index: Int,
    ): List<Span> {
        val token = tokens[index]
        val text = token.text
        if (text.startsWith("/*@") || text.startsWith("//@")) {
            return jmlSpans(text)
        }
        return when (token.category) {
            JavaToken.Category.WHITESPACE_NO_EOL,
            JavaToken.Category.EOL,
            -> listOf(Span(text, StyleClass.TEXT))

            JavaToken.Category.COMMENT -> listOf(Span(text, commentClass(text)))

            JavaToken.Category.KEYWORD -> listOf(Span(text, keywordClass(text)))

            JavaToken.Category.IDENTIFIER -> listOf(Span(text, nameClass(tokens, index)))

            JavaToken.Category.LITERAL -> literalSpans(text)

            JavaToken.Category.SEPARATOR -> listOf(Span(text, separatorClass(text)))

            JavaToken.Category.OPERATOR -> listOf(Span(text, operatorClass(text)))
        }
    }

    private fun jmlSpans(text: String): List<Span> {
        val prefix = if (text.startsWith("/*@")) "/*@" else "//@"
        val prefixStyle = if (prefix == "/*@") StyleClass.COMMENT_MULTILINE else StyleClass.COMMENT_SINGLE
        val suffix =
            when {
                prefix == "/*@" && text.endsWith("@*/") -> "@*/"
                prefix == "/*@" && text.endsWith("*/") -> "*/"
                else -> ""
            }
        val innerEnd = text.length - suffix.length
        val inner = if (innerEnd >= prefix.length) text.substring(prefix.length, innerEnd) else ""
        return buildList {
            add(Span(prefix, prefixStyle))
            addAll(jmlLexer.lex(inner))
            if (suffix.isNotEmpty()) add(Span(suffix, prefixStyle))
        }
    }

    private fun literalSpans(text: String): List<Span> =
        when {
            text.startsWith("\"") -> stringSpans(text)
            text.startsWith("'") -> listOf(Span(text, StyleClass.STRING_CHAR))
            else -> listOf(Span(text, numberClass(text)))
        }

    private fun stringSpans(text: String): List<Span> {
        val result = mutableListOf<Span>()
        var runStart = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\') {
                if (i > runStart) result.add(Span(text.substring(runStart, i), StyleClass.STRING))
                var end = i + 1
                if (end < text.length && text[end] == 'u') {
                    end++
                    var digits = 0
                    while (end < text.length && digits < 4 && text[end].isHexDigit()) {
                        end++
                        digits++
                    }
                } else if (end < text.length) {
                    end++
                }
                result.add(Span(text.substring(i, end), StyleClass.STRING_ESCAPE))
                i = end
                runStart = i
                continue
            }
            i++
        }
        if (runStart < text.length) result.add(Span(text.substring(runStart), StyleClass.STRING))
        return result
    }

    private fun numberClass(text: String): StyleClass =
        when {
            text.startsWith("0x") || text.startsWith("0X") ->
                if (text.any { it == '.' || it == 'p' || it == 'P' }) {
                    StyleClass.NUMBER_FLOAT
                } else {
                    StyleClass.NUMBER_HEX
                }

            text.startsWith("0b") || text.startsWith("0B") -> StyleClass.NUMBER_BINARY

            OCTAL.matches(text) -> StyleClass.NUMBER_OCT

            text.any { it == '.' || it == 'e' || it == 'E' } ||
                text.endsWith('f') ||
                text.endsWith('F') ||
                text.endsWith('d') ||
                text.endsWith('D') -> StyleClass.NUMBER_FLOAT

            else -> StyleClass.NUMBER_INTEGER
        }

    private fun commentClass(text: String): StyleClass =
        when {
            text.startsWith("/**") -> StyleClass.COMMENT_SPECIAL
            text.startsWith("//") -> StyleClass.COMMENT_SINGLE
            else -> StyleClass.COMMENT_MULTILINE
        }

    private fun keywordClass(text: String): StyleClass =
        when (text) {
            in CONTROL_KEYWORDS -> StyleClass.KEYWORD
            in DECLARATION_KEYWORDS -> StyleClass.KEYWORD_DECLARATION
            in TYPE_KEYWORDS -> StyleClass.KEYWORD_TYPE
            in CONSTANT_KEYWORDS -> StyleClass.KEYWORD_CONSTANT
            in NAMESPACE_KEYWORDS -> StyleClass.KEYWORD_NAMESPACE
            else -> StyleClass.KEYWORD
        }

    private fun nameClass(
        tokens: List<JavaToken>,
        index: Int,
    ): StyleClass {
        val previous = previousSignificant(tokens, index)?.text
        val next = nextSignificant(tokens, index)?.text
        return when {
            previous in CLASS_KEYWORDS -> StyleClass.NAME_CLASS
            previous == "@" -> StyleClass.NAME_DECORATOR
            previous == "." -> StyleClass.NAME_ATTRIBUTE
            next == "(" -> StyleClass.NAME_FUNCTION
            next == ":" -> StyleClass.NAME_LABEL
            else -> StyleClass.NAME
        }
    }

    private fun separatorClass(text: String): StyleClass =
        when (text) {
            "[", "]" -> StyleClass.OPERATOR
            "@" -> StyleClass.NAME_DECORATOR
            else -> StyleClass.PUNCTUATION
        }

    private fun operatorClass(text: String): StyleClass =
        if (text == "@") StyleClass.NAME_DECORATOR else StyleClass.OPERATOR

    private fun previousSignificant(
        tokens: List<JavaToken>,
        index: Int,
    ): JavaToken? {
        for (i in index - 1 downTo 0) {
            val token = tokens[i]
            if (!token.category.isWhitespaceOrComment()) return token
        }
        return null
    }

    private fun nextSignificant(
        tokens: List<JavaToken>,
        index: Int,
    ): JavaToken? {
        for (i in index + 1 until tokens.size) {
            val token = tokens[i]
            if (!token.category.isWhitespaceOrComment()) return token
        }
        return null
    }

    private fun stripWrapperNewlines(spans: List<Span>): List<Span> {
        if (spans.isEmpty()) return spans
        val result = spans.toMutableList()
        val first = result.first()
        if (first.style == StyleClass.TEXT && first.text.startsWith("\n")) {
            result[0] = first.copy(text = first.text.substring(1))
        }
        val last = result.last()
        if (last.style == StyleClass.TEXT && last.text.endsWith("\n")) {
            result[result.lastIndex] = last.copy(text = last.text.dropLast(1))
        }
        return result.filter { it.text.isNotEmpty() }
    }

    private fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'

    private companion object {
        val OCTAL = Regex("0[0-7_]+[lL]?")
        val CONTROL_KEYWORDS =
            setOf(
                "assert",
                "break",
                "case",
                "catch",
                "continue",
                "default",
                "do",
                "else",
                "finally",
                "for",
                "goto",
                "if",
                "instanceof",
                "new",
                "return",
                "switch",
                "this",
                "throw",
                "try",
                "while",
                "yield",
            )
        val DECLARATION_KEYWORDS =
            setOf(
                "abstract",
                "const",
                "enum",
                "exports",
                "extends",
                "final",
                "implements",
                "native",
                "non-sealed",
                "open",
                "opens",
                "permits",
                "private",
                "protected",
                "provides",
                "public",
                "requires",
                "sealed",
                "static",
                "strictfp",
                "super",
                "synchronized",
                "throws",
                "to",
                "transient",
                "transitive",
                "uses",
                "volatile",
                "with",
                "module",
                "class",
                "interface",
                "record",
                "var",
            )
        val TYPE_KEYWORDS = setOf("boolean", "byte", "char", "double", "float", "int", "long", "short", "void")
        val CONSTANT_KEYWORDS = setOf("true", "false", "null")
        val NAMESPACE_KEYWORDS = setOf("package", "import")
        val CLASS_KEYWORDS = setOf("class", "interface", "enum", "record", "new")
    }
}
