/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.TokenRange
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.jml.clauses.JmlClause

enum class Language { JAVA, JML }

enum class SnippetKind { AUTO, CU, MEMBER, STMT, EXPR }

enum class ParsedKind {
    COMPILATION_UNIT,
    MEMBER,
    STATEMENT,
    EXPRESSION,
    JML_EXPRESSION,
    JML_CLAUSE,
    PLAIN,
}

data class ParsedSource(
    val source: String,
    val kind: ParsedKind,
    val nodes: List<Node>,
    val tokenRanges: List<TokenRange>,
    val stripWrapperNewlines: Boolean = false,
)

/** Detects and parses compilation units, members, statements, expressions, and JML snippets. */
class SnippetParser {
    private val configuration =
        ParserConfiguration().apply {
            setProcessJml(true)
            setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
        }

    fun parse(
        text: String,
        language: Language = Language.JAVA,
        kind: SnippetKind = SnippetKind.AUTO,
    ): ParsedSource =
        when (language) {
            Language.JAVA -> parseJava(text, kind)
            Language.JML -> parseJml(text)
        }

    private fun parseJava(
        text: String,
        kind: SnippetKind,
    ): ParsedSource {
        val kinds =
            if (kind == SnippetKind.AUTO) {
                listOf(SnippetKind.CU, SnippetKind.MEMBER, SnippetKind.STMT, SnippetKind.EXPR)
            } else {
                listOf(kind)
            }
        for (candidate in kinds) {
            val parsed =
                when (candidate) {
                    SnippetKind.CU -> parseCompilationUnit(text)
                    SnippetKind.MEMBER -> parseMember(text)
                    SnippetKind.STMT -> parseStatement(text)
                    SnippetKind.EXPR -> parseExpression(text)
                    SnippetKind.AUTO -> null
                }
            if (parsed != null) return parsed
        }
        return plain(text)
    }

    private fun parseCompilationUnit(text: String): ParsedSource? {
        val unit = parseCu(text) ?: return null
        // JavaParser wraps a single member into a synthetic class named $COMPACT_CLASS.
        if (unit.types.any { it.nameAsString == "\$COMPACT_CLASS" }) return null
        return ParsedSource(
            text,
            ParsedKind.COMPILATION_UNIT,
            listOf(unit),
            listOfNotNull(unit.tokenRange.orElse(null)),
        )
    }

    private fun parseMember(text: String): ParsedSource? {
        val wrapper = "class __JmltkSnippet {\n$text\n}"
        val unit = parseCu(wrapper) ?: return null
        val type = unit.getType(0)
        val members = type.members.toList()
        if (members.isEmpty()) return null
        val range = innerRange(type.tokenRange.orElse(null) ?: return null, "{", "}") ?: return null
        return ParsedSource(text, ParsedKind.MEMBER, members, listOf(range), stripWrapperNewlines = true)
    }

    private fun parseStatement(text: String): ParsedSource? {
        val wrapper = "class __JmltkSnippet { void __m() {\n$text\n} }"
        val unit = parseCu(wrapper) ?: return null
        val method = unit.getType(0).methods.firstOrNull { it.nameAsString == "__m" } ?: return null
        val body = method.body.orElse(null) ?: return null
        val statements = body.statements.toList()
        if (statements.isEmpty()) return null
        val range = innerRange(body.tokenRange.orElse(null) ?: return null, "{", "}") ?: return null
        return ParsedSource(
            text,
            ParsedKind.STATEMENT,
            statements,
            listOf(range),
            stripWrapperNewlines = true,
        )
    }

    private fun parseExpression(text: String): ParsedSource? {
        val wrapper = "class __JmltkSnippet { Object __f = (\n$text\n); }"
        val unit = parseCu(wrapper) ?: return null
        val field = unit.getType(0).getFieldByName("__f").orElse(null) ?: return null
        val expression = field.variables.firstOrNull()?.initializer?.orElse(null) ?: return null
        val range = innerRange(field.tokenRange.orElse(null) ?: return null, "(", ")") ?: return null
        return ParsedSource(
            text,
            ParsedKind.EXPRESSION,
            listOf(expression),
            listOf(range),
            stripWrapperNewlines = true,
        )
    }

    private fun parseJml(text: String): ParsedSource {
        parseJmlExpression(text)?.let { return it }

        val parts = splitJmlClauses(text)
        if (parts.size > 1) {
            val clauses = parts.mapNotNull { parseJmlClauseNode(it) }
            if (clauses.size == parts.size && clauses.isNotEmpty()) {
                return ParsedSource(
                    text,
                    ParsedKind.JML_CLAUSE,
                    clauses,
                    clauses.mapNotNull { it.tokenRange.orElse(null) },
                )
            }
        }

        parseJmlClauseNode(text)?.let { clause ->
            return ParsedSource(
                text,
                ParsedKind.JML_CLAUSE,
                listOf(clause),
                listOfNotNull(clause.tokenRange.orElse(null)),
            )
        }
        return plain(text)
    }

    private fun parseJmlExpression(text: String): ParsedSource? {
        val result = JavaParser(configuration).parseJmlExpression<Expression>(text)
        if (!result.isSuccessful) return null
        val expression = result.result.orElse(null) ?: return null
        return ParsedSource(
            text,
            ParsedKind.JML_EXPRESSION,
            listOf(expression),
            listOfNotNull(expression.tokenRange.orElse(null)),
        )
    }

    private fun parseJmlClauseNode(text: String): JmlClause? {
        val result = JavaParser(configuration).parseJmlClause(text)
        return if (result.isSuccessful) result.result.orElse(null) else null
    }

    private fun parseCu(text: String): CompilationUnit? {
        val result = JavaParser(configuration).parse(text)
        return if (result.isSuccessful) result.result.orElse(null) else null
    }

    private fun innerRange(
        range: TokenRange,
        open: String,
        close: String,
    ): TokenRange? {
        val tokens = range.iterator().asSequence().filter { it.text.isNotEmpty() }.toList()
        val first = tokens.indexOfFirst { it.text == open }
        val last = tokens.indexOfLast { it.text == close }
        if (first < 0 || last <= first + 1) return null
        return TokenRange(tokens[first + 1], tokens[last - 1])
    }

    private fun splitJmlClauses(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        var i = 0
        var inString = false
        var quote = ' '
        var inLineComment = false
        var inBlockComment = false
        while (i < text.length) {
            val c = text[i]
            when {
                inLineComment -> {
                    if (c == '\n') inLineComment = false
                    i++
                }

                inBlockComment -> {
                    if (c == '*' && i + 1 < text.length && text[i + 1] == '/') {
                        inBlockComment = false
                        i += 2
                    } else {
                        i++
                    }
                }

                inString -> {
                    if (c == '\\' && i + 1 < text.length) {
                        i += 2
                    } else {
                        if (c == quote) inString = false
                        i++
                    }
                }

                c == '"' || c == '\'' -> {
                    inString = true
                    quote = c
                    i++
                }

                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> {
                    inLineComment = true
                    i += 2
                }

                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> {
                    inBlockComment = true
                    i += 2
                }

                c == '(' || c == '[' || c == '{' -> {
                    depth++
                    i++
                }

                c == ')' || c == ']' || c == '}' -> {
                    depth--
                    i++
                }

                c == ';' && depth == 0 -> {
                    parts.add(text.substring(start, i + 1).trim())
                    start = i + 1
                    i++
                }

                else -> i++
            }
        }
        val rest = text.substring(start).trim()
        if (rest.isNotEmpty()) parts.add(rest)
        return parts.filter { it.isNotEmpty() }
    }

    private fun plain(text: String): ParsedSource =
        ParsedSource(text, ParsedKind.PLAIN, emptyList(), emptyList())
}
