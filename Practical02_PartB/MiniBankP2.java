public class MiniBankP2 {
    static class Customer {
        private final String customerId;
        private String name, email, mobile;
        private static long customerCounter = 100;
        private static String generateCustomerId() { return "CUST" + (++customerCounter); }
        Customer(String name, String email, String mobile) {
            this.customerId = generateCustomerId(); this.name=name; this.email=email; this.mobile=mobile;
        }
        public String getCustomerId(){return customerId;} public String getName(){return name;}
        public String getEmail(){return email;} public String getMobile(){return mobile;}
    }
    static class Account {
        private final String accountNumber;
        private String ownerName;
        private long balance;
        private boolean active = true;
        private static long counter = 0;
        Account(String ownerName, long openingBalance) {
            if (openingBalance < 0) throw new IllegalArgumentException("Negative opening balance");
            this.accountNumber = String.format("AC%04d", ++counter);
            this.ownerName=ownerName; this.balance=openingBalance;
        }
        Account(String ownerName) { this(ownerName, 0); }
        public void deposit(long amount) { if(amount>0) balance+=amount; }
        public boolean withdraw(long amount) {
            if(amount<=0 || balance<amount) return false;
            balance-=amount; return true;
        }
        public String getAccountNumber(){return accountNumber;} public String getOwnerName(){return ownerName;}
        public long getBalance(){return balance;} public boolean isActive(){return active;}
    }
    public static void main(String[] args) {
        Customer c = new Customer("Riya","riya@example.com","9876543210");
        System.out.println("Customer: "+c.getCustomerId()+" "+c.getName());
        Account[] accounts = {new Account("Riya",5000),new Account("Aman"),new Account("Neha",2500)};
        accounts[0].deposit(1000); accounts[1].deposit(2000); accounts[2].withdraw(500);
        for(Account a:accounts) System.out.println(a.getAccountNumber()+" "+a.getOwnerName()+" Balance: "+a.getBalance());
    }
}
