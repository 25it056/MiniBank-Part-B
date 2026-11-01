import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MiniBankP14 {
    static class BankException extends Exception { BankException(String m){super(m);} }
    static class InvalidAmountException extends BankException { InvalidAmountException(String m){super(m);} }
    static class InsufficientFundsException extends BankException { InsufficientFundsException(String m){super(m);} }
    static class Account {
        private final String accountNumber; private final String owner; private long balance;
        Account(String no,String owner,long balance){accountNumber=no;this.owner=owner;this.balance=balance;}
        synchronized void deposit(long amount)throws InvalidAmountException{if(amount<=0)throw new InvalidAmountException("Amount must be positive");balance+=amount;}
        synchronized void withdraw(long amount)throws BankException{
            if(amount<=0)throw new InvalidAmountException("Amount must be positive");
            if(amount>balance)throw new InsufficientFundsException("Insufficient funds");balance-=amount;
        }
        synchronized long getBalance(){return balance;}
        String getAccountNumber(){return accountNumber;}
        public String toString(){return accountNumber+" "+owner+" Rs."+balance;}
    }
    static class BankService {
        private final Map<String,Account> repository=new ConcurrentHashMap<>();
        void openAccount(String id,String owner,long opening){repository.put(id,new Account(id,owner,opening));}
        Account find(String id){return repository.get(id);}
        void deposit(String id,long amount)throws BankException{
            Account a=repository.get(id);if(a==null)throw new BankException("Account not found");a.deposit(amount);
        }
        void withdraw(String id,long amount)throws BankException{
            Account a=repository.get(id);if(a==null)throw new BankException("Account not found");a.withdraw(amount);
        }
        void transfer(String from,String to,long amount)throws BankException{
            Account a=repository.get(from),b=repository.get(to);
            if(a==null||b==null)throw new BankException("Account not found");
            Account first=from.compareTo(to)<0?a:b,second=first==a?b:a;
            synchronized(first){synchronized(second){a.withdraw(amount);b.deposit(amount);}}
        }
        List<Account> listAccounts(){return new ArrayList<>(repository.values());}
        String statement(String id){Account a=repository.get(id);return a==null?"Account not found":a.toString();}
    }
    public static void main(String[] args)throws Exception{
        BankService bank=new BankService();bank.openAccount("AC0001","Riya",5000);bank.openAccount("AC0002","Aman",3000);
        try(Scanner sc=new Scanner(System.in)){
            boolean run=true;
            while(run){
                System.out.println("\\n1.Open 2.Deposit 3.Withdraw 4.Transfer 5.Statement 6.List 0.Exit");
                System.out.print("Choice: ");
                int ch;
                try{ch=Integer.parseInt(sc.nextLine());}catch(NumberFormatException e){System.out.println("Invalid choice");continue;}
                try{
                    switch(ch){
                        case 1 -> {System.out.print("Account number: ");String id=sc.nextLine();System.out.print("Owner: ");String owner=sc.nextLine();bank.openAccount(id,owner,0);System.out.println("Opened.");}
                        case 2 -> {System.out.print("Account: ");String id=sc.nextLine();System.out.print("Amount: ");bank.deposit(id,Long.parseLong(sc.nextLine()));}
                        case 3 -> {System.out.print("Account: ");String id=sc.nextLine();System.out.print("Amount: ");bank.withdraw(id,Long.parseLong(sc.nextLine()));}
                        case 4 -> {System.out.print("From: ");String f=sc.nextLine();System.out.print("To: ");String t=sc.nextLine();System.out.print("Amount: ");bank.transfer(f,t,Long.parseLong(sc.nextLine()));}
                        case 5 -> {System.out.print("Account: ");System.out.println(bank.statement(sc.nextLine()));}
                        case 6 -> bank.listAccounts().forEach(System.out::println);
                        case 0 -> run=false;
                        default -> System.out.println("Invalid choice");
                    }
                }catch(Exception e){System.out.println("Operation failed: "+e.getMessage());}
            }
        }
        System.out.println("MiniBank closed.");
    }
}
