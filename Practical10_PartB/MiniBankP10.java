import java.util.*;
import java.util.concurrent.*;

public class MiniBankP10 {
    static class TransactionProcessor {
        private final ExecutorService pool=Executors.newFixedThreadPool(4);
        void submit(Runnable task){pool.execute(task);}
        void stop(){
            pool.shutdown();
            try{if(!pool.awaitTermination(10,TimeUnit.SECONDS))pool.shutdownNow();}
            catch(InterruptedException e){pool.shutdownNow();Thread.currentThread().interrupt();}
        }
    }
    static class Buffer {
        private final Queue<String> queue=new ArrayDeque<>(); private final int capacity;
        Buffer(int capacity){this.capacity=capacity;}
        synchronized void put(String value)throws InterruptedException{
            while(queue.size()==capacity)wait();
            queue.add(value);System.out.println("Produced: "+value);notifyAll();
        }
        synchronized String take()throws InterruptedException{
            while(queue.isEmpty())wait();
            String v=queue.remove();System.out.println("Processed: "+v);notifyAll();return v;
        }
    }
    static class BankAccount {
        final String accountNumber; long balance;
        BankAccount(String no,long bal){accountNumber=no;balance=bal;}
        static void safeTransfer(BankAccount a,BankAccount b,long amount){
            BankAccount first=a.accountNumber.compareTo(b.accountNumber)<0?a:b;
            BankAccount second=first==a?b:a;
            synchronized(first){synchronized(second){
                if(a.balance>=amount){a.balance-=amount;b.balance+=amount;}
            }}
        }
    }
    public static void main(String[] args)throws Exception{
        long[] balance={0}; TransactionProcessor p=new TransactionProcessor();
        for(int i=0;i<20;i++)p.submit(()->{synchronized(balance){balance[0]+=100;}});
        p.stop();System.out.println("Batch balance: "+balance[0]);
        Buffer buffer=new Buffer(3);
        Thread producer=new Thread(()->{try{for(int i=1;i<=5;i++)buffer.put("TX"+i);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        Thread consumer=new Thread(()->{try{for(int i=1;i<=5;i++)buffer.take();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        producer.start();consumer.start();producer.join();consumer.join();
        BankAccount a=new BankAccount("AC0001",1000),b=new BankAccount("AC0002",500);
        Thread t1=new Thread(()->BankAccount.safeTransfer(a,b,100));
        Thread t2=new Thread(()->BankAccount.safeTransfer(b,a,50));
        t1.start();t2.start();t1.join();t2.join();
        System.out.println("A="+a.balance+" B="+b.balance);
    }
}
