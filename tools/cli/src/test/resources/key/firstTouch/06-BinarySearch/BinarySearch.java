// KeY firstTouch 06-BinarySearch (key.ui/examples/firstTouch), VCG-adapted:
// the classic search verified through its loop invariant. The only deviation
// from the KeY file is the explicit `a != null` requires (KeY assumes non-null
// parameters through its proof config, the VCG needs the clause).
class KeyBinarySearch {

    /*@ public normal_behaviour
      @   requires a != null &&
      @     (\forall int x; (\forall int y; 0 <= x && x < y && y < a.length; a[x] <= a[y]));
      @   ensures ((\exists int x; 0 <= x && x < a.length; a[x] == v) ? a[\result] == v : \result == -1);
      @*/
    static /*@pure@*/ int search(int[] a, int v) {
        int l = 0;
        int r = a.length - 1;

        if(a.length == 0) return -1;
        if(a.length == 1) return a[0] == v ? 0 : -1;

        /*@ loop_invariant 0 <= l && l < r && r < a.length
          @                && (\forall int x; 0 <= x && x < l; a[x] < v)
          @                && (\forall int x; r < x && x < a.length; v < a[x]);
          @ assignable \nothing;
          @ decreases r - l;
          @*/
        while(r > l + 1) {
            int mid = l + (r - l) / 2;
            if(a[mid] == v) {
                return mid;
            } else if(a[mid] > v) {
                r = mid;
            } else {
                l = mid;
            }
        }

        if(a[l] == v) return l;
        if(a[r] == v) return r;
        return -1;
    }
}
