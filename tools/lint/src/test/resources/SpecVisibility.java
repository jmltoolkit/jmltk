public class SpecVisibility {
    private int secret;
    public int visible;

    //@ spec_public
    private int widened;

    //@ public invariant visible >= 0;
    //@ public invariant secret >= 0;

    //@ spec_private invariant secret >= 0;

    //@ requires visible >= 0;
    //@ requires secret >= 0;
    public void publicMethod() {
    }

    //@ requires secret >= 0;
    private void privateMethod() {
    }

    //@ spec_public
    private int hidden() {
        return secret;
    }

    //@ public invariant widened >= 0;

    //@ requires hidden() >= 0;
    public void caller() {
    }
}
