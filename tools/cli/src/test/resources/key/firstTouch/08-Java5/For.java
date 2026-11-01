// KeY firstTouch 08-Java5 (key.ui/examples/firstTouch), VCG-adapted: the
// for-each sum whose contract uses the JML `\sum` generalized quantifier (and
// the KeY-specific `\index` loop variable). The VCG models only
// `\forall`/`\exists` binders, so `\sum` must be *rejected* loudly instead of
// being silently encoded as a universal quantifier. The `implements Iterable`
// /`Trivial` machinery of the example is dropped.
final class KeyFor {

    int[] a;

    //@ ensures \result == (\sum int j; 0 <= j && j < a.length; a[j]);
    int sum() {
        int s = 0;
        int z = a.length;

        /*@ maintaining s == (\sum int j; 0 <= j && j < \index; a[j]);
          @ maintaining 0 <= \index && \index <= a.length;
          @ decreasing a.length - \index;
          @ assignable \strictly_nothing;
          @*/
        for (int i : a) s += i;
        return s;
    }
}
