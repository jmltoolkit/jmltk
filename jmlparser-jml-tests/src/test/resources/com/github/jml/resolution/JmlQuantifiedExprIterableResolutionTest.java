package com.github.jml.resolution;

import java.util.List;

public class JmlQuantifiedExprIterableResolutionTest {
    int[] a;
    int[][] m;
    List<String> words;
    //@ invariant (\forall int e : a; e > 0);
    //@ invariant (\forall int[] row : m; row.length > 0);
    //@ invariant (\forall String s : words; s.length() > 0);
    //@ invariant (\forall var v : a; v > 1);
    //@ invariant (\forall int i : 0..a.length; a[i] > 0);
    //@ invariant (\forall int x : a; (\exists int y : a; y > x));
}

//? name: a@(line 12,col 36) to a@(line 6,col 5)
//? name: a@(line 13,col 39) to a@(line 6,col 5)
//? name: a@(line 13,col 49) to a@(line 6,col 5)
//? name: a@(line 14,col 36) to a@(line 6,col 5)
//? name: a@(line 14,col 56) to a@(line 6,col 5)
//? name: a@(line 9,col 36) to a@(line 6,col 5)
//? name: e@(line 9,col 39) to e@(line 9,col 19)
//? name: i@(line 13,col 51) to i@(line 13,col 19)
//? name: m@(line 10,col 40) to m@(line 7,col 5)
//? name: row@(line 10,col 43) to row@(line 10,col 19)
//? name: s@(line 11,col 46) to s@(line 11,col 19)
//? name: v@(line 12,col 39) to v@(line 12,col 19)
//? name: words@(line 11,col 39) to words@(line 8,col 5)
//? name: x@(line 14,col 63) to x@(line 14,col 19)
//? name: y@(line 14,col 59) to y@(line 14,col 39)
//? type: a@(line 12,col 36)
//? type: a@(line 13,col 39)
//? type: a@(line 13,col 49)
//? type: a@(line 14,col 36)
//? type: a@(line 14,col 56)
//? type: a@(line 9,col 36)
//? type: e@(line 9,col 39)
//? type: i@(line 13,col 51)
//? type: m@(line 10,col 40)
//? type: row@(line 10,col 43)
//? type: s@(line 11,col 46)
//? type: v@(line 12,col 39)
//? type: words@(line 11,col 39)
//? type: x@(line 14,col 63)
//? type: y@(line 14,col 59)
