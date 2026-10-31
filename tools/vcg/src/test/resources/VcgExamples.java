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

    // regression: when a loop is handled via its invariant, the return/break/continue
    // flags are havoced on the loop-exit path; they must be tied to "the body did
    // not return" — otherwise the post-loop `return -1` is treated as optional and
    // `\result` stays at its pre-loop value (`ensures \result == -1` → falsifiable)
    //@ requires n >= 0;
    //@ ensures \result == -1;
    public int loopNoReturn(int n) {
        int i = 0;
        //@ maintaining 0 <= i && i <= n;
        //@ decreasing n - i;
        while (i < n) {
            i = i + 1;
        }
        return -1;
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

    //@ requires a != null && (\forall int j; 0 <= j && j < a.length; a[j] >= 0);
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

    // KeY's classic binary search (key.core.testgen/.../BinarySearch.java), verified
    // through the loop's invariant: a `return` inside the loop body plus post-loop
    // `return -1` is the exact shape that exposed the loop-exit return-flag bug
    //@ requires a != null &&
    //@     (\forall int x; (\forall int y; 0 <= x && x < y && y < a.length; a[x] <= a[y]));
    //@ ensures ((\exists int x; 0 <= x && x < a.length; a[x] == v) ? a[\result] == v : \result == -1);
    static int binarySearchInv(int[] a, int v) {
        int l = 0;
        int r = a.length - 1;
        if (a.length == 0) return -1;
        if (a.length == 1) return a[0] == v ? 0 : -1;
        //@ maintaining 0 <= l && l < r && r < a.length
        //@           && (\forall int x; 0 <= x && x < l; a[x] < v)
        //@           && (\forall int x; r < x && x < a.length; v < a[x]);
        //@ decreasing r - l;
        while (r > l + 1) {
            int mid = (l + r) / 2;
            if (a[mid] == v) {
                return mid;
            } else if (a[mid] > v) {
                r = mid;
            } else {
                l = mid;
            }
        }
        if (a[l] == v) return l;
        if (a[r] == v) return r;
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

    // aliasing through the enclosing instance: a local that provably denotes `this`
    // (`VcgExamples alias = this`) carries its field writes into `this.val`
    //@ requires this.val == 0;
    //@ ensures this.val == 9;
    public void aliasThisViaLocal() {
        VcgExamples alias = this;
        alias.val = 9;
    }

    // the same via a parameter whose aliasing of `this` is assumed by contract
    //@ requires alias == this && this.val == 0;
    //@ ensures this.val == 7;
    public void aliasThisViaParam(VcgExamples alias) {
        alias.val = 7;
    }

    // bare-name counterpart: the write through the alias of `this` mutates `val`,
    // observable under its bare name in the postcondition
    //@ requires this.val == 0;
    //@ ensures val == 9;
    public void setValViaAlias() {
        VcgExamples alias = this;
        alias.val = 9;
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

    //@ requires x >= 0;
    //@ ensures \result == (x == 1 ? 10 : 20);
    public int switchBasic(int x) {
        int r;
        switch (x) {
            case 1:
                r = 10;
                break;
            default:
                r = 20;
        }
        return r;
    }

    //@ requires x >= 0;
    //@ ensures \result == 10;
    public int switchWrong(int x) {
        int r;
        switch (x) {
            case 1:
                r = 10;
                break;
            default:
                r = 20;
        }
        return r;
    }

    //@ requires x >= 0;
    //@ ensures \result == (x == 1 || x == 2 ? 42 : 0);
    public int switchFallThrough(int x) {
        int r = 0;
        switch (x) {
            case 1:
            case 2:
                r = 42;
                break;
            default:
                r = 0;
        }
        return r;
    }

    //@ requires x >= 0;
    //@ ensures \result == (x == 1 ? 1 : (x == 2 ? 2 : 3));
    public int switchBreakStops(int x) {
        int r = 0;
        switch (x) {
            case 1:
                r = 1;
                break;
            case 2:
                r = 2;
                break;
            default:
                r = 3;
        }
        return r;
    }

    // switch inside a loop: a `continue` inside a case must target the loop (not the
    // switch), a `break` inside a case must exit only the switch.
    //@ requires n >= 0;
    //@ ensures \result == (n == 0 ? 0 : 2 * n - 1);
    public int switchBreakStaysInLoop(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            switch (i) {
                case 0:
                    s = s + 1;
                    break;
                default:
                    s = s + 2;
            }
            i = i + 1;
        }
        return s;
    }

    //@ requires n >= 0;
    //@ ensures \result == n / 2;
    public int switchContinueInLoop(int n) {
        int i = 0;
        int s = 0;
        while (i < n) {
            switch (i % 2) {
                case 0:
                    i = i + 1;
                    continue;
                default:
                    i = i + 1;
            }
            s = s + 1;
        }
        return s;
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

    // P1.3: an unrefined call (unresolvable callee) is rejected by default; with
    // allowUnrefinedCalls the caller-visible frame is havoced, so a caller that
    // observes a field after the call cannot assume it is untouched. `this.val`
    // must be read before the call so the havoc has a slot to refresh.
    //@ requires true;
    //@ ensures this.val == 0;
    public int callUnresolvedPreservesVal(int x) {
        int before = this.val;
        mysteryHelper(x);
        return this.val;
    }

    // the sound counterpart under the flag: returning the post-call field reads the
    // havoced value, so `\result == this.val` stays provable.
    //@ requires true;
    //@ ensures \result == this.val;
    public int callUnresolvedRereads(int x) {
        mysteryHelper(x);
        return this.val;
    }

    //@ requires true;
    //@ ensures \result == 1;
    public int declareInThen(int c, int x) {
        if (c > 0) {
            int z = x;
        }
        return 1;
    }

    //@ ensures \result == 0;
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

    //@ requires a != null;
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

    // Two spec cases with disjoint requires — the JML disjunctive semantics. The
    // correct body satisfies both; call sites may pick either case.
    //@ requires x >= 0;
    //@ ensures \result == x;
    //@ also
    //@ requires x < 0;
    //@ ensures \result == -x;
    public int twoCaseAbs(int x) {
        return x < 0 ? -x : x;
    }

    //@ requires true;
    //@ ensures \result == 6;
    public int callTwoCaseAbs() {
        return twoCaseAbs(-6);
    }

    // This body is wrong for the second case (and for x == 0 the two cases
    // overlap): self-verification must expose a failing postcondition.
    //@ requires x >= 0;
    //@ ensures \result == x;
    //@ also
    //@ requires x < 0;
    //@ ensures \result == -x;
    public int twoCaseAbsWrong(int x) {
        return x;
    }

    // Neither spec case applies to `x` in the gap (-100 ... 100): no legal caller
    // may enter here, so nothing may be proven from a contradictory merged requires
    // — this must fail after the disjunctive-semantics fix.
    //@ requires x > 100;
    //@ ensures \result == 1;
    //@ also
    //@ requires x < -100;
    //@ ensures \result == 2;
    public int unreachableGap(int x) {
        return 0;
    }

    // byte/short/char arithmetic runs at int precision after binary numeric
    // promotion (JLS 5.6): a byte sum can reach 128 without wrapping at 8 bits.
    //@ requires true;
    //@ ensures \result == 128;
    public int bytePlus() {
        byte a = 127;
        byte b = 1;
        return a + b;
    }

    // byte multiply would wrap at 8 bits (500 mod 256 == 244) but not at the
    // promoted int width.
    //@ requires true;
    //@ ensures \result == 500;
    public int byteMultiply() {
        byte a = 100;
        byte b = 5;
        return a * b;
    }

    // compound assignment truncates the int result back to byte: 10 + 300 == 310
    // wraps to 54 on the store (JLS 5.1.3).
    //@ requires true;
    //@ ensures \result == 54;
    public int byteCompoundAssign() {
        byte b = 10;
        b += 300;
        return b;
    }

    // the explicit cast wraps (300 -> 44), and the promoted sum is then 45.
    //@ requires true;
    //@ ensures \result == 45;
    public int byteCastArith() {
        return (byte) 300 + 1;
    }

    // char values are unsigned: '\uFFFF' zero-extends to 65535 when promoted.
    //@ requires true;
    //@ ensures \result == 65535;
    public int charArith() {
        return '\uFFFF' + 0;
    }

    // a byte variable mixed with int literals arithmetic is well-sorted and
    // correct after promotion.
    //@ requires x >= 0 && x <= 127;
    //@ ensures \result == x + 300;
    public int mixedIntByte(int x) {
        byte b = 100;
        return b + x + 200;
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

    // ---- coverage-batch: targeted engine branches (Vcg.kt) ----

    static class MyErr extends RuntimeException {
    }

    // catch of a custom (unqualified) exception type -> qualifyType fallback
    //@ requires x != 0;
    //@ ensures \result == 1;
    public int customExceptionCatch(int x) {
        int r = 0;
        try {
            r = 10 / x;
        } catch (MyErr e) {
            r = 0;
        }
        return 1;
    }

    // catch type given in fully qualified form -> qualifyType dot-preserving branch
    //@ requires x != 0;
    //@ ensures \result >= 1;
    public int qualifiedCatch(int x) {
        try {
            int q = 10 / x;
            return 2;
        } catch (java.lang.ArithmeticException e) {
            return 1;
        }
    }

    // array store inside an unrolled loop -> locationKey(NfArray) in modifiedLocations
    // (the loop index reaches 2 for n == 3, so the array must hold three elements:
    // default-on index checks (P3.3) otherwise flag the third iteration's store)
    //@ requires a != null && a.length >= 3 && n >= 0 && n <= 3;
    //@ ensures a[0] == 1 && a[1] == 2;
    public void arrayStoreInLoop(int[] a, int n, int v) {
        int i = 0;
        //@ assert i >= 0;
        while (i < n) {
            a[i] = v;
            i = i + 1;
        }
        a[0] = 1;
        a[1] = 2;
    }

    // array/null comparisons with the array on the right and the null on the left
    //@ requires a != null;
    //@ ensures \result == 0;
    public int arrayNullInStmt2(int[] a) {
        boolean l = null == a;
        boolean r = a == null;
        int s = 0;
        if (l) {
            s = s + 1;
        }
        if (r) {
            s = s + 10;
        }
        return s;
    }

    // bounded subtraction with overflow checks -> widened sign-extended bvsub
    //@ requires x >= -100 && x <= 100 && y >= -100 && y <= 100;
    //@ ensures \result == x - y;
    public int boundedSub(int x, int y) {
        return x - y;
    }

    // remainder with division-by-zero checks -> checkDivision REMAINDER branch
    //@ requires y != 0;
    //@ ensures \result == x % y;
    public int boundedRem(int x, int y) {
        return x % y;
    }

    // loop contract with a continues-only contract: LOOP_CONTRACT, empty breaks list
    //@ requires n >= 0;
    //@ ensures \result == 0;
    public int loopContractContinueOnly(int n) {
        int i = 0;
        //@ maintaining 0 <= i && i <= n;
        //@ decreasing n - i;
        //@ continues () true;
        while (i < n) {
            i = i + 1;
            if (i == 3) {
                continue;
            }
        }
        return 0;
    }

    // continue statement in a loop whose contract declares only a breaks clause:
    // the continues-clause obligation must be skipped, the invariant one emitted
    //@ requires n >= 2 && n <= 10;
    //@ ensures \result == 0;
    public int loopContractContinueNoClause(int n) {
        int i = 0;
        int s = 0;
        //@ maintaining 0 <= i && i <= n && s == 0;
        //@ decreasing n - i;
        //@ breaks () s == 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                continue;
            }
            if (i == 3) {
                break;
            }
        }
        return s;
    }

    // break statement in a loop whose contract declares only a continues clause:
    // the breaks-clause obligation must be skipped
    //@ requires n >= 2 && n <= 10;
    //@ ensures \result == 0;
    public int loopContractBreakNoClause(int n) {
        int i = 0;
        int s = 0;
        //@ maintaining 0 <= i && i <= n && s == 0;
        //@ decreasing n - i;
        //@ continues () s == 0;
        while (i < n) {
            i = i + 1;
            if (i == 2) {
                break;
            }
        }
        return s;
    }

    // assignable clause naming a field by bare name -> NameExpr location key
    //@ requires true;
    //@ ensures fieldV == 13;
    public void callAssignableName() {
        bumpViaNameClause();
    }

    //@ assignable fieldV;
    //@ ensures fieldV == 13;
    public void bumpViaNameClause() {
        fieldV = 13;
    }

    // assignable clause naming something that is neither a field nor a parameter
    //@ requires true;
    //@ ensures true;
    public void callAssignableLocal() {
        bumpLocalClause();
    }

    //@ assignable mysteryScalar;
    //@ ensures true;
    public void bumpLocalClause() {
    }

    int[] dataField;

    // \everything havoc over fields incl. an array-typed field -> array branch
    //@ requires true;
    //@ ensures fieldV == 7;
    public void callBumpEverythingArr() {
        bumpEverything();
    }

    // static callee invoked through its class name under CONTRACT strategy
    //@ requires x >= 0 && x <= 100;
    //@ ensures \result == x + 1;
    public int callStaticContract(int x) {
        return VcgExamples.statInc(x);
    }

    //@ requires x >= 0;
    //@ ensures \result == x + 1;
    public static int statInc(int x) {
        return x + 1;
    }

    // non-static callee invoked through an explicit `this.` scope under CONTRACT
    //@ requires true;
    //@ ensures fieldV == 42;
    public void callThisShorthand() {
        this.bumpFieldTo42();
    }

    // static callee invoked through its class name under INLINE strategy
    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int callStaticInline(int x) {
        return VcgExamples.doubleIt(x);
    }

    public static int doubleIt(int x) {
        return x + x;
    }

    // callee with an object (non-array) reference parameter under CONTRACT
    //@ requires b != null;
    //@ ensures \result == 0;
    public int callObjParam(Box b) {
        return boxInfo(b);
    }

    //@ requires b != null;
    //@ ensures \result == 0 && b.value >= 0;
    public int boxInfo(Box b) {
        return 0;
    }

    // inlined callee with if/loop/try bodies and callee-local declarations
    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int inlineNested(int x) {
        return helperInline(x);
    }

    public int helperInline(int x) {
        int acc = x;
        if (x > 100) {
            acc = acc - 100;
        } else {
            acc = acc + 1;
        }
        int i = 0;
        while (i < 2) {
            int t = acc + 1;
            acc = t;
            i = i + 1;
        }
        try {
            acc = acc / 1;
        } catch (ArithmeticException e) {
            acc = 0;
        } finally {
            acc = acc + 1;
        }
        return acc;
    }

    // inlined callee whose array parameter aliases a caller location
    //@ requires a != null && a.length >= 1 && a[0] == 5;
    //@ ensures a[0] == 6;
    public int inlineArrayParam(int[] a) {
        bumpFirst(a);
        return a[0];
    }

    public void bumpFirst(int[] p) {
        p[0] = p[0] + 1;
    }

    // inlined call on a fresh receiver whose field is untracked by the caller
    //@ requires true;
    //@ ensures \result == 0;
    public int callNewBox() {
        new Box().bumpBoxValue();
        return 0;
    }

    static class Holder {
        int[] vals;
        Box box;
    }

    // write into an array field of an arbitrary receiver (untyped slot -> fallback)
    //@ requires h != null && h.vals != null && h.vals.length >= 1;
    //@ ensures h.vals[0] == 5;
    public void writeHolderArray(Holder h) {
        h.vals[0] = 5;
    }

    // read of an array-typed receiver field -> array field-selector
    //@ requires h != null && h.vals != null && h.vals.length >= 1;
    //@ ensures h.vals[0] == h.vals[0];
    public void readHolderArray(Holder h) {
        int v = h.vals[0];
    }

    // read of an object-typed receiver field -> JAVA_OBJECT field-selector
    //@ requires h != null && h.box != null;
    //@ ensures h.box != null;
    public void readBoxField(Holder h) {
        Box fresh = h.box;
    }

    // read of a field that does not exist on a resolved receiver type ->
    // scalar fallback sort in fieldSelectorType
    //@ requires b != null;
    //@ ensures \result == 0;
    public int readUnknownField(Box b) {
        int v = b.someField;
        return 0;
    }

    // ---- variable aliasing: a write through one reference must be visible
    // ---- through every other reference that provably denotes the same object
    // ---- or array (see aliasArrayParams / aliasArrayLocal / aliasObjectLocal)

    // a and b may be the same array; the assertion captures that with `==>`.
    // Before the reference-identity model this proves *vacuously* (array values
    // are compared extensionally by content, and the mutated contents differ at
    // index 0); afterwards it proves because `a == b` is reference equality.
    //@ requires a != null && a.length >= 1 && b != null && b.length >= 1;
    //@ ensures true;
    public void aliasArrayParams(int[] a, int[] b) {
        a[0] = 1;
        b[0] = 2;
        //@ assert a == b ==> a[0] == 2;
    }

    // the precondition pins the two parameters to denote the same array object:
    // the two stores hit one shared array, so the last one wins and `a[0]` is 2.
    // This can only be discharged once `==` on arrays means reference identity.
    //@ requires a != null && a.length >= 1 && b != null && b.length >= 1 && a == b;
    //@ ensures a[0] == 2;
    public void aliasArrayParamAliased(int[] a, int[] b) {
        a[0] = 1;
        b[0] = 2;
    }

    // a local that aliases the parameter: the store through the alias is a store to `a`
    //@ requires a != null && a.length >= 1;
    //@ ensures a[0] == 5;
    public void aliasArrayLocal(int[] a) {
        int[] a2 = a;
        a2[0] = 5;
    }

    // two aliases of the same array, each writing a different cell through its own name
    //@ requires a != null && a.length >= 2;
    //@ ensures a[0] == 7 && a[1] == 4;
    public void aliasArrayTwoLocals(int[] a) {
        int[] x = a;
        int[] y = a;
        x[0] = 7;
        y[1] = 4;
    }

    // ---- P4.2: arrays are ordinary (nullable) references ------------------
    // Unannotated array parameters/fields/locals may be null unless declared
    // `non_null`; `arr == null`/`arr != null` are real predicates; derefs carry
    // nullchecks; object-array elements are non-null by default (`nonnullelements`
    // states that default explicitly). `new T[...]` results are never null.

    // `a == null` is a real predicate: both branches are reachable and the
    // specification speaks about the null path (impossible under the legacy
    // never-null encoding — `a == null` was always false).
    //@ ensures \result == 2 || a == null;
    public int nullableArrayBranch(int[] a) {
        return (a == null) ? 1 : 2;
    }

    // a deref of a (possibly) null array guarded by an explicit null test: the
    // nullcheck on `a[0]` discharges from `a != null` on the reachable branch
    //@ requires a == null || a.length >= 1;
    //@ ensures \result == (a == null ? 0 : 1);
    public int nullableArrayGuard(int[] a) {
        if (a == null) {
            return 0;
        }
        int v = a[0];
        return 1;
    }

    // `non_null`-declared parameter: the array can never be null (the caller's
    // implicit `requires a != null`), so it derefs without nullchecks and the
    // comparison folds — the spec still needs the explicit length bound.
    //@ requires a.length >= 1;
    //@ ensures \result == a[0];
    public int nonNullParamArray(/*@ non_null @*/ int[] a) {
        return a[0];
    }

    // `new T[...]` never returns null (Java): the nullcheck discharges
    //@ ensures \result == 0;
    public int freshArrayNonNull() {
        int[] a = new int[5];
        //@ assert a != null;
        return 0;
    }

    // object-array elements are non-null by default: storing a provably non-null
    // reference passes the element-store obligation
    //@ requires arr != null && arr.length >= 1 && b != null;
    //@ ensures arr[0] == b;
    public void storeNonNullElement(Box[] arr, Box b) {
        arr[0] = b;
    }

    // `nonnullelements` states the default element nullability explicitly
    //@ requires a != null && a.length >= 1 && b != null;
    //@ ensures a[0] == b;
    public void storeElemNonNullElements(/*@ nonnullelements @*/ Box[] a, Box b) {
        a[0] = b;
    }

    // a deref of a nullable array without any guarantee: the default nullcheck
    // obligation cannot be discharged from the (empty) state
    //@ ensures true;
    public void derefNullableArray(int[] a) {
        int v = a[0];
    }

    // elements are non-null by default: storing null into an object array is
    // rejected (the element-store obligation `b != null` is falsifiable)
    //@ requires arr != null && arr.length >= 1;
    //@ ensures true;
    public void storeNullElementFalsifiable(Box[] arr) {
        arr[0] = null;
    }

    // object aliasing: `b2 = b` then a write through `b2` mutates `b.value`
    //@ requires b != null;
    //@ ensures b.value == 5;
    public void aliasObjectLocal(Box b) {
        Box b2 = b;
        b2.value = 5;
    }

    // same aliased object reached through two references; the last write wins
    //@ requires b != null;
    //@ ensures b.value == 6;
    public void aliasObjectTwice(Box b) {
        Box b1 = b;
        Box b2 = b;
        b1.value = 5;
        b2.value = 6;
    }

    // inlined callee invoked with the same array as both arguments; each increment
    // in the callee applies to the single shared array object
    //@ requires a != null && a.length >= 1 && a[0] == 1;
    //@ ensures a[0] == 3;
    public int inlineAliasedTwoArgs(int[] a) {
        bumpBoth(a, a);
        return a[0];
    }

    public void bumpBoth(int[] p, int[] q) {
        p[0] = p[0] + 1;
        q[0] = q[0] + 1;
    }

    // self-recursive call under INLINE: findDeclarationInCu skips the callable itself
    //@ requires true;
    //@ ensures true;
    public int selfRecInline(int n) {
        return selfRecInline(n - 1);
    }

    // overloaded callees: arity mismatch in findDeclarationInCu
    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int overloadCaller(int x) {
        return overloaded(x);
    }

    //@ requires x >= 0;
    //@ ensures \result >= 0;
    public int overloaded(int x) {
        return x + 1;
    }

    public int overloaded(int x, int y) {
        return x + y + 1;
    }

    // create an array with a symbolic length -> ArrayCreationExpr visit (anon name).
    // The expression in a `return` stays attached to the AST (declarator
    // initializers are cloned by the normalizer, which would detach it).
    //@ requires n >= 0 && n <= 5;
    //@ ensures \result.length == n;
    public int[] makeArray(int n) {
        return new int[n];
    }

    // a method whose return type cannot be resolved -> returnTypeOf fallback
    //@ requires true;
    //@ ensures true;
    public NoSuchType returnsUnknown(int x) {
        return null;
    }

    // local declarator whose declared type cannot be resolved -> tryResolve
    // fallback (assignment target sort defaults to int)
    //@ requires true;
    //@ ensures true;
    public void catchUnknownType(int x) {
        NoSuchType t = 42;
    }

    // inlining a callee that contains a switch: rejected by the engine, but the
    // declared-local collection still walks the switch (reached before the check)
    //@ requires true;
    //@ ensures true;
    public int callInlineSwitch(int x) {
        helperSwitch(x);
        return 0;
    }

    public void helperSwitch(int x) {
        switch (x) {
            case 1:
                x = 2;
                break;
            default:
                x = 3;
        }
    }

    // ---- implicit runtime-exception checks (opt-in via checkNull / checkCast /
    // ---- checkNegativeArraySize / checkStringIndex)

    //@ requires s != null;
    //@ ensures true;
    public int derefOk(String s) {
        return s.length();
    }

    //@ ensures true;
    public int derefUnchecked(String s) {
        return s.length();
    }

    //@ requires b != null;
    //@ ensures true;
    public int fieldDerefOk(Box b) {
        return b.value;
    }

    //@ ensures true;
    public int fieldDerefUnchecked(Box b) {
        return b.value;
    }

    //@ requires o instanceof String;
    //@ ensures true;
    public int castOk(Object o) {
        Object v = (String) o;
        return 0;
    }

    //@ ensures true;
    public int castWrongM(Object o) {
        Object v = (String) o;
        return 0;
    }

    //@ requires n >= 0;
    //@ ensures \result.length == n;
    public int[] newArrayOk(int n) {
        return new int[n];
    }

    //@ ensures true;
    public int[] newArrayNeg(int n) {
        return new int[n];
    }

    //@ requires i >= 0 && i < 2;
    //@ ensures true;
    public char charAtOk(int i) {
        return "ab".charAt(i);
    }

    //@ ensures true;
    public char charAtUnchecked(int i) {
        return "ab".charAt(i);
    }

    //@ requires i >= 0 && i <= j && j <= 2;
    //@ ensures true;
    public String subOk(int i, int j) {
        return "ab".substring(i, j);
    }

    // ---- P4.3: the abstract-string model (equals/charAt/concat/indexOf) ----

    // reflexivity and the null contract: s.equals(s) is provable and
    // s.equals(null) is false — both from the modeled extensional equality
    //@ requires s != null;
    //@ ensures true;
    public boolean stringSelfEquals(String s) {
        //@ assert s.equals(s);
        //@ assert !s.equals(null);
        return true;
    }

    // a non-negative indexOf result pinpoints a content position holding the
    // character, so s.charAt(k) (and its bounds obligation) discharges
    //@ requires s != null;
    //@ ensures true;
    public boolean stringCharAtIndexOf(String s) {
        int k = s.indexOf('x');
        if (k >= 0) {
            char c = s.charAt(k);
            //@ assert c == 'x';
            return true;
        }
        return true;
    }

    // the concatenation's runtime length is the sum of the parts, so the
    // chained length reads compose
    //@ requires s != null && t != null;
    //@ ensures \result == s.length() + t.length();
    public int stringConcatLen(String s, String t) {
        return s.concat(t).length();
    }

    // a search report of -1 means no position holds the character: the absent
    // axiom discharges a concrete charAt read against the reported index
    //@ requires s != null && s.indexOf('q') == -1;
    //@ ensures true;
    public boolean stringIndexOfAbsent(String s) {
        if (s.length() > 0) {
            char c = s.charAt(0);
            //@ assert c != 'q';
            return true;
        }
        return true;
    }

    // without a length guard even a first-position read is unprovable: the
    // bounds obligation and the content claim both stay falsifiable
    //@ requires s != null;
    //@ ensures true;
    public boolean stringCharAtUnknown(String s) {
        //@ assert s.charAt(0) == 'x';
        return true;
    }

    // ---- P4.3 extension: substring / startsWith / replace ----

    // substring's runtime length is the slice width, so suffix-length proofs
    // discharge through the strsub length axiom; the requires bounds the start
    // index so the string-index obligation also proves
    //@ requires s != null && 0 <= i && i <= s.length();
    //@ ensures \result == s.length() - i;
    public int substringLen(String s, int i) {
        return s.substring(i).length();
    }

    // the two-argument overload's length is the slice width (the bounds come
    // from the requires, checked by the string-index obligation)
    //@ requires s != null && 0 <= i && i <= j && j <= s.length();
    //@ ensures \result == j - i;
    public int substringLen2(String s, int i, int j) {
        return s.substring(i, j).length();
    }

    // a literal-startsWith claim: the prefix axiom plus the literal's bound
    // content prove the char-read consequences
    //@ requires s != null && s.length() >= 2 && s.charAt(0) == 'a' && s.charAt(1) == 'b';
    //@ ensures true;
    public boolean literalStartsWith(String s) {
        //@ assert s.startsWith("ab");
        return true;
    }

    // a broken literal-startsWith claim stays falsifiable: content alone is
    // not enough when the prefix length is longer than the receiver
    //@ requires s != null && s.length() == 1 && s.charAt(0) == 'a';
    //@ ensures true;
    public boolean literalStartsWithBroken(String s) {
        //@ assert s.startsWith("ab");
        return true;
    }

    // startsWith against a parameter: an assumed prefix yields the length bound
    //@ requires s != null && p != null && s.startsWith(p);
    //@ ensures p.length() <= s.length();
    public boolean startsWithLengthBound(String s, String p) {
        return true;
    }

    // replace(char, char) preserves the runtime length
    //@ requires s != null;
    //@ ensures \result == s.length();
    public int replaceLen(String s) {
        return s.replace('a', 'b').length();
    }

    // replace maps the replaced character: a content read at a position where
    // the receiver held 'a' reads back 'b'
    //@ requires s != null && s.length() > 0 && s.charAt(0) == 'a';
    //@ ensures \result;
    public boolean replaceContentAt0(String s) {
        char c = s.replace('a', 'b').charAt(0);
        //@ assert c == 'b';
        return true;
    }

    // indexOf(String) is a substring search, which the abstract-string model
    // does not cover (only the single-character `indexOf(int/char)` is modeled
    // as `strindex`): the call stays unrefined and is rejected by default (P1.3)
    //@ requires s != null;
    //@ ensures \result >= -1;
    public int stringIndexOfString(String s) {
        return s.indexOf("ab");
    }

    // ---- P2.1: constructor contracts at creation sites ----
    // note: named Counted (not Counter), because `Counter` is already a distinct
    // nested fixture type used by readCounter/writeReadCounter/... — a same-name
    // nested class would confuse the type solver's lookup of `VcgExamples.Counter`

    // a contracted constructor: the creation site must establish the requires,
    // may assume the ensures, and may assume the class invariant (count >= 0)
    // once construction finished
    static class Counted {
        private int count;

        //@ invariant count >= 0;

        //@ requires n >= 0;
        //@ assignable this.count;
        //@ ensures count == n;
        public Counted(int n) {
            this.count = n;
        }

        //@ assignable \nothing;
        //@ ensures \result == count;
        public int get() {
            return count;
        }
    }

    // `new Counted(5)` satisfies the constructor's requires; the caller may then
    // assume `count == 5` (ensures) through the later `get()` contract call
    //@ ensures \result == 5;
    public int useCountedCtor() {
        Counted c = new Counted(5);
        return c.get();
    }

    // the constructor's requires (n >= 0) are violated at the creation site, so
    // the precondition obligation is falsifiable
    //@ ensures true;
    public int ctorRequiresViolated() {
        Counted c = new Counted(-1);
        return c.get();
    }

    // the constructor-established invariant is usable by the caller: the contract
    // pins nothing via ensures (count is fresh/unconstrained), so the assert only
    // discharges through the assumed `\invariant_for(c)` (P2.1, step 2)
    //@ ensures \result == true;
    public boolean ctorInvariantUsable() {
        Counted c = new Counted(3);
        //@ assert c.count >= 0;
        return true;
    }

    // ---- P2.2: constant field initializers ----
    //
    // `static final` constants fold into the entry state of every callable; the
    // binder also folds arithmetic *through* the constants (BLOCK -> TWO_BLOCKS),
    // so the ensures below was previously unprovable and now discharges

    private static final int P22_BLOCK = 1024;
    private static final int P22_TWO_BLOCKS = P22_BLOCK * 2;

    //@ ensures \result == 2048;
    public int p22StaticFinalBlocks() {
        return P22_TWO_BLOCKS;
    }

    // boolean constant expressions (logical ops + comparisons on constants)
    private static final boolean P22_DEBUG = true;

    //@ ensures \result == true;
    public boolean p22StaticFinalBool() {
        //@ assert P22_DEBUG;
        return true;
    }

    // deeper constant-expression shapes: unary minus, hex digits, char literals,
    // long arithmetic and comparisons — all folded into the entry state
    private static final int P22_NEG = -7;
    private static final int P22_HEX = 0x400;
    private static final char P22_CHAR = 'z';
    private static final long P22_SMALL_LONG = 2L * 3L;
    private static final boolean P22_CMP = P22_BLOCK > 512;

    //@ ensures \result == true;
    public boolean p22ConstantShapes() {
        //@ assert P22_NEG == -7;
        //@ assert P22_HEX == 1024;
        //@ assert P22_CHAR == 'z';
        //@ assert P22_SMALL_LONG == 6;
        //@ assert P22_CMP == true;
        return true;
    }

    // non-constant initializers stay unconstrained: `p22Dynamic` is initialized by
    // a method call, which the folder refuses, so the postcondition remains
    // falsifiable exactly like before P2.2 (the field is havoced, not pinned)
    private static final int P22_DYNAMIC = p22Assign();

    private static int p22Assign() {
        return 7;
    }

    //@ ensures \result == 7;
    public int p22NonConstInit() {
        return P22_DYNAMIC;
    }

    // forward references (JLS 8.3.3 textual order) are NOT folded: when this
    // initializer runs, the later field is still at its default. The folder only
    // sees earlier-declared constants, so the field stays unconstrained (sound)
    private final int p22FwdInstance = p22LaterField;
    private final int p22LaterField = 3;

    //@ ensures \result == 3;
    public int p22ForwardRefInstance() {
        return p22FwdInstance;
    }

    // ---- P3.1: multi-dimensional array creation would be silently mis-modeled
    // ---- (only the first dimension is encoded), so it is rejected loudly ----

    public int multiDim(int n) {
        int[][] a = new int[n][n];
        return a.length;
    }

    // a jagged multi-dim array (empty trailing dimension) hits the same rejection
    public int multiDimJagged(int n) {
        int[][] a = new int[n][];
        return a.length;
    }

    // ---- P3.2: unsupported constructs raise a structured
    // ---- UnsupportedFeatureException naming the construct at its source range

    // lambdas are not modeled
    public int lambdaUse(int x) {
        java.util.function.Function<Integer, Integer> f = y -> y + 1;
        return f.apply(x);
    }

    // method references are not modeled
    public int methodRef() {
        java.util.function.IntSupplier s = VcgExamples::answer;
        return s.getAsInt();
    }

    public static int answer() {
        return 42;
    }

    // anonymous classes are not modeled
    public int anonClass() {
        Runnable r = new Runnable() {
            public void run() {
            }
        };
        return 0;
    }

    // a plain Java `assert` (with or without a message) is not a JML assert
    public int javaAssert(int x) {
        assert x >= 0 : "negative";
        return x;
    }

    // the JML \fresh keyword is not modeled
    //@ ensures \fresh(\result);
    public Object freshObj() {
        return new Object();
    }

    // the JML \reach keyword is not modeled
    //@ ensures \reach(\result).isEmpty();
    public Object reachObj() {
        return new Object();
    }

    // ---- P4.1: visible-state invariants for explicit receivers ----

    // the invariant of the *receiver's* class must hold at `o.m(...)`: here it
    // discharges through the constructor-established invariant (P2.1 assumes
    // `\invariant_for(r)` once construction finished)
    //@ ensures true;
    public void receiverInvariantHeld() {
        InvariantReceiver r = new InvariantReceiver(4);
        r.setCounter(6);
    }

    // breaking the receiver's invariant between two calls falsifies the second
    // call's pre-call check
    //@ ensures true;
    public void receiverInvariantBroken() {
        InvariantReceiver r = new InvariantReceiver(4);
        r.counter = -1;
        r.setCounter(6);
    }
}

// ---- \invariant_for fixtures: separate top-level types so the class-level
// ---- invariants only constrain the methods that are meant to observe them ----

// the base type contributes its invariants to the statically-visible set of a
// child that extends it (gathered from the context)

class InvariantExamplesBase {
    int limit;

    //@ invariant this.limit > 0;
}

class InvariantExamples extends InvariantExamplesBase {
    int counter;
    static int staticCount;

    //@ invariant counter >= 0;
    //@ invariant this.counter <= 10;
    //@ invariant staticCount >= 0;

    // `counter` is unconstrained at entry (no requires clause): the postcondition
    // is only provable because the class invariants are assumed for `this`
    //@ ensures this.counter >= 0 && this.counter <= 10;
    public int readCounterInv() {
        return this.counter;
    }

    // a method that preserves the invariants verifies
    //@ ensures this.counter == 5;
    public void setCounter5() {
        this.counter = 5;
    }

    // preserving the invariants is part of the postcondition: writing -1 violates
    // `invariant counter >= 0`
    //@ ensures true;
    public void setCounterNeg() {
        this.counter = -1;
    }

    // a `helper` method is exempt from the invariant on entry and exit
    //@ ensures true;
    //@ helper
    public void helperBreaksInvariant() {
        this.counter = -1;
    }

    // the literal \invariant_for spelling expands to the same folded conjunction
    //@ requires \invariant_for(this);
    //@ ensures \invariant_for(this);
    public int explicitInvariantFor(int x) {
        return x;
    }

    // \static_invariant_for(T.class) is recognized (a static method has no
    // enclosing instance, so the instance invariants stay satisfiable and the
    // static-field fact does flow through the folded conjunction)
    //@ requires \static_invariant_for(InvariantExamples.class);
    //@ ensures \result == staticCount + 1;
    public static int staticInvariantDoubles() {
        return staticCount + 1;
    }

    // static methods have no enclosing instance: the instance-method invariant
    // machinery is not in play, so this proves despite violating `staticCount >= 0`
    //@ ensures true;
    public static void staticBreaksCount() {
        staticCount = -1;
    }

    // ---- P4.1: visible-state class invariants at method calls ----
    // A call to a non-helper instance method is a *visible state*:
    // `\invariant_for(this)` must hold before the call (pre-call check) and may
    // be assumed again after it (post-call assume — the callee's own exit
    // discipline re-establishes it, mirrored by `checkPostcondition`).

    // a contract callee that keeps `counter` in bounds. `counter` is assignable,
    // so the post-call state stays consistent even when the callee is invoked
    // from a state that violates the invariant — the pre-call check, not a
    // post-state ex-falso, is what catches such callers (`\nothing` here would
    // make the unconditional `ensures` contradict the pre-state and poison every
    // subsequent obligation).
    //@ assignable this.counter;
    //@ ensures this.counter >= 0 && this.counter <= 10;
    public void resetCounter() {
        this.counter = 3;
    }

    // a contract callee with an uninformative postcondition: `counter` is
    // assignable, so after the call it is fresh — only the post-call assume of
    // the class invariant re-establishes its bounds
    //@ assignable this.counter;
    //@ ensures true;
    public void reassignCounter() {
        this.counter = 3;
    }

    // the invariant holds at the call site (established by the body), so the
    // pre-call check discharges and the method verifies
    //@ ensures true;
    public void callWithInvariantHeld() {
        this.counter = 3;
        this.resetCounter();
        this.counter = 5;
    }

    // the guardrail: breaking the invariant mid-method and only restoring it
    // *after* the call leaves the pre-call check falsifiable
    //@ ensures true;
    public void breakInvariantBeforeCall() {
        this.counter = -1;
        this.resetCounter();
        this.counter = 0;
    }

    // the post-call assume is load-bearing: after `reassignCounter()` the callee's
    // ensures says nothing about `counter`, yet the caller may still prove the
    // explicit assert (and the exit obligation) from the re-assumed invariant
    //@ ensures true;
    public int relyOnInvariantAfterCall() {
        this.counter = 5;
        this.reassignCounter();
        //@ assert this.counter <= 10 && this.counter >= 0;
        return this.counter;
    }

    // a `\helper` callee (P4.1): exempt from the visible-state discipline at the
    // call site, so the invariant may be broken across it and restored afterwards
    //@ assignable this.counter;
    //@ ensures true;
    //@ helper
    public void helperReassignCounter() {
        this.counter = -1;
    }

    //@ ensures true;
    public void helperCallExempt() {
        this.counter = -1;
        this.helperReassignCounter();
        this.counter = 7;
    }
}

// an inherited invariant (`this.limit > 0`) is assumed for a subclass method
class InvariantChild extends InvariantExamplesBase {
    //@ ensures this.limit > 0;
    public int readBaseInv() {
        return this.limit;
    }
}

// constructors must *establish* the class invariants: no `\invariant_for(this)`
// is assumed at entry (fields are uninitialized), but the exit obligation holds
class InvariantCtorExamples {
    int counter;

    //@ invariant this.counter >= 0;

    //@ requires c >= 0;
    //@ ensures this.counter == c;
    public InvariantCtorExamples(int c) {
        this.counter = c;
    }
}

// this constructor cannot establish the invariant, so its postcondition must fail
class InvariantCtorBad {
    int counter;

    //@ invariant this.counter >= 0;

    //@ ensures true;
    public InvariantCtorBad(int c) {
        this.counter = -1;
    }
}

// P2.2: constant instance-field initializers meet constructors. The initializer
// (`count = 0`) runs before the constructor body (JLS 12.5), so a constructor
// that never writes `count` still establishes `count == 0` and the class
// invariant purely through the folded initializer. A constructor that does write
// the field overrides the entry binding through the normal SSA store. A `final`
// instance constant (`cap`) can never be rewritten, so an instance method reads
// its initializer value even though this class has a competing constructor.
class ConstantInitCtor {
    private int count = 0;
    private final int cap = 5;

    //@ invariant count >= 0;

    //@ assignable this.count;
    //@ ensures count == 0;
    public ConstantInitCtor() {
        // intentionally empty: the constant initializer establishes the state
    }

    //@ assignable this.count;
    //@ requires n >= 0;
    //@ ensures count == n;
    public ConstantInitCtor(int n) {
        this.count = n;
    }

    // a `final` instance constant can never be rewritten, so an instance method
    // reads its initializer value even though a competing constructor exists
    //@ ensures \result == 5;
    public int capacity() {
        return cap;
    }

    // soundness boundary: `count` is *mutable* instance state, so a method entry
    // state may have been reached through a constructor that overwrote the field
    // (`constInitCtor(int)` stores `count = n`). The initializer is NOT folded
    // into methods here, so `\result == 0` stays unprovable (falsifiable).
    //@ ensures \result == 0;
    public int countAfterAnyCtor() {
        return count;
    }
}

// P3.2: verification of enum members is rejected up front — enumerations would
// need an ordinal/constant model the VCG does not have
enum Color {
    RED,
    GREEN;

    public int rgb() {
        return 3;
    }
}

// P4.2: a `non_null`-declared array field is the type invariant — assumed at
// method entry (not in constructors), so it derefs without nullchecks and the
// spec still bounds the deref explicitly.
class NonNullHolder {
    /*@ non_null @*/ int[] data;

    //@ requires data.length >= 1;
    //@ ensures data[0] == 5;
    public void storeNonNullField() {
        this.data[0] = 5;
    }
}

// P4.1: the visible-state invariant check extends to an explicit receiver: the
// statically-visible invariants of the *receiver's* class must hold at a call
// `o.m(...)` — checked before and re-assumed after, exactly like `this`.
class InvariantReceiver {
    public int counter;

    //@ invariant counter >= 0;

    //@ requires n >= 0;
    //@ assignable this.counter;
    //@ ensures this.counter == n;
    public InvariantReceiver(int n) {
        this.counter = n;
    }

    //@ requires n >= 0;
    //@ assignable this.counter;
    //@ ensures this.counter == n;
    public void setCounter(int n) {
        this.counter = n;
    }
}
