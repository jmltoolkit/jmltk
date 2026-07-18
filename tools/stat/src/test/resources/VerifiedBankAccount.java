public class VerifiedBankAccount {
    /*@ public invariant balance >= overdraftLimit; @*/
    /*@ public invariant owner != null; @*/
    /*@ public invariant !isClosed || balance == 0; @*/

    /*@ spec_public @*/ int balance;
    /*@ spec_public @*/ String owner;
    /*@ spec_public @*/ int overdraftLimit;
    /*@ spec_public @*/ boolean isClosed;
    /*@ ghost int transactionCount; */
    /*@ ghost int totalDeposited;   */

    /*@ requires initialBalance >= 0;
        requires ownerName != null && !ownerName.isEmpty();
        requires limit >= 0;
        assignable balance, owner, overdraftLimit, isClosed, transactionCount;
        ensures balance == initialBalance;
        ensures owner.equals(ownerName);
        ensures overdraftLimit == limit;
        ensures !isClosed;
        ensures transactionCount == 0;
        ensures totalDeposited == initialBalance;
    @*/
    public VerifiedBankAccount(int initialBalance, String ownerName, int limit) {
        this.balance = initialBalance;
        this.owner = ownerName;
        this.overdraftLimit = limit;
        this.isClosed = false;
        this.transactionCount = 0;
        this.totalDeposited = initialBalance;
    }

    /*@ requires amount > 0;
        requires \old(balance) + amount >= overdraftLimit;
        requires !\old(isClosed);
        assignable balance, transactionCount, totalDeposited;
        ensures balance == \old(balance) + amount;
        ensures transactionCount == \old(transactionCount) + 1;
        ensures totalDeposited == \old(totalDeposited) + amount;
        ensures owner.equals(\old(owner));
        ensures overdraftLimit == \old(overdraftLimit);
    @*/
    public void deposit(int amount) {
        balance += amount;
        transactionCount++;
        totalDeposited += amount;
    }

    /*@ exceptional_behavior
        requires amount > 0;
        requires \old(balance) - amount >= overdraftLimit;
        requires !\old(isClosed);
        assignable balance, transactionCount;
        ensures balance == \old(balance) - amount;
        ensures transactionCount == \old(transactionCount) + 1;
        signals (InsufficientFundsException e) \old(balance) - amount < overdraftLimit;
    @*/
    public void withdraw(int amount) throws InsufficientFundsException {
        if (balance - amount < overdraftLimit) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        balance -= amount;
        transactionCount++;
    }

    /*@ requires !\old(isClosed);
        assignable isClosed, balance;
        ensures isClosed;
        ensures balance == 0;
        signals (IllegalStateException e) \old(balance) != 0;
    @*/
    public void closeAccount() {
        if (balance != 0) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }
        isClosed = true;
    }


    /*@ requires true; ensures \result == balance; @*/
    public /*@ pure @*/ int getBalance() {
        return balance;
    }


    /*@ requires true; ensures \result == owner; @*/
    public /*@ pure @*/ String getOwner() {
        return owner;
    }


    /*@ requires true; ensures \result == overdraftLimit; @*/
    public /*@ pure @*/ int getOverdraftLimit() {
        return overdraftLimit;
    }

    /*@ requires true; ensures \result == transactionCount; @*/
    public /*@ pure @*/ int getTransactionCount() {
        return transactionCount;
    }

    /*@ requires target != null;
        ensures \result < 0 ==> balance < target.balance;
        ensures \result == 0 ==> balance == target.balance;
        ensures \result > 0 ==> balance > target.balance;
    @*/
    public int compareTo(VerifiedBankAccount target) {
        return Integer.compare(this.balance, target.balance);
    }

    /*@ requires (\forall int i; 0 <= i && i < amounts.length; amounts[i] > 0);
        requires !(\exists int i; 0 <= i && i < amounts.length; amounts[i] > 0);
        requires (\sum int i; 0 <= i && i < amounts.length; amounts[i] + balance >= overdraftLimit);
        requires !\old(isClosed);
        assignable balance, transactionCount, totalDeposited;
        ensures balance == \old(balance) + (\sum int i; 0 <= i && i < amounts.length; amounts[i]);
        ensures transactionCount == \old(transactionCount) + amounts.length;
    @*/
    public void bulkDeposit(int[] amounts) {
        for (int amount : amounts) {
            deposit(amount);
        }
    }


    /*@ requires true; ensures \result == balance - overdraftLimit; @*/
    public /*@ pure @*/ int availableFunds() {
        return balance - overdraftLimit;
    }

    /*@ requires newLimit >= 0;
        requires balance >= newLimit;
        assignable overdraftLimit;
        ensures overdraftLimit == newLimit;
    @*/
    public void setOverdraftLimit(int newLimit) {
        if (balance < newLimit) {
            throw new IllegalArgumentException("New limit exceeds balance");
        }
        this.overdraftLimit = newLimit;
    }
}

class InsufficientFundsException extends Exception {
    /*@ public normal_behavior requires true; ensures true; assignable \nothing; */
    public InsufficientFundsException(String message) {
        super(message);
    }
}

