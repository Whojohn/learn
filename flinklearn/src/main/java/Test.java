import scala.collection.Parallel;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Test implements Runnable {
    public volatile static Integer a = 0;

    public static void main(String[] args) throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1));
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(200000);
                    }
                } catch (Exception e) {
                }

            }
        };
        pool.execute(r);
        pool.submit(r);
        pool.execute(r);
        System.out.println("aaa");

    }

    @Override
    public void run() {
        synchronized (a.getClass()) {
            for (int b = 0; b < 50000000; b++) {
                a += 1;
            }
        }
    }
}


