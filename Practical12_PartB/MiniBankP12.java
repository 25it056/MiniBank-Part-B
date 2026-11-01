import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniBankP12 {
    static class Account implements Comparable<Account> {
        private final String accountNumber; private final String owner; private long balance;
        Account(String no,String owner,long balance){accountNumber=no;this.owner=owner;this.balance=balance;}
        public String getAccountNumber(){return accountNumber;} public long getBalance(){return balance;}
        public int compareTo(Account other){return accountNumber.compareTo(other.accountNumber);}
        public String toString(){return accountNumber+" "+owner+" Rs."+balance;}
    }
    static class AccountStore {
        private final HashMap<String,Account> accounts=new HashMap<>();
        private final ArrayList<String> history=new ArrayList<>();
        void add(Account a){accounts.put(a.getAccountNumber(),a);history.add("ADDED "+a.getAccountNumber());}
        Account get(String id){return accounts.get(id);}
        ArrayList<Account> getAll(){return new ArrayList<>(accounts.values());}
        ArrayList<Account> sortedByBalance(){
            ArrayList<Account> list=getAll();
            list.sort(Comparator.comparingLong(Account::getBalance).reversed());return list;
        }
        ArrayList<String> getHistory(){return new ArrayList<>(history);}
    }
    public static void main(String[] args){
        AccountStore store=new AccountStore();
        store.add(new Account("AC0001","Riya",5000));store.add(new Account("AC0002","Aman",2000));store.add(new Account("AC0003","Neha",9000));
        System.out.println("Lookup: "+store.get("AC0002"));
        System.out.println("Sorted by balance:");
        for(Account a:store.sortedByBalance())System.out.println(a);
        ConcurrentHashMap<String,Account> concurrent=new ConcurrentHashMap<>();
        for(Account a:store.getAll())concurrent.put(a.getAccountNumber(),a);
        System.out.println("Thread-safe map size: "+concurrent.size());
        System.out.println("History: "+store.getHistory());
    }
}
