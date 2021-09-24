package 线程同步;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class CountdownLatchExample {

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Container container = new Container();
        ReentrantLock reentrantLock = new ReentrantLock(false);
        Produce produce = new Produce(container, reentrantLock, countDownLatch);
        Consumer consumer = new Consumer(container, reentrantLock, countDownLatch);
        Thread t1 = new Thread(produce);
        Thread t2 = new Thread(consumer);
        Thread t3 = new Thread(new Consumer(container, reentrantLock, countDownLatch));
        t1.start();

        t3.start();

        t1.join();
        t3.join();

        t2.start();
        t2.join();
        System.out.println(container.getTemp());
    }
}

class Container {
    Long temp = 0L;

    public void setTemp(Long temp) {
        this.temp = temp;
    }

    public Long getTemp() {
        return temp;
    }
}

class Consumer implements Runnable {
    private final Container container;
    private final ReentrantLock reentrantLock;
    private final CountDownLatch countDownLatch;

    Consumer(Container container, ReentrantLock reentrantLock, CountDownLatch countDownLatch) {
        this.container = container;
        this.countDownLatch = countDownLatch;
        this.reentrantLock = reentrantLock;
    }

    @Override
    public void run() {
        try {
            countDownLatch.await();
            reentrantLock.lock();
            container.setTemp(0L);
            reentrantLock.unlock();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Produce implements Runnable {
    private final Container container;
    private final ReentrantLock reentrantLock;
    private final CountDownLatch countDownLatch;

    Produce(Container container, ReentrantLock reentrantLock, CountDownLatch countDownLatch) {
        this.container = container;
        this.countDownLatch = countDownLatch;
        this.reentrantLock = reentrantLock;
    }

    @Override
    public void run() {
        reentrantLock.lock();
        for (int b = 0; b < 20; b++) {
            for (int a = 0; a < 100000000; a++) {
                container.setTemp((long) a);
            }
        }
        reentrantLock.unlock();
        countDownLatch.countDown();
    }
}
