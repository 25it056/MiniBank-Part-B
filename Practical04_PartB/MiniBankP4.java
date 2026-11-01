import java.util.regex.Pattern;

public class MiniBankP4 {
    static class Account {
        private final String accountNumber; private final String ownerName; private final long balance;
        Account(String no,String owner,long balance){accountNumber=no;ownerName=owner;this.balance=balance;}
        public String getAccountNumber(){return accountNumber;} public String getOwnerName(){return ownerName;}
        public long getBalance(){return balance;}
    }
    enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER }
    record Command(TransactionType type,String accountNumber,long amount) {}
    static class Validator {
        static boolean isValidMobile(String s){return Pattern.matches("[6-9][0-9]{9}",s);}
        static boolean isValidEmail(String s){return Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",s);}
        static boolean isValidPan(String s){return Pattern.matches("[A-Z]{5}[0-9]{4}[A-Z]",s);}
        static boolean isValidIfsc(String s){return Pattern.matches("[A-Z]{4}0[A-Z0-9]{6}",s);}
        static boolean isValidAmount(String s){return s.matches("[1-9][0-9]*");}
    }
    static class CommandParser {
        static Command parse(String line){
            String[] p=line.trim().split("\\s+");
            if(p.length!=3) throw new IllegalArgumentException("Use: TYPE ACCOUNT AMOUNT");
            TransactionType type=TransactionType.valueOf(p[0].toUpperCase());
            long amount=Long.parseLong(p[2]);
            if(amount<=0) throw new IllegalArgumentException("Amount must be positive");
            return new Command(type,p[1],amount);
        }
    }
    static class StatementFormatter {
        static String buildStatement(Account a){
            return new StringBuilder("----- ACCOUNT STATEMENT -----\\n")
                .append("Account: ").append(a.getAccountNumber()).append("\\n")
                .append("Owner: ").append(a.getOwnerName()).append("\\n")
                .append("Balance: Rs. ").append(a.getBalance()).append("\\n").toString();
        }
    }
    public static void main(String[] args){
        System.out.println("Mobile valid: "+Validator.isValidMobile("9876543210"));
        System.out.println("PAN valid: "+Validator.isValidPan("ABCDE1234F"));
        Command c=CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println(c.type()+" "+c.accountNumber()+" "+c.amount());
        System.out.println(StatementFormatter.buildStatement(new Account("AC0001","Riya",5500)));
    }
}
