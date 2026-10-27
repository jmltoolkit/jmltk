public class JmlLemmaResolutionTest {
    public int value;

    //@ instance lemma Pred(int i) = i > 0;

    //@ model lemma Between(int lo, int hi) = value <= lo && lo <= hi;
}

//? name: hi@(line 6,col 68) to hi@(line 6,col 37)
//? name: i@(line 4,col 38) to i@(line 4,col 29)
//? name: lo@(line 6,col 56) to lo@(line 6,col 29)
//? name: lo@(line 6,col 62) to lo@(line 6,col 29)
//? name: value@(line 6,col 47) to value@(line 2,col 5)
//? type: hi@(line 6,col 68)
//? type: i@(line 4,col 38)
//? type: lo@(line 6,col 56)
//? type: lo@(line 6,col 62)
//? type: value@(line 6,col 47)
