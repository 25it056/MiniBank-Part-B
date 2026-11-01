import java.util.concurrent.*;

public class MiniBankP9 {
    static class Account {
        private long balance;
        Account(long balance){this.balance=balance;}
        public synchronized void deposit(long amount){balance+=amount;}
        public synchronized boolean withdraw(long amount){if(amount<=0||balance<amount)return false;balance-=amount;return true;}
        public synchronized long getBalance(){return balance;}
    }
    static class AccountWorker implements Runnable {
        private final Account account; private final int times; private final long amount;
        AccountWorker(Account a,int times,long amount){account=a;this.times=times;this.amount=amount;}
        public void run(){
            System.out.println(Thread.currentThread().getName()+" started");
            for(int i=0;i<times;i++)account.deposit(amount);
            System.out.println(Thread.currentThread().getName()+" finished");
        }
    }
    public static void main(String[] args)throws InterruptedException{
        Account account=new Account(0); Thread[] workers=new Thread[10];
        for(int i=0;i<workers.length;i++){workers[i]=new Thread(new AccountWorker(account,1000,10),"Worker-"+(i+1));workers[i].start();}
        for(Thread t:workers)t.join();
        System.out.println("Expected balance: 100000");
        System.out.println("Actual balance: "+account.getBalance());
    }
}
