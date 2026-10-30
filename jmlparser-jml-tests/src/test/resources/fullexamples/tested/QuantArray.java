// Generated for the quantarray feature: single-binding element quantification
// over arrays and Iterables. This file is parsed by FullExamplesTest with
// processJml=true, exercising both the grammar and JmlProcessor validation.
package tested;

/**
 * Iterable-domain quantifiers in method contracts:
 * {@code (\forall int e : a; ...)}.
 */
public class QuantArray {
    private /*@ spec_public @*/ int[] a;

    /*@ requires a != null;
      @ ensures (\forall int e : a; e >= 0);
      @*/
    public void allNonNegative() {
    }

    /*@ requires a != null && a.length > 0;
      @ ensures \result == (\sum int e : a; e * e);
      @*/
    public /*@ pure @*/ int sumOfSquares() {
        return 0;
    }
}
