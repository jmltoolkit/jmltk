/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.stat

/**
 * @author Alexander Weigl
 * @version 1 (02.06.22)
 */
data class ExpressionCosts(
    val sum: Int = 1,
    val minus: Int = 1,
    val divide: Int = 1,
    val mult: Int = 1,
    val mod: Int = 1,
    val land: Int = 1,
    val lor: Int = 1,
    val lnot: Int = 1,
    val band: Int = 1,
    val bor: Int = 2,
    val bnot: Int = 1,
    val quantor: Int = 3,
    val binderCostsPerVariable: Int = 1,
    val nullLiteral: Int = 0,
    val charLiteral: Int = 0,
    val stringLiteral: Int = 0,
    val integerLiteral: Int = 0,
    val longLiteral: Int = 0,
    val name: Int = 0,
    val methodCall: Int = 2,
    val conditional: Int = 2,
    val cast: Int = 1,
    val assign: Int = 1,
    val let: Int = 1,
    val compare: Int = 1,
    val instanceof: Int = 1
)
