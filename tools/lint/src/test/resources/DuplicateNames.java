public class DuplicateNames {
    int count;

    //@ ghost int count;

    //@ ghost int other;

    int f() {
        return 0;
    }

    //@ model int f();

    void m() {
        //@ ghost int q = 0;
        int q = 1;
    }

    void ok() {
        //@ ghost int q = 0;
        int r = 1;
    }
}
