// Generated for the quantarray feature: element-domain quantifiers over a
// ghost array declaration — as a class invariant and inside a method-body
// assert. Parsed by FullExamplesTest with processJml=true, exercising the
// grammar and JmlProcessor validation for `(\forall int e : array; ...)`.
package tested;

/** Element quantification over a {@code //@ ghost} array. */
public class GhostArrayQuantifier {
    //@ ghost int[] array = new int[10];

    //@ invariant (\forall int e : array; e >= 0);

    /*@ requires array != null && array.length > 0;
      @ ensures (\forall int e : array; e > 0);
      @*/
    public void allElementsPositive() {
        //@ assert (\forall int e : array; e > 0);
    }
}
