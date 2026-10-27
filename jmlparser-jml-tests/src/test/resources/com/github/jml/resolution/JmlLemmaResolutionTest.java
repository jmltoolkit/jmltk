public class JmlLemmaResolutionTest {

    //@ lemma Pred(int i) = i > 0;
    //@ lemma seqLemma(\seq s, \bigint n) \by { use_lemma nospace(s, n); auto; };
}


//? e type: n@(line 4,col 70)
//? e type: s@(line 4,col 67)
//? name: i@(line 3,col 29) to i@(line 3,col 20)
//? name: n@(line 4,col 70) to n@(line 4,col 32)
//? name: s@(line 4,col 67) to s@(line 4,col 24)
//? type: i@(line 3,col 29)
