/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.smt

import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType
import java.math.BigInteger

/**
 * @author Alexander Weigl
 * @version 1 (07.08.22)
 */
class IntArithmeticTranslator(smtLog: SmtQuery) : BitVectorArithmeticTranslator((smtLog)) {
    private val term: SmtTermFactory = SmtTermFactory

    override fun makeChar(n: CharLiteralExpr): SExpr = term.makeInt("" + n.asChar().code)

    override fun makeLong(n: LongLiteralExpr): SExpr = term.makeInt("" + n.value)

    override fun makeInt(n: IntegerLiteralExpr): SExpr = term.makeInt("" + n.value)

    override fun makeInt(i: BigInteger): SExpr = term.makeInt(i.toString())

    override fun makeIntVar(): SExpr {
        val name = "__RAND_" + (++cnt)
        smtLog.declareConst(name, SmtType.BV32)
        return term.symbol(name)
    }

    override fun getPrimitiveType(rType: ResolvedPrimitiveType) = when (rType) {
        ResolvedPrimitiveType.FLOAT, ResolvedPrimitiveType.DOUBLE -> SmtType.REAL
        else -> SmtType.INT
    }

    override fun arrayLength(obj: SExpr): SExpr = term.list(ResolvedPrimitiveType.INT, SmtType.INT, term.symbol("int\$length"), obj)
}
