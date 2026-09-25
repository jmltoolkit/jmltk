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
}
