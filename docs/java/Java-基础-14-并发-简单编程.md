# Java-基础-14-并发-简单编程

## 1. 线程的创建

### 1.1 线程创建方式

- 继承 Thread 类
- 实现 Runnable 接口
- 实现 Callable 接口

### 1.1.1 继承Thread类

- 注意事项

1. 单个Thread 异常不会引发其他Thread异常。处理逻辑需要重写run 函数实现，run 函数父类没有异常捕获，因此不能主动抛出非 RuntimeException 异常。
2. 需要传参必须通过构造函数进行传递。
3. 没有任何返回值，假如需要返回值需要传入特定容器，对容器进行操作。
4. 由于是继承类，java 不支持类的多继承，不利于扩展。

- demo

```
import java.util.ArrayList;
import java.util.List;

public class ThreadClassTest extends Thread {

    private final List<Integer > source;

    ThreadClassTest(List<Integer> source) {
        super("test");
        this.source = source;
    }

    @Override
    public void run() {
        /**
         *  需要重写Thread 的方法以实现逻辑，假如需要传参，必须通过构造函数传参;
         *  线程间独立，单个线程异常退出不会导致其他线程异常退出。
         */
        if (this.source.size() < 2) {
            int a = 1;
            int b = 0;
            b = a / b;
        }

        source.forEach(e -> {
            try {
                Thread.sleep(500);
                System.out.println(e);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });
        source.set(0,999);

    }

    public static void main(String[] args) throws InterruptedException {
        List<Integer> temp = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
            add(4);
        }};
        Thread t1 = new ThreadClassTest(temp.subList(0, 1));
        Thread t2 = new ThreadClassTest(temp);
        // 启动线程
        t1.start();
        t2.start();
        // 等待所有线程结束
        t1.join();
        t2.join();
        // 通过修改 容器值 ，返回处理结果值
        temp.forEach(System.out::println);

    }

}
```

### 1.1.2 实现 Runnable 接口

- demo

```
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class RunnableThread {


    public static void main(String[] args) throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger();
        Thread t1 = new Thread(new ProcessThread(atomicInteger), "T1");
        Thread t2 = new Thread(new ProcessThread(atomicInteger), "T2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(atomicInteger.get());
    }
}

class ProcessThread implements Runnable{
    AtomicInteger atomicInteger;

    ProcessThread(AtomicInteger source){
        this.atomicInteger = source;
    }

    @Override
    public void run() {
        IntStream.range(0,100000000).forEach(e -> this.atomicInteger.addAndGet(1));

    }
}
```

### 1.1.3 Callable 接口

- 为什么要使用 Callable 接口

Callable 接口解决 Thread 类和 Callable 没有返回结果的缺点。

- 如何使用 Callable 接口实现多线程

1. 创建 `Callable` 接口的实现类，并实现 `call` 方法。该 `call` 方法将作为线程执行体，并且有返回值。
2. 创建 `Callable` 实现类的实例，使用 `FutureTask` 类来包装 `Callable` 对象，该 `FutureTask` 对象封装了该 `Callable` 对象的 `call` 方法的返回值。
3. 使用 `FutureTask` 对象作为 `Thread` 对象的 target 创建并启动新线程。
4. 调用 `FutureTask` 对象的 `get` 方法来获得线程执行结束后的返回值。

- Callable 接口

     Callable 定义了一个无输入参数，带有异常抛出和返回值的处理过程。

- Future 接口

     Future 用于管理线程的执行结果进行取消、查询是否完成、获取结果。 注意get 方法获取执行结果，会阻塞直到任务返回结果(可配置等待超时)。

- FutureTask 类

    FutureTask 类实现RunnableFuture接口，RunnableFuture 实现Runnable和Future 接口，并且通过调用 Callable 接口的实现方法，实现了数据的返回。

- demo

```
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CallableThread implements Callable<AtomicInteger> {
    static final AtomicInteger source = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Callable<AtomicInteger> callable = new CallableThread();
        FutureTask<AtomicInteger> futureTask = new FutureTask<AtomicInteger>(callable);
        FutureTask<AtomicInteger> futureTask2 = new FutureTask<AtomicInteger>(callable);

        Thread t1 = new Thread(futureTask, "t1");
        Thread t2 = new Thread(futureTask2, "t2");
        t1.start();t2.start();
        t1.join();t2.join();
        System.out.println(futureTask.get());
    }

    @Override
    public AtomicInteger call() throws Exception {
        for (int a = 0; a < 10000000; a++) {
            source.addAndGet(1);
        }
        return source;
    }
}
```

## 2. 线程的控制

### 2.1 休眠 & 礼让

- 休眠： Thread.sleep

- 礼让：  Thread.yield(一般用于线程完成任务，告知线程调度器优先运行其他同级别优先级线程)

### 2.2 终止

- Thread.stop Thread.suspend Thread.resume

> stop 方法存在缺陷，会导致资源未释放(锁被一直持有)，因此已被移除。suspend resume 也存在缺陷。

- 利用Thread.interrupt 配合 Thread.interrupted 方法

Thread.interrupt 可以中断某个线程(中断会引发中断标记为 True 或者是InterruptedException 异常。因此需要Thread.interrupted 判定该线程是否被中断 )。
> **注意Thread.interrupt  不能中断 `synchronized` 和除 `
lockInterruptibly`  以外的 ReentrantLock **

```
public class ThreadStopDemo3 {

    public static void main(String[] args) throws Exception {
        MyTask task = new MyTask();
        Thread thread = new Thread(task, "MyTask");
        thread.start();
        TimeUnit.MILLISECONDS.sleep(50);
        thread.interrupt();
    }

    private static class MyTask implements Runnable {

        private volatile long count = 0L;

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 线程启动");
            // 通过 Thread.interrupted 和 interrupt 配合来控制线程终止
            while (!Thread.interrupted()) {
                System.out.println(count++);
            }
            System.out.println(Thread.currentThread().getName() + " 线程终止");
        }
    }
}
```

- 使用 `volatile` 标志位控制线程终止 (类似中断信号)

> 注意，该方法必须自行保证对标志位的修改是原子操作(比如 )。
```
public class ThreadStopDemo2 {

    public static void main(String[] args) throws Exception {
        MyTask task = new MyTask();
        Thread thread = new Thread(task, "MyTask");
        thread.start();
        TimeUnit.MILLISECONDS.sleep(50);
        task.cancel();
    }

    private static class MyTask implements Runnable {

        private volatile boolean flag = true;

        private volatile long count = 0L;

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 线程启动");
            while (flag) {
                System.out.println(count++);
            }
            System.out.println(Thread.currentThread().getName() + " 线程终止");
        }

        /**
         * 通过 volatile 标志位来控制线程终止
         */
        public void cancel() {
            flag = false;
        }

    }

}
```

### 2.3 线程的优先级

### 2.4 守护线程

> **是在后台执行并且不会阻止 JVM 终止的线程**。 如jvm GC 线程。

## 3. 线程通信

### 3.1 信号量(wait/notify/notifyAll)

**注意：**

**1. wait/notify/notifyAll只能用于同步方法/代码块中，即 synchronized 修饰的代码块/方法/类。**

**2. 信号和信号量不一样，信号量只有数值。信号可以是软件层次上对中断机制的一种模拟，具有多种值(多种中断方式)。**

- `wait` - `wait` 会自动释放当前线程占有的对象锁，并请求操作系统挂起当前线程，**让线程从 `Running` 状态转入 `Waiting` 状态**，等待 `notify` / `notifyAll` 来唤醒。如果没有释放锁，那么其它线程就无法进入对象的同步方法或者同步控制块中，那么就无法执行 `notify` 或者 `notifyAll` 来唤醒挂起的线程，造成死锁。
- `notify` - 唤醒一个正在 `Waiting` 状态的线程，并让它拿到对象锁，具体唤醒哪一个线程由 JVM 控制 。
- `notifyAll` - 唤醒所有正在 `Waiting` 状态的线程，接下来它们需要竞争对象锁。       

      注意它们都属于 Object 的一部分，而不属于 Thread类。因为他们只能用在同步方法或者同步控制块中使用，否则会在运行时抛出 IllegalMonitorStateException。 

   ### 3.2 管道(PipedOutputStream`、`PipedInputStream`、`PipedReader` 和 `PipedWriter)

## 3.3 网络(RPC/Http)

### 3.4 内存共享(传入一个共享变量)





## 4.  线程的状态

      Linux 下 Java 线程也是基于系统函数生成的，出于跨平台考虑 java 线程模型异于Linux 线程状态，Linux Thread state 不等于  java.lang.Thread.State 。如 Java thread state 只有 Runnable 不区分 Linux 线程是运行中，还是等待被调度。

## 4.1 java.lang.Thread.State (Java 中线程状态)

- **新建（New）** - 尚未调用 `start` 方法的线程处于此状态。此状态意味着：**创建的线程尚未启动**。
- **就绪（Runnable）** - 已经调用了 `start` 方法的线程处于此状态。此状态意味着：**线程已经在 JVM 中运行**。但是在操作系统层面，它可能处于运行状态，也可能等待资源调度（例如处理器资源），资源调度完成就进入运行状态。所以该状态的可运行是指可以被运行，具体有没有运行要看底层操作系统的资源调度。**只有进入到就绪状态，线程才能被linux 调度。**
- **阻塞（Blocked）** - 此状态意味着：**线程处于被阻塞状态**。表示线程在等待 `synchronized` 的隐式锁（Monitor lock）。`synchronized` 修饰的方法、代码块同一时刻只允许一个线程执行，其他线程只能等待，即处于阻塞状态。当占用 `synchronized` 隐式锁的线程释放锁，并且等待的线程获得 `synchronized` 隐式锁时，就又会从 `BLOCKED` 转换到 `RUNNABLE` 状态。**只有进入到就绪状态，线程才能被linux 调度。**
- **等待（Waiting）** - 此状态意味着：**线程无限期等待，直到被其他线程显式地唤醒**。 阻塞和等待的区别在于，阻塞是被动的，它是在等待获取 `synchronized` 的隐式锁。而等待是主动的，通过调用 `Object.wait` 等方法进入。**只有进入到就绪状态，线程才能被linux 调度。**

- | 进入方法                                                     | 退出方法                             |
  | ------------------------------------------------------------ | ------------------------------------ |
  | 没有设置 Timeout 参数的 `Object.wait` 方法                   | `Object.notify` / `Object.notifyAll` |
  | 没有设置 Timeout 参数的 `Thread.join` 方法                   | 被调用的线程执行完毕                 |
  | `LockSupport.park` 方法（Java 并发包中的锁，都是基于它实现的） | `LockSupport.unpark`                 |

- **定时等待（Timed waiting）** - 此状态意味着：**无需等待其它线程显式地唤醒，在一定时间之后会被系统自动唤醒**。

  | 进入方法                                                     | 退出方法                                        |
  | ------------------------------------------------------------ | ----------------------------------------------- |
  | `Thread.sleep` 方法                                          | 时间结束                                        |
  | 获得 `synchronized` 隐式锁的线程，调用设置了 Timeout 参数的 `Object.wait` 方法 | 时间结束 / `Object.notify` / `Object.notifyAll` |
  | 设置了 Timeout 参数的 `Thread.join` 方法                     | 时间结束 / 被调用的线程执行完毕                 |
  | `LockSupport.parkNanos` 方法                                 | `LockSupport.unpark`                            |
  | `LockSupport.parkUntil` 方法                                 | `LockSupport.unpark`                            |

- **终止(Terminated)** - 线程执行完 `run` 方法，或者因异常退出了 `run` 方法。此状态意味着：线程结束了生命周期。