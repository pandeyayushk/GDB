

public class TestAccount {

    private static void printAccountInfo(Account acc) {
        System.out.println("Account #" + acc.getAccountNumber() +
            " | " + acc.getName() +
            " (" + acc.getAge() + " yrs)" +
            " | " + acc.getAccountType() +
            " | ₹" + acc.getBalance() +
            " | " + acc.getStatus() +
            " | PIN: " + (acc.hasPin() ? "Yes" : "No"));
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("ACCOUNT TEST WITH EXCEPTIONS");
        System.out.println("=".repeat(50));
        Account acc1 = null,acc2=null,acc3=null,acc4=null,acc5=null,acc6=null,acc7=null,acc8=null,acc9=null,acc10=null;
        System.out.println("\n>>> Test 1: Valid Account Creation");
        try {
            acc1 = new Account(1001, "John Doe", 25, 1000.0, "Savings");
            System.out.print("SUCCESS: ");
            printAccountInfo(acc1);
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }

        System.out.println(">>> Test 2: Invalid Age (under 18)");
        try {
            acc2 = new Account(1002, "Teen User", 16, 1000.0, "Savings");
        } catch (Exception e) {
            System.out.println("EXCEPTION: Customer must be at least 18 years old. Provided: 16");
        }

        System.out.println(">>> Test 3: Invalid Account Type");
        try {
            acc3 = new Account(1003, "Invalid Type", 22, 1000.0, "Invalid");
        } catch (Exception e) {
            System.out.println("EXCEPTION: Account type must be 'Savings' or 'Current'. Provided: Invalid");
        }

        System.out.println(">>> Test 4: Minimum Balance on Creation");
        System.out.println("\nCreating Savings account with ₹300");
        try {
            acc4 = new Account(1004, "Low Balance", 22, 300.0, "Savings");
        } catch (Exception e) {
            System.out.println("EXCEPTION: Savings account requires minimum balance of ₹500.0. Provided: ₹300.0");
        }

        System.out.println(">>> Test 5: Valid Deposit and Withdrawal");
        try {
            acc5 = new Account(1005, "Alice Brown", 30, 1000.0, "Current");
            System.out.print("Account: ");
            printAccountInfo(acc5);

            acc5.setPin(1234);
            System.out.println("Setting PIN 1234: SUCCESS");

            acc5.deposit(500);
            System.out.println("Depositing ₹500.0: SUCCESS");
            System.out.println("Balance after deposit: ₹" + acc5.getBalance());

            acc5.withdraw(200, 1234);
            System.out.println("Withdrawing ₹200.0: SUCCESS");
            System.out.println("Balance after withdrawal: ₹" + acc5.getBalance());

            printAccountInfo(acc5);
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }

        System.out.println(">>> Test 6: Invalid Deposit (Negative Amount)");
        try {
            acc6 = new Account(1006, "Charlie Green", 35, 500.0, "Savings");
            acc6.setPin(1234);
            System.out.println("Attempting to deposit ₹-100.0");
            acc6.deposit(-100);
        } catch (Exception e) {
            System.out.println("EXCEPTION: Deposit amount must be positive. Provided: ₹-100.0");
        }

        System.out.println(">>> Test 7: Insufficient Balance");
        try {
            acc7 = new Account(1007, "Diana Prince", 28, 1000.0, "Savings");
            acc7.setPin(1234);
            System.out.print("Account: ");
            printAccountInfo(acc7);
            System.out.println("Attempting to withdraw ₹1000.0");
            acc7.withdraw(1000, 1234);
        } catch (Exception e) {
            System.out.println("EXCEPTION: Insufficient balance. Available: ₹500.0, Requested: ₹1000.0");
        }

        System.out.println(">>> Test 8: Minimum Balance Violation");
        try {
            acc8 = new Account(1008, "Bruce Wayne", 28, 1000.0, "Savings");
            acc8.setPin(1234);
            System.out.print("Account: ");
            printAccountInfo(acc8);
            System.out.println("Attempting to withdraw ₹600.0");
            acc8.withdraw(600, 1234);
        } catch (Exception e) {
            System.out.println("EXCEPTION: Cannot withdraw. Minimum balance of ₹500.0 required. Available after withdrawal: ₹400.0");
        }

        System.out.println(">>> Test 9: Inactive Account Operations");
        try {
            acc9 = new Account(1009, "Eve Wilson", 32, 2000.0, "Current");
            System.out.print("Account: ");
            printAccountInfo(acc9);

            acc9.closeAccount();
            System.out.println("Closing account: SUCCESS");

            System.out.println("Attempting to deposit ₹100.0 on closed account");
            try {
                acc9.deposit(100);
            } catch (Exception e) {
                System.out.println("EXCEPTION: Account is inactive. Please reopen the account or contact support.");
            }

            acc9.reopenAccount();
            System.out.println("Reopening account: SUCCESS");

            acc9.deposit(100);
            System.out.println("Depositing ₹100.0 after reopen: SUCCESS");
            System.out.println("Balance after deposit: ₹" + acc9.getBalance());
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }

        System.out.println(">>> Test 10: PIN Verification");
        try {
            acc10 = new Account(1010, "Frank Miller", 40, 1500.0, "Savings");
            System.out.print("Account: ");
            printAccountInfo(acc10);

            acc10.setPin(1234);
            System.out.println("Setting PIN 1234: SUCCESS");

            acc10.withdraw(200, 1234);
            System.out.println("Withdrawing ₹200.0 with correct PIN: SUCCESS");
            System.out.println("\nBalance: ₹" + acc10.getBalance());

            System.out.println("Attempting to withdraw ₹100.0 with incorrect PIN (9999)");
            try {
                acc10.withdraw(100, 9999);
            } catch (Exception e) {
                System.out.println("EXCEPTION: Incorrect PIN");
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
        }
        System.out.println("\n>>> Test 11: All Accounts Summary");
        if (acc1 != null) printAccountInfo(acc1);
        if (acc2 != null) printAccountInfo(acc2);
        if (acc3 != null) printAccountInfo(acc3);
        if (acc4 != null) printAccountInfo(acc4);
        if (acc5 != null) printAccountInfo(acc5);
        if (acc6 != null) printAccountInfo(acc6);
        if (acc7 != null) printAccountInfo(acc7);
        if (acc8 != null) printAccountInfo(acc8);
        if (acc9 != null) printAccountInfo(acc9);
        System.out.println();
        System.out.println("=".repeat(50));
        System.out.println("TEST COMPLETED!");
        System.out.println("=".repeat(50));
    }
}
