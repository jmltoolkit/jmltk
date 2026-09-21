package io.github.jmltoolkit.smt

import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedType
import io.github.jmltoolkit.smt.SmtTermFactory.not
import io.github.jmltoolkit.smt.model.SExpr
import io.github.jmltoolkit.smt.model.SmtType

/**
 * Formalization of Java reference values (objects) in SMT-LIB.
 *
 * Java objects are modeled as an uninterpreted sort `Object` together with:
 *  - a distinguished constant `null : Object`,
 *  - an uninterpreted predicate `instanceof : Object String -> Bool`, mapping
 *    an object to its runtime class relationship, and
 *  - the axiom `forall s:String. not (instanceof null s)`, i.e. `null`
 *    satisfies no instanceof test (JLS 15.20.2).
 *
 * Field accesses are modeled as uninterpreted functions (see
 * [SmtTermFactory.fieldAccess]), array accesses with the theory of arrays
 * (see [SmtTermFactory.select]). With this encoding, a null-pointer
 * dereference corresponds to `(= expr null)` and well-definedness
 * conditions can demand `not (= expr null)`.
 *
 * @author Alexander Weigl
 * @version 1 (20.09.26)
 */
object SmtObjectModel {
    const val SORT_OBJECT = "Object"
    const val CONST_NULL = "null"
    const val FN_INSTANCEOF = "instanceof"
    const val SORT_STRING = "String"

    /** Name of the `length` function used for arrays, cf. [ArithmeticTranslator.arrayLength]. */
    const val FN_LENGTH = "length"

    /**
     * Declares the object formalization in the given query. It is idempotent,
     * i.e. multiple invocations do not re-declare the symbols.
     */
    fun declare(query: SmtQuery) {
        val t = SmtTermFactory
        if (query.toString().contains("(declare-sort $SORT_OBJECT 0)")) {
            return
        }

        // (declare-sort Object 0)
        query.addCommand("declare-sort", t.symbol(SORT_OBJECT), t.intValue(0))

        // (declare-const null Object)
        query.declareConst(CONST_NULL, SmtType.JAVA_OBJECT)

        // (declare-fun instanceof (Object String) Bool)
        query.addCommand(
            "declare-fun",
            t.symbol(FN_INSTANCEOF),
            t.list(null, SmtType.TYPE, SORT_OBJECT, SORT_STRING),
            t.boolType()
        )

        // (assert (forall ((s String)) (not (instanceof null s))))
        val s = t.binder(SmtType.STRING, "s")
        query.addAssert(
            t.forall(listOf(s), !(t.instanceOf(t.makeNull(), "s")))
        )
    }

    /**
     * Well-definedness contribution of a dereference, i.e. the receiver of a
     * field access or method invocation must not be `null`.
     */
    fun noNullDereference(receiver: SExpr): SExpr = SmtTermFactory.nonNull(receiver)
}

/**
 * Helper for Java's boxing and unboxing conversions (JLS 5.1.7 / 5.1.8).
 * Unboxing a `null` reference causes a NullPointerException, hence the
 * well-definedness check has to demand non-null whenever a boxed value is
 * used in a primitive context, cf. [SmtObjectModel].
 */
object Boxing {
    private val BOXED_TYPES: Map<String, ResolvedPrimitiveType> = mapOf(
        "java.lang.Byte" to ResolvedPrimitiveType.BYTE,
        "java.lang.Short" to ResolvedPrimitiveType.SHORT,
        "java.lang.Integer" to ResolvedPrimitiveType.INT,
        "java.lang.Long" to ResolvedPrimitiveType.LONG,
        "java.lang.Character" to ResolvedPrimitiveType.CHAR,
        "java.lang.Float" to ResolvedPrimitiveType.FLOAT,
        "java.lang.Double" to ResolvedPrimitiveType.DOUBLE,
        "java.lang.Boolean" to ResolvedPrimitiveType.BOOLEAN
    )

    /**
     * Returns the primitive type if the given type is one of the boxed
     * primitive types, i.e. unboxing is necessary.
     */
    fun primitiveOf(type: ResolvedType): ResolvedPrimitiveType? {
        if (!type.isReferenceType) return null
        return try {
            BOXED_TYPES[type.asReferenceType().describe()]
        } catch (e: Throwable) {
            null
        }
    }
}
