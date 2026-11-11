// KeY firstTouch 08-Java5 (key.ui/examples/firstTouch), VCG-adapted: the
// for-each sum whose contract uses the JML `\sum` generalized quantifier (and
// the KeY-specific `\index` loop variable). The VCG models `\sum` as a
// recursive SMT summation (`jmlSum`) over the quantifier's integer range, so
// the example verifies in both modes (see VcgExamples.KeyFor). Deviations from
// the KeY source: an explicit `a != null` requires (KeY assumes non-null fields
// through its proof config; the VCG needs the clause) and the `implements
// Iterable`/`Trivial` machinery is dropped.
final class KeyFor {

    int[] a;

    //@ requires a != null;
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
