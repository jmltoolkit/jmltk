/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.lint.rules.locset

/*
 * # Abstract domain for locset (store-ref) expressions
 *
 * A locset denotes a set of memory locations (as used in `assignable`, `accessible`,
 * `modifiable` and `modifies` clauses). We approximate the concrete semantics
 * `P(Set<Location>)` by a reduced product of two independent three-level chains:
 *
 * ```
 *              Emptiness                     Universality
 *        (is the set empty?)            (does it contain \everything?)
 *
 *            DEF_NONEMPTY                        DEF_NOT_UNIV
 *               ^                                    ^
 *              MAYBE          x                  MAYBE
 *               ^                                    ^
 *            DEF_EMPTY                           DEF_UNIV
 * ```
 *
 * The information order is bottom-to-top: `DEF_EMPTY` (definitely `\\nothing`) is more
 * precise than `MAYBE`, which is more precise than `DEF_NONEMPTY` (at least one location).
 * Dually for universality, where `DEF_UNIV` means the locset contains `\\everything`.
 *
 * The concretization is
 * ```
 * γ(empty, univ) = { S ⊆ Location | empty ⇒ S = ∅,  univ ⇒ everything ∈ S }
 * ```
 * and the pair `(DEF_EMPTY, DEF_UNIV)` concretizes to the empty set family — it is
 * contradictory (`BOTTOM`), because `\\everything` is a location itself.
 *
 * @author Alexander Weigl
 * @version 1 (21.09.26)
 */

/**
 * Is the locset definitely empty, definitely non-empty, or unknown?
 * Information order: `DEF_EMPTY` ⊑ `MAYBE` ⊑ `DEF_NONEMPTY`.
 */
enum class Emptiness {
    /** Definitely the empty locset (`\nothing`). */
    DEF_EMPTY,

    /** No information. */
    MAYBE,

    /** Definitely contains at least one location. */
    DEF_NONEMPTY,
}

/**
 * Does the locset definitely contain `\everything`, definitely not, or unknown?
 * Information order: `DEF_UNIV` ⊑ `MAYBE` ⊑ `DEF_NOT_UNIV`.
 */
enum class Universality {
    /** Definitely contains `\everything`. */
    DEF_UNIV,

    /** No information. */
    MAYBE,

    /** Definitely does not contain `\everything`. */
    DEF_NOT_UNIV,
}

/**
 * One abstract locset value: the product of [Emptiness] and [Universality].
 *
 * The standard library values are:
 *  - [NOTHING] — `\\nothing` / `\\strictly_nothing`,
 *  - [EVERYTHING] — `\\everything`,
 *  - [SINGLE_LOCATION] — one concrete location (a field, an array cell, `this`),
 *  - [ALL_ARRAY_ELEMENTS] — `a[*]`, all elements of one array,
 *  - [ALL_OBJECT_FIELDS] — `o.*`, all fields of one object,
 *  - [TOP] — no information,
 *  - [BOTTOM] — contradictory, concretizes to the empty set family.
 */
data class AbsLoc(val empty: Emptiness, val univ: Universality) {

    /** The locset is provably empty: it cannot denote any location. */
    val alwaysEmpty: Boolean get() = empty == Emptiness.DEF_EMPTY

    /** The locset provably contains `\\everything`, i.e. the frame clause is vacuous. */
    val containsEverything: Boolean get() = univ == Universality.DEF_UNIV

    /** Contradictory abstract value (empty, yet contains everything). */
    val isBottom: Boolean get() = alwaysEmpty && containsEverything

    /**
     * Set union, the semantics of the comma-separated expression list of a frame
     * clause (`assignable e1, e2` = `e1 ∪ e2`):
     *  - the union is empty iff both parts are empty (`∅ ∪ S = S`),
     *  - the union contains everything iff either part does.
     */
    infix fun union(that: AbsLoc): AbsLoc {
        val e = when {
            this.empty == Emptiness.MAYBE || that.empty == Emptiness.MAYBE -> Emptiness.MAYBE
            this.empty == Emptiness.DEF_EMPTY -> that.empty
            that.empty == Emptiness.DEF_EMPTY -> this.empty
            else -> Emptiness.DEF_NONEMPTY
        }
        val u = when {
            this.univ == Universality.MAYBE || that.univ == Universality.MAYBE -> Universality.MAYBE

            this.univ == Universality.DEF_UNIV || that.univ == Universality.DEF_UNIV ->
                Universality.DEF_UNIV

            else -> Universality.DEF_NOT_UNIV
        }
        return AbsLoc(e, u)
    }

    /**
     * Set intersection: dual to [union].
     *  - empty if either part is empty,
     *  - contains everything iff both parts do (and is then non-empty).
     */
    infix fun intersect(that: AbsLoc): AbsLoc {
        val u = when {
            this.univ == Universality.MAYBE || that.univ == Universality.MAYBE -> Universality.MAYBE

            this.univ == Universality.DEF_NOT_UNIV || that.univ == Universality.DEF_NOT_UNIV ->
                Universality.DEF_NOT_UNIV

            else -> Universality.DEF_UNIV
        }
        val e = when {
            this.empty == Emptiness.DEF_EMPTY || that.empty == Emptiness.DEF_EMPTY ->
                Emptiness.DEF_EMPTY

            u == Universality.DEF_UNIV -> Emptiness.DEF_NONEMPTY

            // everything ∈ S ∩ T
            else -> Emptiness.MAYBE
        }
        return AbsLoc(e, u)
    }

    /**
     * Set difference `this \ that`. Sound approximation:
     *  - empty if this is empty,
     *  - empty if that contains everything and this is definitely finite,
     *  - unchanged if that is empty,
     *  - contains everything if this contains everything and that is empty.
     */
    infix fun minus(that: AbsLoc): AbsLoc {
        val e = when {
            this.empty == Emptiness.DEF_EMPTY -> Emptiness.DEF_EMPTY

            that.univ == Universality.DEF_UNIV && this.univ == Universality.DEF_NOT_UNIV ->
                Emptiness.DEF_EMPTY

            that.empty == Emptiness.DEF_EMPTY -> this.empty

            else -> Emptiness.MAYBE
        }
        val u = when {
            this.univ == Universality.DEF_UNIV && that.empty == Emptiness.DEF_EMPTY ->
                Universality.DEF_UNIV

            this.univ == Universality.DEF_NOT_UNIV -> Universality.DEF_NOT_UNIV

            else -> Universality.MAYBE
        }
        return AbsLoc(e, u)
    }

    override fun toString(): String = when {
        isBottom -> "BOTTOM"
        alwaysEmpty -> "NOTHING"
        containsEverything -> "EVERYTHING"
        this == TOP -> "TOP"
        this == SINGLE_LOCATION -> "SINGLE_LOCATION"
        this == ALL_ARRAY_ELEMENTS -> "ALL_ARRAY_ELEMENTS"
        this == ALL_OBJECT_FIELDS -> "ALL_OBJECT_FIELDS"
        else -> "($empty, $univ)"
    }

    companion object {
        /** `α(∅)` — the empty locset. */
        val NOTHING = AbsLoc(Emptiness.DEF_EMPTY, Universality.DEF_NOT_UNIV)

        /** `α({\\everything})` — the full locset. */
        val EVERYTHING = AbsLoc(Emptiness.DEF_NONEMPTY, Universality.DEF_UNIV)

        /** One concrete location: a field, an array cell, `this`, `\\result`. */
        val SINGLE_LOCATION = AbsLoc(Emptiness.DEF_NONEMPTY, Universality.DEF_NOT_UNIV)

        /** `a[*]`: all elements of a single array. A zero-length array yields no locations. */
        val ALL_ARRAY_ELEMENTS = AbsLoc(Emptiness.MAYBE, Universality.DEF_NOT_UNIV)

        /** `o.*`: all fields of a single object; `o` may be null. */
        val ALL_OBJECT_FIELDS = AbsLoc(Emptiness.MAYBE, Universality.DEF_NOT_UNIV)

        /** No information. */
        val TOP = AbsLoc(Emptiness.MAYBE, Universality.MAYBE)

        /** Contradiction: empty, yet contains everything. */
        val BOTTOM = AbsLoc(Emptiness.DEF_EMPTY, Universality.DEF_UNIV)
    }
}
