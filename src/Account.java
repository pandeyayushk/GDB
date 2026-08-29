public class Account {
    private int accountNumber;
    private String name;
    private int age;
    private double balance;
    private String accountType;
    private String status;
    private Integer pin;
    static private final int MIN_AGE=18;
    static private final String Default_Account="SAVINGS";
    static private final double Savings_MIN_BALANCE=500.0;
    static private final double Current_MIN_BALANCE=1000.0;  
    static private final int MIN_BALANCE=200;

    Account(int accountNumber,String name,int age,double initialBalance,String accountType){
        this.accountNumber=accountNumber;
        this.name=name;
        this.age=(age>=18)?age:MIN_AGE;
        this.accountType=(accountType.equalsIgnoreCase("Savings")||accountType.equalsIgnoreCase("CURRENT"))?accountType:Default_Account;
        if(this.accountType.equalsIgnoreCase("SAVINGS")){
            balance=(initialBalance>=500)?initialBalance:Savings_MIN_BALANCE;
        }else{
            balance=(initialBalance>=1000)?initialBalance:Current_MIN_BALANCE;
        }
        this.status="ACTIVE";
    }
    boolean deposit(double amount){
        if (amount > 0 && this.status.equalsIgnoreCase("ACTIVE")){
            balance += amount;
            return true;
        }
    return false;
    }
    boolean withdraw(double amount,int pin){
        if (amount > 0 && balance - amount >= MIN_BALANCE && this.status.equalsIgnoreCase("ACTIVE")&&verifyPin(pin)) {
            balance -= amount;
            return true;
        }
    return false;
    }
    int getAccountNumber(){
        return accountNumber;
    }
    String getName(){
        return name;
    }
    int getAge(){
        return age;
    }
    double getBalance(){
        return balance;
    }
    String getAccountType(){
        return accountType;
    }
    String getStatus(){
        return status;
    }
    void setName(String name){
        this.name=name;
    }
    void setAge(int age){
        this.age=age;
    }
    boolean closeAccount(){
        if(this.status.equalsIgnoreCase("ACTIVE")){
            this.status="INACTIVE";
            return true;
        }else{
            return false;
        }
    }
    boolean reopenAccount() {
        if (this.status.equalsIgnoreCase("INACTIVE")) {
            this.status="ACTIVE";
            return true;
        }
        return false;
    }

    boolean setPin(int pin){
        this.pin=(pin>=1000&&pin<=9999)?pin:null;
        return this.pin!=null;
    }
    boolean verifyPin(int pin){
        return this.pin!=null && this.pin==pin;
    }
    boolean hasPin(){
        return this.pin!=null;
    }
}
