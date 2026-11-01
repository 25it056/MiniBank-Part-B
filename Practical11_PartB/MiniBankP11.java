import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class MiniBankP11 {
    static class Account implements Serializable {
        private static final long serialVersionUID=1L;
        private final String accountNumber; private String owner; private long balance;
        Account(String no,String owner,long balance){accountNumber=no;this.owner=owner;this.balance=balance;}
        public String toString(){return accountNumber+" "+owner+" Rs."+balance;}
    }
    static class StatePersister {
        static void save(Account[] accounts,Path file)throws IOException{
            Files.createDirectories(file.getParent());
            try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(file))){out.writeObject(accounts);}
        }
        static Account[] load(Path file)throws IOException,ClassNotFoundException{
            try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(file))){return (Account[])in.readObject();}
        }
    }
    static class TransactionLog {
        static void append(Path file,String line)throws IOException{
            Files.createDirectories(file.getParent());
            Files.writeString(file,line+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }
    }
    static class ReportGenerator {
        static String generate(Path folder,Path report)throws IOException{
            long deposits=0,withdrawals=0;StringBuilder details=new StringBuilder();
            if(Files.exists(folder))try(DirectoryStream<Path> stream=Files.newDirectoryStream(folder,"*.log")){
                for(Path f:stream){
                    BasicFileAttributes a=Files.readAttributes(f,BasicFileAttributes.class);
                    details.append(f.getFileName()).append(" size=").append(a.size()).append(" modified=").append(a.lastModifiedTime()).append("\\n");
                    for(String line:Files.readAllLines(f)){
                        String[] p=line.trim().split("\\s+");
                        if(p.length>=3)try{long amount=Long.parseLong(p[2]);if(p[0].equals("DEPOSIT"))deposits+=amount;if(p[0].equals("WITHDRAW"))withdrawals+=amount;}catch(NumberFormatException ignored){}
                    }
                }
            }
            String result="Deposits: "+deposits+"\\nWithdrawals: "+withdrawals+"\\nNet: "+(deposits-withdrawals)+"\\n"+details;
            Files.writeString(report,result,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);return result;
        }
    }
    public static void main(String[] args)throws Exception{
        Path data=Path.of("data"), file=data.resolve("accounts.dat"), logs=data.resolve("logs"), report=data.resolve("report.txt");
        Account[] accounts={new Account("AC0001","Riya",5000),new Account("AC0002","Aman",3000)};
        StatePersister.save(accounts,file);
        TransactionLog.append(logs.resolve("transactions.log"),"DEPOSIT AC0001 500");
        TransactionLog.append(logs.resolve("transactions.log"),"WITHDRAW AC0002 200");
        System.out.println("Reloaded accounts:");
        for(Account a:StatePersister.load(file))System.out.println(a);
        System.out.println(ReportGenerator.generate(logs,report));
    }
}
