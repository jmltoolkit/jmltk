public class Locsets {
    int[] a;
    int x;

    //@ assignable \nothing;
    void empty() {
    }

    //@ assignable \strictly_nothing;
    void strictlyEmpty() {
    }

    //@ assignable x;
    void single() {
    }

    //@ assignable a[0];
    void cell() {
    }

    //@ assignable a[*];
    void array() {
    }

    //@ assignable \everything;
    void vacuous() {
    }

    //@ assignable x, \everything;
    void vacuousUnion() {
    }
}
