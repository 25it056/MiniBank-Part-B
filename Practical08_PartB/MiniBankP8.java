public class MiniBankP8 {
    static class BankException extends Exception { BankException(String m){super(m);} }
    static class InsufficientFundsException extends BankException {
        private final long shortfall;
        InsufficientFundsException(long shortfall){super("Insufficient funds. Shortfall: "+shortfall);this.shortfall=shortfall;}
        long getShortfall(){return shortfall;}
    }
    static class InvalidAmountException extends BankException { InvalidAmountException(String m){super(m);} }
    static class AccountNotFoundException extends BankException { AccountNotFoundException(String m){super(m);} }
    static class Account {
        private final String accountNumber; private long balance;
        Account(String no,long balance){accountNumber=no;this.balance=balance;}
        void deposit(long amount)throws InvalidAmountException{if(amount<=0)throw new InvalidAmountException("Deposit must be positive");balance+=amount;}
        void withdraw(long amount)throws InsufficientFundsException,InvalidAmountException{
            if(amount<=0)throw new InvalidAmountException("Withdrawal must be positive");
            if(amount>balance)throw new InsufficientFundsException(amount-balance); balance-=amount;
        }
        void transfer(Account to,long amount)throws BankException{
            try{withdraw(amount);to.deposit(amount);System.out.println("Transfer successful");}
            catch(BankException e){throw e;}
            finally{System.out.println("Transfer operation finished");}
        }
        long getBalance(){return balance;}
    }
    static class DemoResource implements AutoCloseable {
        public void use(){System.out.println("Resource in use");}
        public void close(){System.out.println("Resource closed");}
    }
    public static void main(String[] args){
        Account a=new Account("AC1",1000),b=new Account("AC2",200);
        try{a.transfer(b,1500);}
        catch(InsufficientFundsException e){System.out.println(e.getMessage());}
        catch(BankException e){System.out.println("Bank error: "+e.getMessage());}
        try(DemoResource r=new DemoResource()){r.use();}
    }
}
