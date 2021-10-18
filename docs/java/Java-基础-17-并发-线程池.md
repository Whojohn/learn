# Java-基础-17-并发-线程池

reference:

1. https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html

2. https://www.cnblogs.com/thisiswhy/p/12690630.html

       线程池是一种多线程处理形式，将线程的创建、销毁交由线程池类负责，让用户专注于业务(线程管理和业务逻辑抽离)。

## 1. 线程池优缺点

- 优点

1. 减少线程创建的开销，提高响应速度。
2. 将线程的管理方式和业务逻辑抽离。

- 缺点

1. 线程池排障复杂。
2. 使用不当会导致 `oom` 等问题。

## 2. 线程池 Executor 框架

>         Executor 框架是一个根据一组执行策略调用，调度，执行和控制的异步任务的框架，目的是提供一种将”任务提交”与”任务如何运行”分离开来的机制。**注意区分 `Executor接口`，`Executors 类`。  **

###  2.1  核心 API

Executor 框架核心 API 如下：

- Executor - 运行任务的简单接口。

```
public interface Executor {
    void execute(Runnable command);
}
```

- ExecutorService - 扩展了 Executor 接口。扩展能力：
  1. 支持有返回值的线程；
  2. 支持管理线程的生命周期。
- AbstractExecutorService - ExecutorService 接口的抽象类，规定了线程池的具体实现类需要实现的控制方法。
- ThreadPoolExecutor - Executor 框架最核心的类，它继承了 AbstractExecutorService 类。
- ScheduledExecutorService - 扩展了 ExecutorService 接口。扩展能力：支持定期执行任务。
- ScheduledThreadPoolExecutor - ScheduledExecutorService 接口的实现，一个可定时调度任务的线程池。
- Executors - 可以通过调用 Executors 的静态工厂方法来创建线程池并返回一个 ExecutorService 对象。

## 3 ThreadPoolExecutor

        `ThreadPoolExecutor `  是最常用的线程池实现类，基于 `ThreadPoolExecutor`  衍生 `Executors` 类，包含了：`FixedThreadPool` ， `SingleThreadExecutor ` 等线程池实现。 《阿里巴巴 Java 开发手册》中强制线程池不允许使用 `Executors` 去创建线程池，只允许使用 `ThreadPoolExecutor `， 是因为衍生类都有特定场景。`ThreadPoolExecutor ` 理解透了，也能写出对应的实现类(防止使用不当)。

### 3.1 ThreadPoolExecutor 基本工作原理

        线程池内部通过队列，以生产者消费者的形式将线程和任务解耦。线程池核心主要包括两点：任务管理、线程管理。任务管理以生产者的角色，提交任务。线程管理以消费者的形式，维护在线程池内。实际执行流程如下（`任务调度方式描述`）:

1. 首先检测线程池运行状态，如果不是RUNNING，则直接拒绝，线程池要保证在RUNNING的状态下执行任务。

2. 如果workerCount < corePoolSize，则创建并启动一个线程来执行新提交的任务。

3. 如果workerCount >= corePoolSize，且线程池内的阻塞队列未满，则将任务添加到该阻塞队列中。

4. 如果workerCount >= corePoolSize && workerCount < maximumPoolSize，且线程池内的阻塞队列已满，则创建并启动一个线程来执行新提交的任务。

5. 如果workerCount >= maximumPoolSize，并且线程池内的阻塞队列已满, 则根据拒绝策略来处理该任务, 默认的处理方式是直接抛异常。

   ![图4 任务调度流程](https://p0.meituan.net/travelcube/31bad766983e212431077ca8da92762050214.png)

![图2 ThreadPoolExecutor运行流程](https://p0.meituan.net/travelcube/77441586f6b312a54264e3fcf5eebe2663494.png)

### 3.2 线程池生命周期(`ThreadPoolExecutor` ctl变量)

```
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
// runState is stored in the high-order bits
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;
```

参数说明：

- ctl - 用于控制线程池的运行状态和线程池中的有效线程数量。它包含两部分的信息：

  1. 线程池的运行状态 (runState)
  2. 线程池内有效线程的数量 (workerCount)

  > 可以看到，ctl 使用了 Integer 类型来保存，高 3 位保存 runState，低 29 位保存 workerCount。COUNT_BITS 就是 29，CAPACITY 就是 1 左移 29 位减 1（29 个 1），这个常量表示 workerCount 的上限值，大约是 5 亿。

- 运行状态 - 线程池一共有五种运行状态：

1. RUNNING - 运行状态。接受新任务，并且也能处理阻塞队列中的任务。

2. SHUTDOWN - 关闭状态。不接受新任务，但可以处理阻塞队列中的任务。

   > 1. 在线程池处于 RUNNING 状态时，调用 shutdown 方法会使线程池进入到该状态。
   >
   > 2. finalize 方法在执行过程中也会调用 shutdown 方法进入该状态。

3. STOP - 停止状态。**不接受新任务，也不处理队列中的任务,会中断正在处理任务的线程**。调用 shutdownNow 方法会使线程池进入到该状态。

4. TIDYING - 整理状态。如果所有的任务都已终止了，workerCount (有效线程数) 为 0，线程池进入该状态后会调用 terminated 方法进入 TERMINATED 状态。

5. TERMINATED - 已终止状态。在 terminated 方法执行完后进入该状态。默认 terminated 方法中什么也没有做。进入 TERMINATED 的条件如下：

   > 1. 线程池不是 RUNNING 状态；
   > 2. 线程池状态不是 TIDYING 状态或 TERMINATED 状态；
   > 3. 如果线程池状态是 SHUTDOWN 并且 workerQueue 为空；
   > 4. workerCount 为 0；
   > 5. 设置 TIDYING 状态成功。

### 3.3 线程池中工作线程- Worker

      线程池中真实执行任务的线程是`Worker` 类进行封装，它管理了一个具体的线程。

      线程池需要管理线程的生命周期，需要在线程长时间不运行的时候进行回收。线程池使用一张Hash表去持有线程的引用，这样可以通过添加引用、移除引用这样的操作来控制空余线程的生命周期。Worker是通过继承AQS，使用`AQS独占锁` 检测线程是否在运行作业。通过`AQS` 的`Lock` 方法判定是否有作业在运行，当锁可以获取时，说明线程空闲，回收该线程。

**注意线程的声明周期回收的触发只有以下2种情况**

**1. 仅当线程池线程数大于核心线程数时，并且有空余线程时才会触发。**

**2. 配置了`allowCoreThreadTimeOut`,  才可能回收核心线程。**

      核心代码如下

```
private final class Worker extends AbstractQueuedSynchronizer implements Runnable{
    final Thread thread; //Worker持有的线程
    Runnable firstTask; //初始化的任务，可以为null，firstTask通过线程工厂方法传入
}
```



### 3.4 `ThreadPoolExecutor` 使用方法

- 构造方法

```
public ThreadPoolExecutor(int corePoolSize, // 线程池核心线程数
                              int maximumPoolSize, // 最大线程数
                              long keepAliveTime, // 线程保持活动时间(没有作业时，不马上销毁线程)
                              TimeUnit unit, // keepAliveTime 的时间单位
                              BlockingQueue<Runnable> workQueue, // 任务队列
                              ThreadFactory threadFactory,// 线程工厂方法
                              RejectedExecutionHandler handler)// 任务队列拒绝机制
```

参数说明：

- `corePoolSize` - **线程池核心线程数**。

         除非配置了 `allowCoreThreadTimeOut` ，线程池一般会保留的最少线程数。假如线程池线程数在没有配置`allowCoreThreadTimeOut` 时少于核心线程数，说明提交的任务的并发远低于核心线程数并发(见`ThreadPoolExecutor` 基本工作原理，线程创建的流程)。

- `maximumPoolSize` - **最大线程数量**。

         只有使用有界队列时，该参数才会起效。**当队列容量已满时，线程池中的可用空闲工作线程为0，且线程池中线程数小于`maximumPoolSize` 创建新的线程，才会新建线程运行**。

- `keepAliveTime`：**线程保持活动的时间**。

         当线程池中的线程数量大于 `corePoolSize` 的时候，如果这时没有新的任务提交，核心线程外的线程不会立即销毁，而是会等待，直到等待的时间超过了 `keepAliveTime`。

- `unit` - **`keepAliveTime` 的时间单位**。有 7 种取值。可选的单位有天（DAYS），小时（HOURS），分钟（MINUTES），毫秒(MILLISECONDS)，微秒(MICROSECONDS, 千分之一毫秒)和毫微秒(NANOSECONDS, 千分之一微秒)。

- `workQueue` - **等待执行的任务队列**。用于保存等待执行的任务的阻塞队列。 可以选择以下几个阻塞队列。

  - `ArrayBlockingQueue` - **有界阻塞队列**。
    - 此队列是**基于数组的先进先出队列（FIFO）**。
    - 此队列创建时必须指定大小。
  - `LinkedBlockingQueue` - **无界阻塞队列**。
    - 此队列是**基于链表的先进先出队列（FIFO）**。
    - 如果创建时没有指定此队列大小，则默认为 `Integer.MAX_VALUE`。
    - 吞吐量通常要高于 `ArrayBlockingQueue`。
    - 使用 `LinkedBlockingQueue` 意味着： `maximumPoolSize` 将不起作用，线程池能创建的最大线程数为 `corePoolSize`，因为任务等待队列是无界队列。
    - `Executors.newFixedThreadPool` 使用了这个队列。
  - `SynchronousQueue` - **不会保存提交的任务，而是将直接新建一个线程来执行新来的任务**。
    - 每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态。
    - 吞吐量通常要高于 `LinkedBlockingQueue`。
    - `Executors.newCachedThreadPool` 使用了这个队列。
  - `PriorityBlockingQueue` - **具有优先级的无界阻塞队列**。

- `threadFactory` - **线程工厂**。可以通过线程工厂给每个创建出来的线程设置更有意义的名字。**默认工厂方法为`Executors.defaultThreadFactory()`**;

- `handler` - **饱和策略**。它是 `RejectedExecutionHandler` 类型的变量。当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。线程池支持以下策略：

  - `AbortPolicy` - 丢弃任务并抛出异常。这也是默认策略。
  - `DiscardPolicy` - 丢弃任务，但不抛出异常。
  - `DiscardOldestPolicy` - 丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）。
  - `CallerRunsPolicy` - 直接调用 `run` 方法并且阻塞执行。
  - 如果以上策略都不能满足需要，也可以通过实现 `RejectedExecutionHandler` 接口来定制处理策略。如记录日志或持久化不能处理的任务。

