public class MiniBankP6 {
    interface Transactable { void deposit(long amount); boolean withdraw(long amount); }
    interface InterestBearing {
        double interestRate();
        long getBalance();
        default double yearlyInterest(){return getBalance()*interestRate()/100.0;}
    }
    @FunctionalInterface interface WithdrawRule { boolean allow(Account account,long amount); }
    interface Premium {}
    static class Account implements Transactable,InterestBearing {
        private final String accountNumber; private long balance;
        Account(String no,long balance){accountNumber=no;this.balance=balance;}
        public void deposit(long amount){if(amount>0)balance+=amount;}
        public boolean withdraw(long amount){if(amount<=0||amount>balance)return false;balance-=amount;return true;}
        public double interestRate(){return 4.0;} public long getBalance(){return balance;}
        public String getAccountNumber(){return accountNumber;}
    }
    public static void main(String[] args){
        Account a=new Account("AC0001",5000);
        WithdrawRule anonymous=new WithdrawRule(){public boolean allow(Account x,long n){return n>0&&n<=x.getBalance();}};
        WithdrawRule lambda=(x,n)->n>0&&n<=x.getBalance();
        System.out.println("Anonymous rule: "+anonymous.allow(a,1000));
        System.out.println("Lambda rule: "+lambda.allow(a,1000));
        System.out.println("Yearly interest: "+a.yearlyInterest());
        a.deposit(500); System.out.println("Balance: "+a.getBalance());
    }
}
