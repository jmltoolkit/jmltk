public class JmlPredicateResolutionTest {
    public int value;

    //@ instance predicate Pred(int i) = i > 0;

    //@ model predicate Between(int lo, int hi) = value <= lo && lo <= hi;
}

//? name: hi@(line 6,col 72) to hi@(line 6,col 41)
//? name: i@(line 4,col 42) to i@(line 4,col 33)
//? name: lo@(line 6,col 60) to lo@(line 6,col 33)
//? name: lo@(line 6,col 66) to lo@(line 6,col 33)
//? name: value@(line 6,col 51) to value@(line 2,col 5)
//? type: hi@(line 6,col 72)
//? type: i@(line 4,col 42)
//? type: lo@(line 6,col 60)
//? type: lo@(line 6,col 66)
//? type: value@(line 6,col 51)
