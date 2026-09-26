public class VcgExamples {

    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int abs(int x) {
        if (x >= 0) {
            return x;
        } else {
            return -x;
        }
    }

    //@ requires x >= 0;
    //@ ensures \result >= x + 2;
    public int buggy(int x) {
        return x + 1;
    }

    //@ requires n >= 0 && n <= 5;
    //@ ensures \result >= 0;
    public int sumBounded(int n) {
        int s = 0;
        int i = 0;
        while (i < n) {
            s = s + i;
            i = i + 1;
        }
        return s;
    }

    //@ requires n >= 0;
    //@ ensures \result >= 0;
    public int sumInvariant(int n) {
        int s = 0;
        int i = 0;
        //@ maintaining 0 <= i && i <= n && s >= 0;
        //@ decreasing n - i;
        while (i < n) {
            s = s + i;
            i = i + 1;
        }
        return s;
    }

    //@ requires n >= 0;
    //@ ensures \result == 0 || \result == 1;
    public int loopContractBreak(int n) {
        int i = 0;
        int r = 0;
        //@ maintaining 0 <= i && i <= n && r == 0;
        //@ decreasing n - i;
        //@ breaks () r == 1;
        while (i < n) {
            if (i == n - 1) {
                r = 1;
                break;
            }
            i = i + 1;
        }
        return r;
    }

    //@ requires x >= 0;
    //@ ensures \result >= 1;
    public int useInc(int x) {
        return inc(x);
    }

    //@ requires x >= 0;
    //@ ensures \result > x;
    public int inc(int x) {
        return x + 1;
    }

    //@ requires x >= 0 && x <= 100;
    //@ ensures \result == x + 1;
    public int useIncInline(int x) {
        return inc(x);
    }

    int counter;

    //@ requires n >= 0;
    //@ ensures counter == n;
    public int countUp(int n) {
        counter = 0;
        int i = 0;
        //@ maintaining 0 <= i && i <= n && counter == i;
        //@ decreasing n - i;
        while (i < n) {
            counter = counter + 1;
            i = i + 1;
        }
        return counter;
    }

    //@ requires x != 0;
    //@ ensures \result == 1;
    public int divideByItselfWithTry(int x) {
        int r = 0;
        try {
            r = x / x;
        } catch (ArithmeticException e) {
            r = 0;
        }
        if (r == 1) {
            return 1;
        } else {
            return 0;
        }
    }

    //@ requires a != null && a.length >= 1;
    //@ ensures a[0] == 7;
    public int storeFirst(int[] a, int v) {
        a[0] = 7;
        return a[0];
    }

    static class Box {
        int value;

        //@ assigns value;
        //@ ensures value == 42;
        public void bumpBoxValue() {
            this.value = 42;
        }
    }

    //@ requires b != null;
    //@ ensures \result == 5 && b.value == 5;
    public int setBox(Box b) {
        b.value = 5;
        return b.value;
    }

    //@ requires a != null && a.length >= 3 && i >= 0 && i < a.length;
    public void fillFrom(int[] a, int i, int value) {
        while (i < a.length) {
            a[i] = value;
            i = i + 1;
        }
    }

    //@ requires (\forall int j; 0 <= j && j < a.length; a[j] >= 0);
    //@ ensures \result >= 0;
    public int sumArray(int[] a) {
        int s = 0;
        int i = 0;
        //@ maintaining 0 <= i && i <= a.length && s >= 0;
        //@ decreasing a.length - i;
        while (i < a.length) {
            s = s + a[i];
            i = i + 1;
        }
        return s;
    }

    //@ requires a != null && a.length <= 4 && (\forall int i; 0 <= i && i < a.length - 1; a[i] <= a[i + 1]);
    //@ ensures \result == -1 || (0 <= \result && \result < a.length && a[\result] == key);
    public int binarySearch(int[] a, int key) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == key) {
                return mid;
            } else if (a[mid] < key) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    int sumResult;
    int maxResult;

    //@ requires a != null && a.length >= 1 && (\forall int i; 0 <= i && i < a.length; a[i] >= 0);
    //@ ensures sumResult >= 0 && maxResult >= 0;
    public void sumAndMax(int[] a) {
        int s = 0;
        int m = 0;
        int i = 0;
        //@ maintaining 0 <= i && i <= a.length && s >= 0 && m >= 0;
        //@ decreasing a.length - i;
        while (i < a.length) {
            s = s + a[i];
            if (a[i] > m) {
                m = a[i];
            }
            i = i + 1;
        }
        sumResult = s;
        maxResult = m;
    }

    //@ ensures counter == 1;
    public void callIncCounter() {
        counter = 0;
        incCounter();
    }

    public void incCounter() {
        counter = counter + 1;
    }

    int fieldV;

    //@ requires fieldV == 0;
    //@ ensures fieldV == 42;
    public void callBumpFieldTo42() {
        fieldV = 0;
        bumpFieldTo42();
    }

    //@ assigns fieldV;
    //@ ensures fieldV == 42;
    public void bumpFieldTo42() {
        fieldV = 42;
    }

    //@ requires true;
    //@ ensures fieldV == 5;
    public void callBumpFieldThenCheck() {
        fieldV = 1;
        bumpField();
    }

    //@ assigns fieldV;
    //@ ensures fieldV == 7;
    public void bumpField() {
        fieldV = 7;
    }

    Box box;

    //@ requires box != null;
    //@ ensures box.value == 42;
    public void callBumpBoxValue() {
        box.value = 1;
        box.bumpBoxValue();
    }

    //@ requires box != null;
    //@ ensures box.value == 42;
    public void callBumpBoxValueInline() {
        box.value = 1;
        box.bumpBoxValue();
    }

    //@ requires a != null;
    public int storeUnchecked(int[] a, int i, int v) {
        a[i] = v;
        return a[0];
    }

    //@ requires true;
    public int divUnsafe(int x, int y) {
        return x / y;
    }

    //@ requires counter == 5;
    //@ ensures counter == \old(counter);
    public void bumpButClaimUnchanged() {
        counter = counter + 1;
    }

    //@ requires n >= 10;
    //@ ensures \result >= 0;
    public int unmaintainedInvariant(int n) {
        int s = 1;
        int i = 0;
        //@ maintaining s == 1 && 0 <= i && i <= n;
        //@ decreasing n - i;
        while (i < n) {
            s = s + 1;
            i = i + 1;
        }
        return s;
    }

    //@ requires a != null && a.length >= 1 && (\forall int j; 0 <= j && j < a.length; a[j] >= 0);
    //@ ensures \result >= a[0] + 2;
    public int buggyQuantifiedPost(int[] a) {
        return 0;
    }

    // ---- beyond: unit-test fixtures for the VC generation stage ----

    int ctorField;

    //@ requires v > 0;
    //@ ensures ctorField == v;
    public VcgExamples(int v) {
        this.ctorField = v;
    }

    static int staticCounter;

    //@ requires staticCounter == 0;
    //@ ensures staticCounter == 1;
    public void bumpStatic() {
        staticCounter = staticCounter + 1;
        return;
    }

    //@ requires true;
    //@ ensures \result == 0;
    public int usesUnknownType(NoSuchType t) {
        return 0;
    }

    int val;

    //@ requires this.val == 0;
    //@ ensures this.val == 9;
    public void setThisField() {
        this.val = this.val + 9;
    }

    //@ requires true;
    //@ ensures \result == x;
    public int probeGhostField(int x) {
        int y = this.ghostValue + 1;
        return x;
    }

    static class Counter {
        int n;

        //@ assignable this.n;
        //@ ensures this.n == 6;
        public void setSix() {
            this.n = 6;
        }
    }

    //@ requires c != null;
    //@ ensures \result == c.n;
    public int readCounter(Counter c) {
        return c.n;
    }

    //@ requires c != null;
    //@ ensures c.n == 7 && \result == 7;
    public int writeReadCounter(Counter c) {
        c.n = 7;
        return c.n;
    }

    //@ requires greatestSoFar >= 0;
    //@ ensures \result == 0;
    public int unknownContractName(int x) {
        return 0;
    }

    //@ requires true;
    //@ ensures \result == 0;
    public int unknownBodyName(int x) {
        int q = mysteryScalar;
        return 0;
    }

    //@ requires n >= 0;
    //@ ensures \result == n;
    public int havocAndNestedLoops(int n) {
        int i = 0;
        while (i < n) {
            int scratch;
            scratch = i + 1;
            int j = 0;
            while (j < scratch) {
                j = j + 1;
            }
            i = i + 1;
        }
        return n;
    }

    public int switchNotSupported(int x) {
        switch (x) {
            case 1:
                return 1;
            default:
                return 0;
        }
    }

    //@ requires n > 1;
    //@ ensures \result == 0;
    public int breakAndContinue(int n) {
        int i = 0;
        int s = 0;
        //@ maintaining 0 <= i && i <= n && s == 0;
        //@ decreasing n - i;
        //@ breaks () s == 0;
        //@ continues () s == 0;
        while (i < n) {
            if (i == 2) {
                i = i + 1;
                continue;
            }
            if (i == 3) {
                i = i + 1;
                break;
            }
            i = i + 1;
        }
        return s;
    }

    //@ requires true;
    //@ ensures \result == 5;
    public int tryFinallyOnly(int x) {
        try {
            return 5;
        } finally {
            int z = x + 1;
        }
    }

    //@ requires x >= 2;
    //@ ensures \result >= 0;
    public int multiCatchFinally(int x) {
        int r = 0;
        try {
            r = 100 / (x - 1);
        } catch (ArithmeticException e) {
            r = 1;
        } catch (RuntimeException e) {
            r = 2;
        } finally {
            r = r + 1;
        }
        return r;
    }

    //@ requires x >= -100 && x <= 100 && y >= -100 && y <= 100;
    //@ ensures \result == x * y;
    public int boundedMul(int x, int y) {
        return x * y;
    }

    //@ requires x >= -1000 && x <= 1000 && y >= -1000 && y <= 1000;
    //@ ensures \result == x + y;
    public int boundedAdd(int x, int y) {
        return x + y;
    }

    //@ requires a != null && a.length >= 1;
    //@ ensures a[0] == 1;
    public void boundedStore(int[] a) {
        a[0] = 1;
    }

    //@ ensures \result == 2147483647 + 1;
    public int overflowDetected() {
        return 2147483647 + 1;
    }

    //@ requires x > 0;
    //@ ensures \result == x;
    public int bodyAssert(int x) {
        //@ assume 1 < 2;
        //@ assert x > 0;
        return x;
    }

    //@ requires a != null;
    //@ ensures \result == 2;
    public int arrayNullInStmt(int[] a) {
        boolean l = a == null;
        boolean r = a != null;
        int s = 0;
        if (l) {
            s = s + 1;
        }
        if (r) {
            s = s + 2;
        }
        return s;
    }

    //@ requires 0 <= x && x <= 10;
    //@ ensures \result >= 1;
    public int miscExprs(int x, int c) {
        int a = c > 0 ? x : x + 1;
        int b = (int) (a);
        int d = (x);
        return d + b - d + 1;
    }
    //@ requires true;
    //@ ensures true;
    public int callUnresolved(int x) {
        int v = mysteryHelper(x);
        return v;
    }

    //@ requires true;
    //@ ensures \result == 1;
    public int declareInThen(int c, int x) {
        if (c > 0) {
            int z = x;
        }
        return 1;
    }

    public int loopWithoutInvariant(int n) {
        int i = 0;
        while (i < n) {
            i = i + 1;
        }
        return 0;
    }

    //@ requires n >= 3;
    //@ ensures \result == 3;
    public int unrolledLoopWithBreak(int n) {
        int i = 0;
        int acc = 0;
        while (i < n) {
            if (i == 3) {
                break;
            }
            acc = acc + 1;
            i = i + 1;
        }
        return acc;
    }

    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int level0(int x) {
        return level1(x + 1);
    }

    //@ requires x >= 0;
    //@ ensures \result > x;
    public int level1(int x) {
        return level2(x + 1);
    }

    //@ requires x >= 0;
    //@ ensures \result > x;
    public int level2(int x) {
        return x + 1;
    }

    //@ requires y != 0;
    //@ ensures \result == x / y;
    public int boundedDiv(int x, int y) {
        return x / y;
    }

    //@ requires b != null && a != null && a.length >= 1 && b.n >= 0 && a[0] >= 0;
    //@ ensures \result >= 0;
    public int callInlineMixedArgs(Counter b, int[] a) {
        int v = incScalar(b.n);
        int w = incScalar(a[0]);
        int u = incScalar(0);
        return v + w + u;
    }

    public int incScalar(int x) {
        return x + 1;
    }

    //@ requires true;
    //@ ensures fieldV == 7;
    public void callBumpEverything() {
        bumpEverything();
    }

    //@ assignable \everything;
    //@ ensures fieldV == 7;
    public void bumpEverything() {
        fieldV = 7;
    }

    //@ requires true;
    //@ ensures fieldV == 11;
    public void callAssignableField() {
        bumpViaFieldClause();
    }

    //@ assignable this.fieldV;
    //@ ensures fieldV == 11;
    public void bumpViaFieldClause() {
        fieldV = 11;
    }

    //@ requires a != null && a.length >= 1;
    //@ ensures a[0] == 3;
    public void callAssignableArray(int[] a) {
        setFirstP(a);
    }

    //@ requires a != null && a.length >= 1;
    //@ assignable a[0];
    //@ ensures a[0] == 3;
    public void setFirstP(int[] a) {
        a[0] = 3;
    }

    //@ requires true;
    //@ ensures true;
    public void callAssignableCast(int[] a) {
        bumpCastA(a);
    }

    //@ requires a != null;
    //@ assignable (int[]) a;
    //@ ensures true;
    public void bumpCastA(int[] a) {
    }

    //@ requires true;
    //@ ensures true;
    public void callAssignableNothing(int[] a) {
        bumpNothingA(a);
    }

    //@ assignable \nothing;
    //@ ensures a[0] == a[0];
    public void bumpNothingA(int[] a) {
    }

    //@ requires true;
    //@ ensures \result == 0;
    public int newObject(int x) {
        Foo f = new Foo();
        return 0;
    }

    static class Foo {
    }

    //@ requires x > 0;
    //@ ensures \result == 1;
    public int throwNew(int x) {
        if (x == 0) {
            throw new RuntimeException("zero");
        }
        return 1;
    }

    //@ requires true;
    //@ ensures true;
    public int boundedOverflow(int x) {
        return x + 1;
    }

    //@ requires true;
    //@ ensures true;
    public int boundedOob(int[] a) {
        return a[5];
    }

    //@ requires true;
    //@ ensures \result >= 0;
    public int ifThenPhi(int c) {
        int x = 0;
        if (c > 0) {
            x = c;
        }
        return x;
    }

    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int doubleReturn(int x) {
        if (x == 0) {
            return 1;
        }
        return x;
    }

    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == n;
    public int doWhileCount(int n) {
        int i = 0;
        do {
            i = i + 1;
        } while (i < n);
        return i;
    }

    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == n;
    public int unrolledContinue(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            s = s + i;
        }
        return i;
    }

    //@ requires n >= 0 && n <= 10;
    //@ ensures \result == n;
    public int linearCount(int n) {
        int i = 0;
        while (i < n) {
            i = i + 1;
        }
        return i;
    }

    //@ requires a != null && a.length >= 1 && a.length <= 5
    //@     && (\forall int j; 0 <= j && j < a.length; 0 <= a[j] && a[j] <= 1000);
    //@ ensures \result >= 0;
    public int sumBoundedArr(int[] a) {
        int s = 0;
        int i = 0;
        while (i < a.length) {
            s = s + a[i];
            i = i + 1;
        }
        return s;
    }

    // ---- corner cases (open user questions) ----

    //@ requires x == -2147483647 - 1;
    //@ ensures \result == -2147483646 - 1;
    public int minUnderflow(int x) {
        return x - 1;
    }

    // bounded arithmetic wraps MIN_VALUE - 1 around to MAX_VALUE
    //@ requires x == -2147483647 - 1;
    //@ ensures \result == 2147483647;
    public int minUnderflowWrap(int x) {
        return x - 1;
    }

    //@ requires x == 2147483646;
    //@ ensures \result == 2147483646 + 1;
    public int maxOverflow(int x) {
        return x + 1;
    }

    //@ requires true;
    //@ ensures \result == -2147483647 - 1;
    public int intMinLiteral() {
        return -2147483647 - 1;
    }

    //@ requires true;
    //@ ensures \result >= 2147483647 - 1;
    public int intMaxLiteral() {
        return 2147483647;
    }

    //@ requires true;
    //@ ensures \result == 2147483646;
    public int nearMax(int x) {
        return 2147483647 - 1;
    }

    //@ requires x >= -2147483647 && x <= 2147483646;
    //@ ensures \result == x + 1;
    public int boundedIncrement(int x) {
        return x + 1;
    }

    //@ requires o != null;
    //@ ensures \result == 0 || \result == 1;
    public int stmtInstanceof(Object o) {
        boolean isStr = o instanceof String;
        if (isStr) {
            return 1;
        } else {
            return 0;
        }
    }

    //@ requires o != null;
    //@ ensures \result >= 0;
    public int stmtInstanceofCount(Object o) {
        boolean a = o instanceof String;
        boolean b = o instanceof Object;
        int n = 0;
        if (a) {
            n = n + 1;
        }
        if (b) {
            n = n + 1;
        }
        return n;
    }

    //@ requires x <= 2147483646;
    //@ ensures \result == x + 1;
    public int unboxedIncrement(Integer x) {
        return x.intValue() + 1;
    }

    //@ requires x >= 0 && x <= 100;
    //@ ensures \result == x;
    public Integer boxedIdentity(int x) {
        return x;
    }

    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == 1;
    public int tryBreakContinue(int n) {
        int i = 0;
        int acc = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            try {
                if (i == 3) {
                    break;
                }
                acc = acc + 1;
            } catch (RuntimeException e) {
                acc = 0;
            }
        }
        return acc;
    }

    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == 1;
    public int loopContinueNoTry(int n) {
        int i = 0;
        int acc = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            if (i == 3) {
                continue;
            }
            acc = acc + 1;
        }
        return acc;
    }

    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == 1;
    public int loopBreakNoTry(int n) {
        int i = 0;
        int acc = 0;
        while (i < n) {
            i = i + 1;
            if (i == 3) {
                break;
            }
            acc = acc + 1;
        }
        return acc;
    }

    //@ requires x != 0;
    //@ ensures \result == 2;
    public int nestedTry(int x) {
        int r = 0;
        try {
            try {
                r = x / x;
            } finally {
                r = r + 1;
            }
        } catch (ArithmeticException e) {
            r = 0;
        }
        return r;
    }

    //@ requires n >= 4 && n <= 10;
    //@ ensures \result == 2;
    public int returnInsideLoop(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            if (i == 4) {
                return s;
            }
            s = s + 1;
        }
        return s;
    }

    //@ requires n >= 4 && n <= 10;
    //@ ensures \result == 2;
    public int returnEarlyNoContinue(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            i = i + 1;
            if (i == 4) {
                return s;
            }
            s = s + 1;
        }
        return s;
    }

    //@ requires n >= 4 && n <= 10;
    //@ ensures \result == 3;
    public int plainReturnInLoop(int n) {
        int i = 0;
        while (i < n) {
            if (i == 3) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    // isolation: break inside try, no continue
    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == 1;
    public int tryBreakOnly(int n) {
        int i = 0;
        int acc = 0;
        while (i < n) {
            i = i + 1;
            try {
                if (i == 3) {
                    break;
                }
                acc = acc + 1;
            } catch (RuntimeException e) {
                acc = 0;
            }
        }
        return acc;
    }

    // isolation: continue then return on a later click, no try
    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == 1;
    public int continueThenReturn(int n) {
        int i = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            if (i == 4) {
                return 1;
            }
        }
        return 0;
    }

    // isolation: accumulate then continue, return loop counter only
    //@ requires n >= 1 && n <= 10;
    //@ ensures \result == n;
    public int continueCountOnly(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            s = s + i;
        }
        return i;
    }

    //@ requires n >= 3 && n <= 10;
    //@ ensures \result == 3;
    public int nonNormalLoopExit(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            i = i + 1;
            if (i == 3) {
                break;
            }
            s = s + i;
        }
        return s;
    }

    //@ requires n >= 0 && n <= 10;
    //@ ensures \result == 0 || \result == 1;
    public int tryReturnFinally(int n) {
        int r = 0;
        try {
            return n == 0 ? 1 : 0;
        } finally {
            r = 1;
        }
    }

    //@ requires x >= 0;
    //@ ensures \result == 0;
    public int nestedCatchFinally(int x) {
        int r = 0;
        try {
            try {
                r = 10 / (x + 1);
            } catch (ArithmeticException e) {
                r = -1;
            } finally {
                r = r + 1;
            }
        } finally {
            r = r + 1;
        }
        return r - 2;
    }
}
