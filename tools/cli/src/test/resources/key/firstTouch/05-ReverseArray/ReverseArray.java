// KeY firstTouch 05-ReverseArray (key.ui/examples/firstTouch), VCG-adapted:
// the in-place reversal with a quantified `\old` content invariant and the
// element-wise copy variant. Deviations from the KeY file, documented in the
// VCG fixture header (tools/vcg/src/test/resources/VcgExamples.java): KeY
// assumes non-null parameters through its proof config, the VCG needs an
// explicit `a != null` requires; the `main` driver is dropped.
class KeyReverseArray {

    public int[] a;

    public KeyReverseArray() {
    }

    /*@ public normal_behavior
      @ requires a != null && a.length >= 0;
      @ ensures (\forall int j; j >= 0 && j < a.length; a[j] == \old(a[a.length - (j+1)]));
      @ assignable a[*];
      @*/
    public void reverse() {
        int i = 0;

        final int length = (a.length / 2);
        /*@
          @ loop_invariant
          @   (\forall int j; j >= 0 && j < i; \old(a[a.length - (j+1)]) == a[j] && \old(a[j]) == a[a.length - (j+1)])
          @   && (\forall int j; j >= i && j < length; \old(a[a.length - (j+1)]) == a[a.length - (j+1)] && \old(a[j]) == a[j])
          @   && (a.length % 2 != 0 ==> \old(a[a.length / 2]) == a[length])
          @   && i >= 0 && i <= length;
          @ assignable a[*];
          @ decreases a.length - i;
          @*/
        while (i < length) {
            int tmp = a[i];
            a[i] = a[a.length - (i+1)];
            a[a.length - (i+1)] = tmp;
            i++;
        }
    }

    /*@ public normal_behavior
      @ requires p_a != null && p_a.length >= 0;
      @ ensures (\forall int j; j >= 0 && j < \old(p_a.length); \result[j] == \old(p_a[p_a.length - (j+1)])) &&
      @\result.length == \old(p_a.length);
      @ diverges true;
      @*/
    public int[] reverse2(int[] p_a) {
        int[] b = new int[p_a.length];
        int i = 0;
        /*@
          @ loop_invariant (\forall int j; j >= 0 && j < i; b[j] == p_a[p_a.length - (j+1)]) && i >= 0 && i <= p_a.length;
          @ assignable b[*];
          @*/
        while (i < p_a.length) {
            b[i] = p_a[p_a.length - (i+1)];
            i++;
        }
        return b;
    }
}
