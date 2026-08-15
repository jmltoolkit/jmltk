package com.github.jml.resolution;

/**
 * @author Alexander Weigl
 * @version 1 (08.07.22)
 */
public class JmlQuantifiedExprResolutionTest {
    int z;
    //@invariant (\forall int x; x > 2; x > 5 * z);
    //@invariant (\exists int x; x > 2; x > 5 * z);
    //@invariant (\sum int x, y; 0 < y < x < 25; x*y*z);
}

//? name: x@(line 10,col 34) to x@(line 10,col 18)
//? name: x@(line 10,col 41) to x@(line 10,col 18)
//? name: x@(line 11,col 42) to x@(line 11,col 18)
//? name: x@(line 11,col 50) to x@(line 11,col 18)
//? name: x@(line 9,col 34) to x@(line 9,col 18)
//? name: x@(line 9,col 41) to x@(line 9,col 18)
//? name: y@(line 11,col 38) to y@(line 11,col 18)
//? name: y@(line 11,col 52) to y@(line 11,col 18)
//? name: z@(line 10,col 49) to z@(line 8,col 5)
//? name: z@(line 11,col 54) to z@(line 8,col 5)
//? name: z@(line 9,col 49) to z@(line 8,col 5)
//? type: x@(line 10,col 34)
//? type: x@(line 10,col 41)
//? type: x@(line 11,col 42)
//? type: x@(line 11,col 50)
//? type: x@(line 9,col 34)
//? type: x@(line 9,col 41)
//? type: y@(line 11,col 38)
//? type: y@(line 11,col 52)
//? type: z@(line 10,col 49)
//? type: z@(line 11,col 54)
//? type: z@(line 9,col 49)