package 线程同步;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class ThreadPool {
    public static void main(String[] args) {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(3,
                5,
                3L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        poolExecutor.submit(new Sto());
        poolExecutor.submit(new Sto());
        poolExecutor.submit(new Sto());
        poolExecutor.submit(new Sto());

        AtomicBoolean lable = new AtomicBoolean(true);
        IntStream.range(0, 100).forEach(e ->
        {
            try {
                Thread.sleep(2000);
                System.out.println(poolExecutor.getActiveCount());
                System.out.println(poolExecutor.getPoolSize());
                System.out.println(poolExecutor.getQueue().size());
                System.out.println(poolExecutor.getTaskCount());
                if (poolExecutor.getTaskCount() == 4 && lable.get()) {
                    poolExecutor.submit(new Sto());
                    poolExecutor.submit(new Sto());
                } else if (poolExecutor.getTaskCount() == 6 && lable.get()) {
                    poolExecutor.submit(new Sto());
                }
                if (e == 2) {
                    lable.set(false);
                }
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });


    }
}

class Sto implements Runnable {
    @Override
    public void run() {

        try {
            for (int a = 0; a < 10; a++) {
                for (int b = 0; b < 100000000; b++) {
                    float c = (float) Math.sqrt(Math.pow(a * ((b + 1) / 2), 1.3));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}