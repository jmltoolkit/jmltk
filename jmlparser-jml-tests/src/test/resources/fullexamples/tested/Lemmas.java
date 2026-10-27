// Miniature of the VerifyThis 2025.1 MinExcludant0 example: JML class-body lemma
// declarations and KeY-style proof scripts (\by blocks with use_lemma).
package VerifyThis2025;

public class Lemmas {

    //@ instance lemma Pred(int i) = i > 0;

    //@ public static no_state lemma nospace(\seq s) \by { oss; use_lemma nospace(\array2seq(a)); auto; };

    //@ lemma intEq(int a, int b) = a == b;

    /*@
      @ ensures \result >= 0;
      @ assignable \nothing;
      @*/
    public static int minExcludant(int[] a, int n) {
        //@ assert n > 0 \by { auto; };
        //@ assert n >= 0 \by { use_lemma intEq(n, n); auto; };
        int i = 0;
        return i;
    }
}
