/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
public class Predicates {
    //@ instance lemma Pred(int i) = i > 0;

    //@ model lemma Even(int n) = (n % 2 == 0);

    //@ static lemma PositiveSum(int a, int b) = a > 0 && b > a;

    public boolean check(int i) {
        return Pred(i);
    }
}
