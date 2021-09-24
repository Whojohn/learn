package 线程同步;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SynchronizedDemo implements Runnable {
    private long temp = 0;
    private final ReentrantLock lock = new ReentrantLock(false);

    public long getTemp() {
        return temp;
    }

    /**
     * 2个线程下不同实例对象， temp 输出并不是 200000 , 证明 synchronized 变量修饰类非静态方法时，只锁定一个实例对象的
     */
    public void addSyncOnObjectInstance() {
        lock.lock();
        try {
            for (int loop = 0; loop < 50000; loop++) {
                if (!Thread.interrupted()) {
                    for (int a = 0; a < 3000000; a++) {
                        temp += 1;
                    }
                    for (int a = 0; a < 3000000; a++) {
                        temp -= 1;
                    }
                } else {
                    System.out.println("get interrupted signal");
                    return;
                }
            }
        } finally {
            lock.unlock();
        }

    }


    public static void main(String[] args) throws InterruptedException {

        Long start = System.currentTimeMillis();

        SynchronizedDemo s = new SynchronizedDemo();
        Thread t1 = new Thread(s);
        Thread t2 = new Thread(s);
        Thread t3 = new Thread(s);
        Thread t4 = new Thread(s);
        Thread t5 = new Thread(s);
        Thread t6 = new Thread(s);


        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();


        t1.join();
        t2.join();
        t6.interrupt();
        t3.join();
        t4.join();
        t5.join();
        t6.join();

        System.out.println(s.getTemp());

        System.out.println(System.currentTimeMillis() - start);

    }

    public void run() {
        this.addSyncOnObjectInstance();
    }
}
