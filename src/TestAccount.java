public class TestAccount {
    private static void printAccountInfo(Account acc) {
        System.out.println("Account #" + acc.getAccountNumber() +
            " | " + acc.getName() +
            " (" + acc.getAge() + " yrs)" +
            " | " + acc.getAccountType() +
            " | ₹" + acc.getBalance() +
            " | " + acc.getStatus() +
            " | PIN:" + ((acc.hasPin())?"Yes":"No"));
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("ENHANCED ACCOUNT TEST (BOOLEAN RETURNS)");
        System.out.println("=".repeat(50));
        System.out.println();
        System.out.println("\n>>> Test 1: Valid Account Creation");
        Account acc1 = new Account(1001, "John Doe", 25, 1000.0, "Savings");
        printAccountInfo(acc1);
        System.out.println();
        System.out.println(">>> Test 2: Invalid Age (under 18)");
        System.out.println("Creating account with age 16");
        Account acc2=new Account(1002,"Young Kid",16,500,"Savings");
        System.out.println("Age auto-corrected to: " + acc2.getAge());
        printAccountInfo(acc2);
        System.out.println();
        System.out.println(">>> Test 3: Invalid Account Type");
        System.out.println("Creating account with type \"Invalid\"");
        System.out.println("Account type defaulted to: Savings");
        Account acc3=new Account(1003,"Test User",25,500,"Invalid");
        printAccountInfo(acc3);
        System.out.println();
        System.out.println(">>> Test 4: Minimum Balance Enforcement on Creation");
        System.out.println("Creating Savings account with $300 (below minimum)");
        Account acc4=new Account(1004,"Bob Wilson",25,300,"Invalid");
        System.out.println("Balance auto-corrected to minimum:"+acc4.getBalance());
        printAccountInfo(acc4);
        System.out.println();
        System.out.println(">>> Test 5: Withdrawal with Minimum Balance");
        Account acc5 =new Account(1005, "Alice Brown", 30, 1000, "Current");
        acc5.setPin(2590);
        System.out.print("Initials:");
        printAccountInfo(acc5);
        boolean success=acc5.withdraw(200, 2590);
        System.out.println("Withdrawing ₹200.0:"+((success)?"Success":"Failed (Minimum balance violation)"));
        System.out.println("New balance:"+acc5.getBalance());
        System.out.print("After withdrawal:");
        printAccountInfo(acc5);
        success=acc5.withdraw(900, 2590);
        System.out.println("Withdrawing ₹900.0:"+((success)?"Success":"Failed (Minimum balance violation)"));
        System.out.println("Current balance:"+acc5.getBalance());
        System.out.println(">>> Test 6: Account Status Management");
        Account acc6=new Account(1006, "Charlie Green", 35, 2000, "Savings");
        System.out.print("Initials:");
        printAccountInfo(acc6);
        success=acc6.closeAccount();
        System.out.println("Closing Account:"+((success)?"SUCCESS":"FAILED"));
        success=acc6.deposit(500);
        System.out.println("Depositing ₹500.0 to closed account:"+((success)?"SUCCESS":"FAILED (Account inactive)"));
        success=acc6.reopenAccount();
        System.out.println("Reopening account:"+((success)?"SUCCESS":"FAILED"));
        System.out.print("After reopen:");
        printAccountInfo(acc6);
        System.out.println();
        System.out.println(">>> Test 7: PIN Protection");
        Account acc7=new Account(1007, "Diana Prince", 27, 1500, "Savings");
        System.out.printf("Setting PIN 1234: %s%n", acc7.setPin(1234) ? "SUCCESS" : "FAILED");

        System.out.printf("Withdrawing $200.0 with correct PIN (1234): %s%n",
                acc7.withdraw(200, 1234) ? "SUCCESS" : "FAILED");
        System.out.printf("New balance: $%.1f%n", (double) acc7.getBalance());

        System.out.printf("Withdrawing $100.0 with incorrect PIN (9999): %s (Incorrect PIN)%n",
                acc7.withdraw(100, 9999) ? "SUCCESS" : "FAILED");

        Account acc8 = new Account(1008, "Eve Wilson", 22, 800, "SAVINGS");
        System.out.printf("Withdrawing $100.0 with PIN not set: %s (PIN not set)%n",
                acc8.withdraw(100, 0) ? "SUCCESS" : "FAILED");
        System.out.println(">>> Test 8: All Accounts Summary");
        printAccountInfo(acc1);
        printAccountInfo(acc2);
        printAccountInfo(acc3);
        printAccountInfo(acc4);
        printAccountInfo(acc5);
        printAccountInfo(acc6);
        printAccountInfo(acc7);
        System.out.println("=".repeat(50));
        System.out.println("ENHANCED TEST COMPLETED!");
        System.out.println("=".repeat(50));
    }
}

