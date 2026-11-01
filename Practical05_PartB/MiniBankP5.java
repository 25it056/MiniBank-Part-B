public class MiniBankP5 {
    static abstract class Account {
        private final String accountNumber; protected String ownerName; protected long balance;
        Account(String no,String owner,long balance){accountNumber=no;ownerName=owner;this.balance=balance;}
        public abstract double interestRate();
        public abstract boolean canWithdraw(long amount);
        public boolean withdraw(long amount){if(amount<=0||!canWithdraw(amount))return false;balance-=amount;return true;}
        public void deposit(long amount){if(amount>0)balance+=amount;}
        public String getAccountNumber(){return accountNumber;} public long getBalance(){return balance;}
    }
    static class SavingsAccount extends Account {
        private long minBalance;
        SavingsAccount(String no,String owner,long bal,long min){super(no,owner,bal);minBalance=min;}
        @Override public double interestRate(){return 4.0;}
        @Override public boolean canWithdraw(long amount){return amount>0 && balance-amount>=minBalance;}
    }
    static class CurrentAccount extends Account {
        private long overdraftLimit;
        CurrentAccount(String no,String owner,long bal,long limit){super(no,owner,bal);overdraftLimit=limit;}
        @Override public double interestRate(){return 0.0;}
        @Override public boolean canWithdraw(long amount){return amount>0 && balance-amount>=-overdraftLimit;}
    }
    static class FixedDepositAccount extends Account {
        FixedDepositAccount(String no,String owner,long bal){super(no,owner,bal);}
        @Override public double interestRate(){return 7.0;}
        @Override public boolean canWithdraw(long amount){return false;}
    }
    public static void main(String[] args){
        Account[] accounts={new SavingsAccount("AC1","Riya",10000,1000),
            new CurrentAccount("AC2","Aman",2000,5000),new FixedDepositAccount("AC3","Neha",20000)};
        for(Account a:accounts){
            System.out.println(a.getAccountNumber()+" rate="+a.interestRate()+"%");
            if(a instanceof SavingsAccount s) System.out.println("Savings minimum balance: "+s.getBalance());
        }
    }
}
