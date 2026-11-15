/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.prettyprinting

/**
 * Pygments-compatible style classes. [cssClass] is the short class name Pygments emits for HTML
 * (for example `kd` for declaration keywords).
 */
enum class StyleClass(val cssClass: String) {
    TEXT(""),
    KEYWORD("k"),
    KEYWORD_DECLARATION("kd"),
    KEYWORD_TYPE("kt"),
    KEYWORD_CONSTANT("kc"),
    KEYWORD_NAMESPACE("kn"),
    NAME("n"),
    NAME_ATTRIBUTE("na"),
    NAME_BUILTIN("nb"),
    NAME_CLASS("nc"),
    NAME_DECORATOR("nd"),
    NAME_FUNCTION("nf"),
    NAME_LABEL("nl"),
    NAME_NAMESPACE("nn"),
    NUMBER("m"),
    NUMBER_BINARY("mb"),
    NUMBER_FLOAT("mf"),
    NUMBER_HEX("mh"),
    NUMBER_INTEGER("mi"),
    NUMBER_OCT("mo"),
    STRING("s"),
    STRING_CHAR("sc"),
    STRING_ESCAPE("se"),
    OPERATOR("o"),
    PUNCTUATION("p"),
    COMMENT("c"),
    COMMENT_SINGLE("c1"),
    COMMENT_MULTILINE("cm"),
    COMMENT_SPECIAL("cs"),
    ERROR("err"),
    ;

    /**
     * Macro suffix for the raw LaTeX renderer. TeX control words cannot contain digits, so the
     * digit-bearing Pygments classes are renamed.
     */
    val latexMacro: String
        get() =
            when (this) {
                COMMENT_SINGLE -> "cOne"
                else -> cssClass
            }
}

data class StyleAttributes(
    val rgb: Int?,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/** Colors and attributes of the Pygments `default` style. */
internal val pygmentsDefault: Map<StyleClass, StyleAttributes> =
    mapOf(
        StyleClass.KEYWORD to StyleAttributes(0x008000, bold = true),
        StyleClass.KEYWORD_DECLARATION to StyleAttributes(0x008000, bold = true),
        StyleClass.KEYWORD_CONSTANT to StyleAttributes(0x008000, bold = true),
        StyleClass.KEYWORD_NAMESPACE to StyleAttributes(0x008000, bold = true),
        StyleClass.KEYWORD_TYPE to StyleAttributes(0xB00040),
        StyleClass.NAME_ATTRIBUTE to StyleAttributes(0x687822),
        StyleClass.NAME_BUILTIN to StyleAttributes(0x008000),
        StyleClass.NAME_CLASS to StyleAttributes(0x0000FF, bold = true),
        StyleClass.NAME_DECORATOR to StyleAttributes(0xAA22FF),
        StyleClass.NAME_FUNCTION to StyleAttributes(0x0000FF),
        StyleClass.NAME_LABEL to StyleAttributes(0x767600),
        StyleClass.NAME_NAMESPACE to StyleAttributes(0x0000FF, bold = true),
        StyleClass.NUMBER to StyleAttributes(0x666666),
        StyleClass.NUMBER_BINARY to StyleAttributes(0x666666),
        StyleClass.NUMBER_FLOAT to StyleAttributes(0x666666),
        StyleClass.NUMBER_HEX to StyleAttributes(0x666666),
        StyleClass.NUMBER_INTEGER to StyleAttributes(0x666666),
        StyleClass.NUMBER_OCT to StyleAttributes(0x666666),
        StyleClass.STRING to StyleAttributes(0xBA2121),
        StyleClass.STRING_CHAR to StyleAttributes(0xBA2121),
        StyleClass.STRING_ESCAPE to StyleAttributes(0xAA5D1F, bold = true),
        StyleClass.OPERATOR to StyleAttributes(0x666666),
        StyleClass.COMMENT to StyleAttributes(0x3D7B7B, italic = true),
        StyleClass.COMMENT_SINGLE to StyleAttributes(0x3D7B7B, italic = true),
        StyleClass.COMMENT_MULTILINE to StyleAttributes(0x3D7B7B, italic = true),
        StyleClass.COMMENT_SPECIAL to StyleAttributes(0x3D7B7B, italic = true),
        StyleClass.ERROR to StyleAttributes(0xFF0000),
    )

private val xtermPalette: List<Int> by lazy {
    buildList {
        val system =
            listOf(
                0x000000,
                0x800000,
                0x008000,
                0x808000,
                0x000080,
                0x800080,
                0x008080,
                0xC0C0C0,
                0x808080,
                0xFF0000,
                0x00FF00,
                0xFFFF00,
                0x0000FF,
                0xFF00FF,
                0x00FFFF,
                0xFFFFFF,
            )
        addAll(system)
        val levels = listOf(0, 95, 135, 175, 215, 255)
        for (r in levels) {
            for (g in levels) {
                for (b in levels) {
                    add((r shl 16) or (g shl 8) or b)
                }
            }
        }
        for (i in 0 until 24) {
            val value = 8 + 10 * i
            add((value shl 16) or (value shl 8) or value)
        }
    }
}

/** Index of the closest xterm-256 color for a 24-bit RGB value. */
internal fun xterm256(rgb: Int): Int {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    var best = 0
    var bestDistance = Int.MAX_VALUE
    xtermPalette.forEachIndexed { index, candidate ->
        val cr = (candidate shr 16) and 0xFF
        val cg = (candidate shr 8) and 0xFF
        val cb = candidate and 0xFF
        val distance = (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return best
}
