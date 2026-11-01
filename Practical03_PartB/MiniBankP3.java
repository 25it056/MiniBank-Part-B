import java.util.Objects;

public class MiniBankP3 {
    static class Customer implements Cloneable {
        private final String customerId; private String name;
        private Address address;
        Customer(String id,String name,Address address){this.customerId=id;this.name=name;this.address=address;}
        public Address getAddress(){return address;}
        @Override public Customer clone() {
            try { Customer c=(Customer)super.clone(); c.address=new Address(address.line,address.city,address.pincode); return c; }
            catch(CloneNotSupportedException e){throw new AssertionError(e);}
        }
        static class Address {
            private String line,city,pincode;
            Address(String line,String city,String pincode){this.line=line;this.city=city;this.pincode=pincode;}
            public String getLine(){return line;} public String getCity(){return city;} public String getPincode(){return pincode;}
        }
    }
    static class Account {
        private final String accountNumber; private String ownerName; private long balance; private boolean active=true;
        Account(String no,String owner,long balance){accountNumber=no;ownerName=owner;this.balance=balance;}
        @Override public String toString(){return accountNumber+" | "+ownerName+" | Balance: "+balance+" | Active: "+active;}
        @Override public boolean equals(Object o){return o instanceof Account a && accountNumber.equals(a.accountNumber);}
        @Override public int hashCode(){return Objects.hash(accountNumber);}
    }
    public static void main(String[] args){
        Account a=new Account("AC0001","Riya",5000), b=new Account("AC0001","Other",100);
        System.out.println(a); System.out.println("Accounts equal? "+a.equals(b));
        Object obj=a; System.out.println("Is Account? "+(obj instanceof Account));
        Customer c=new Customer("CUST101","Riya",new Customer.Address("12 Main Road","Jamnagar","361001"));
        Customer copy=c.clone(); System.out.println("Address city: "+copy.getAddress().getCity());
    }
}
