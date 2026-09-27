public class TrivialFormulas {
    //@ requires true;
    //@ requires false;
    //@ requires 1 == 2;
    //@ requires 1 <= 2 && true;
    //@ requires x > 3;
    //@ ensures 1 < 2;
    //@ ensures 5 == 5 || false;
    //@ signals (Exception e) false;
    public void m(int a, int x) {
        //@ assert true;
        //@ assert a || !a;
        //@ assert a && !a;
        //@ assert a > 3;
        //@ assert (1 == 1) && (2 == 2);
        //@ assert null instanceof String;
        //@ assert true ? 1 == 1 : 1 == 2;
        //@ assert false ? true : false;
        //@ assert a > 0 ? true : true;
        //@ assert a > 0 ? true : 1 == 2;
        int i = 0;
    }
}
