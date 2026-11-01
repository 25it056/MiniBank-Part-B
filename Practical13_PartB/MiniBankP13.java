import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class MiniBankP13 {
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface Id {}
    static class Account {
        @Id private final String accountNumber; private final String owner; private final long balance;
        Account(String no,String owner,long balance){accountNumber=no;this.owner=owner;this.balance=balance;}
        public String toString(){return accountNumber+" "+owner+" Rs."+balance;}
    }
    static class Customer {
        @Id private final String customerId; private final String name;
        Customer(String id,String name){customerId=id;this.name=name;}
        public String toString(){return customerId+" "+name;}
    }
    static class Repository<T> {
        private final HashMap<String,T> items=new HashMap<>();
        private String readId(T entity){
            Class<?> c=entity.getClass();
            while(c!=null){
                for(Field f:c.getDeclaredFields())if(f.isAnnotationPresent(Id.class)){
                    try{f.setAccessible(true);return String.valueOf(f.get(entity));}
                    catch(IllegalAccessException e){throw new IllegalArgumentException(e);}
                }
                c=c.getSuperclass();
            }
            throw new IllegalArgumentException("No @Id field in "+entity.getClass().getName());
        }
        T save(T entity){items.put(readId(entity),entity);return entity;}
        @SafeVarargs final void saveAll(T... entities){for(T e:entities)save(e);}
        T findById(String id){return items.get(id);}
        ArrayList<T> findAll(){return new ArrayList<>(items.values());}
        boolean delete(String id){return items.remove(id)!=null;}
        int size(){return items.size();}
    }
    static <N extends Number> double average(N[] values){
        if(values.length==0)return 0;double sum=0;for(N n:values)sum+=n.doubleValue();return sum/values.length;
    }
    public static void main(String[] args){
        Repository<Account> accounts=new Repository<>();
        accounts.saveAll(new Account("AC0001","Riya",5000),new Account("AC0002","Aman",2000),new Account("AC0003","Neha",9000));
        System.out.println("Find: "+accounts.findById("AC0002"));
        for(Account a:accounts.findAll())System.out.println(a);
        Repository<Customer> customers=new Repository<>();customers.save(new Customer("CUST101","Riya"));
        System.out.println("Generic customer repository: "+customers.findAll());
        System.out.println("Average: "+average(new Integer[]{10,20,30}));
    }
}
