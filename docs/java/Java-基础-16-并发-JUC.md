# Java-基础-16-并发-JUC

reference:

https://tech.meituan.com/2019/12/05/aqs-theory-and-apply.html

https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Condition.html

## 1. JUC-AQS

         `AbstractQueuedSynchronizer`（简称 **AQS**）是**队列同步器**，其主要作用是利用内置队列处理同步，让锁具备独享锁/共享锁的能力。它是并发锁和很多同步工具类的实现基石（如 `ReentrantLock`、`ReentrantReadWriteLock`、`CountDownLatch`、`Semaphore`、`FutureTask` 等）。**注意AQS 只提供独享锁的功能，不提供可重入的功能，可重入，需要自行实现。**
    
        如下图所示， AQS 从API层提供了独享锁/共享锁操作api。

![img](https://p1.meituan.net/travelcube/82077ccf14127a87b77cefd1ccf562d3253591.png)



### 1.1 AQS 原理

ASQ 原理要点：

- AQS 使用一个整型的 `volatile` 变量`state`来 **维护同步状态**。状态的意义由子类赋予。
- AQS 维护了一个 FIFO 的双链表，用来存储获取锁失败的线程。

AQS 围绕同步状态提供两种基本操作“获取”和“释放”，并提供一系列判断和处理方法，简单说几点：

- state 是独占的，还是共享的；
- state 被获取后，其他线程需要等待；
- state 被释放后，唤醒等待线程；
- 线程等不及时，如何退出等待。

至于线程是否可以获得 state，如何释放 state，就不是 AQS 关心的了，要由子类具体实现。

AQS 的数据结构:

```
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** 等待队列的队头，懒加载。只能通过 setHead 方法修改。 */
    private transient volatile Node head;
    /** 等待队列的队尾，懒加载。只能通过 enq 方法添加新的等待节点。*/
    private transient volatile Node tail;
    /** 同步状态 */
    private volatile int state;
}
```

- state

>  AQS 使用一个整型的 volatile 变量来 维护同步状态。
>
>  - 这个整数状态的意义由子类来赋予，如ReentrantLock 中该状态值表示所有者线程已经重复获取该锁的次数，Semaphore 中该状态值表示剩余的许可数量。


- head 和 tail 

>  AQS 维护了一个 Node 类型（AQS 的内部类）的双链表来完成同步状态的管理。这个双链表是一个双向的 FIFO 队列，通过 head 和 tail 指针进行访问。当 有线程获取锁失败后，就被添加到队列末尾。

AQS Node数据结构：

```
static final class Node {
    /** 该等待同步的节点处于共享模式 */
    static final Node SHARED = new Node();
    /** 该等待同步的节点处于独占模式 */
    static final Node EXCLUSIVE = null;

    /** 线程等待状态，状态值有: 0、1、-1、-2、-3 */
    volatile int waitStatus;
    static final int CANCELLED =  1;
    static final int SIGNAL    = -1;
    static final int CONDITION = -2;
    static final int PROPAGATE = -3;

    /** 前驱节点 */
    volatile Node prev;
    /** 后继节点 */
    volatile Node next;
    /** 等待锁的线程 */
    volatile Thread thread;

  	/** 和节点是否共享有关 */
    Node nextWaiter;
}
```

- waitStatus 
  Node 使用一个整型的 volatile 变量来 维护 AQS 同步队列中线程节点的状态。waitStatus 有五个状态值：
  1. CANCELLED(1) - 此状态表示：该节点的线程可能由于超时或被中断而处于被取消(作废)状态，一旦处于这个状态，表示这个节点应该从等待队列中移除。
  2. SIGNAL(-1) - 此状态表示：后继节点会被挂起，因此在当前节点释放锁或被取消之后，必须唤醒(unparking)其后继结点。
  3. CONDITION(-2) - 此状态表示：该节点的线程 处于等待条件状态，不会被当作是同步队列上的节点，直到被唤醒(signal)，设置其值为 0，再重新进入阻塞状态。
  4. PROPAGATE(-3) - 此状态表示：下一个 acquireShared 应无条件传播。
  5. 0 - 非以上状态。

## 2. JUC-核心接口-Lock & Condition

### 2.1 Lock

- Lock vs synchronized 

       Lock 接口用于解决并发中的互斥问题(锁)。**`Lock` 提供了一组无条件的、可轮询的、定时的以及可中断的锁操作**，所有获取锁、释放锁的操作都是显式的操作。Lock 实现类可以(synchronized无法解决)：

1. 响应中断
2. 超时释放锁
3. 非阻塞地获取锁()

- Lock 接口

```
public interface Lock {
    void lock();//获取锁
    void lockInterruptibly() throws InterruptedException;//锁未被另一个线程持有，且线程没有被中断的情况下，才能获取锁
    boolean tryLock();//尝试获取锁，仅在调用时锁未被另一个线程持有的情况下，才获取该锁
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;//和 tryLock() 类似，区别仅在于限定时间，如果限定时间内未获取到锁，视为失败。
    void unlock();//释放锁
    Condition newCondition();//返回一个绑定到 Lock 对象上的 Condition 实例
}
```

### 2.2 Condition

          `Lock` 接口(类似`synchronized ` )提供了锁的实现接口，`Condition`接口 提供了对线程的控制，类似 ([`wait`](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#wait--), [`notify`](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#notify--) and [`notifyAll`](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#notifyAll--), 但是控制粒度更加灵活)，注意`Condition`配合`Lock`接口使用（JUC 中 Lock 接口`newCondition` 方法返回`Condition`实例）。

- Condition 接口

```
public interface Condition {
    void await() throws InterruptedException;// 类似`synchronized` 中的wait 方法,与`synchronized` 不同的是支持 interrputed 方法中断
    void awaitUninterruptibly();// 
    long awaitNanos(long nanosTimeout) throws InterruptedException;// 带有超时的wait
    boolean await(long time, TimeUnit unit) throws InterruptedException;//等同awaitNanos方法
    boolean awaitUntil(Date deadline) throws InterruptedException;// 带有超时的wait
    void signal(); // 类似`synchronized` 中的 `notify` 方法
    void signalAll();// 类似 `synchronized` 中的 `notifyAll` 方法
}
```

- `Condititon` vs（ `wait`、`notify`、`notifyAll`）

1. 每个锁（`Lock`）上可以存在多个 `Condition`，这意味着锁的状态条件可以有多个。

2. 支持公平的或非公平的队列操作。

3. 支持可中断的条件等待，相关方法：`awaitUninterruptibly()` 。

4. 支持可定时的等待，相关方法：`awaitNanos(long)` 、`await(long, TimeUnit)`、`awaitUntil(Date)`。

- `Condition` 模拟生产者，消费者实例

> 事实上，一般不会直接使用`Condition`。使用 `CountDownLatch`、`Semaphore` 等工具更为便捷、安全。

```
class BoundedBuffer {
   final Lock lock = new ReentrantLock();
   final Condition notFull  = lock.newCondition(); 
   final Condition notEmpty = lock.newCondition(); 

   final Object[] items = new Object[100];
   int putptr, takeptr, count;

   public void put(Object x) throws InterruptedException {
     lock.lock();
     try {
       while (count == items.length)
         notFull.await();
       items[putptr] = x;
       if (++putptr == items.length) putptr = 0;
       ++count;
       notEmpty.signal();
     } finally {
       lock.unlock();
     }
   }
	
   public Object take() throws InterruptedException {
     lock.lock();
     try {
       while (count == 0)
         notEmpty.await();
       Object x = items[takeptr];
       if (++takeptr == items.length) takeptr = 0;
       --count;
       notFull.signal();
       return x;
     } finally {
       lock.unlock();
     }
   }
 }
```

## 3. Lock 实现类

### 3.1 ReentrantLock

       `ReentrantLock` 类是 `Lock` 接口的具体实现，与内置锁 `synchronized` 相同的是，它是一个**可重入锁**。它可选：公平/非公平，支持锁获取超时等特性，注意`ReentrantLock`是独占锁，假如需要共享锁的支持使用`ReentrantReadWriteLock`。

        **注意，锁的释放、获取的消耗远比计算要大。更小粒度的锁多次申请、释放锁的开销，可能比单次保持更大粒度，更少申请、释放锁的开销要更大。**

### 3.1.1 `Lock` & `unlock` (无条件获取锁)

1. 无条件获取锁(`lock` `unlock`，假如需要获取锁超时，避免死锁，应该使用`tryLock` )

- 语法

```
class X {
   private final ReentrantLock lock = new ReentrantLock();
   // ...

   public void m() {
     lock.lock();  // 无条件获取锁。如果当前线程无法获取锁，则当前线程进入休眠状态不可用，直至当前线程获取到锁。如果该锁没有被另一个线程持有，则获取该锁并立即返回，将锁的持有计数设置为 1。
     try {
       // ... method body
     } finally {
       lock.unlock()//释放锁
     }
   }
 }
```

- Demo

```
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
                for (int a = 0; a < 3000000; a++) {
                    temp += 1;
                }
                for (int a = 0; a < 3000000; a++) {
                    temp -= 1;
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

```

### 3.1.2 `tryLock` & `unlock` (获取锁带有超时，防止死锁)

      与无条件获取锁相比，tryLock 拥有获取锁超时的支持，避免长时间获取锁失败，堵塞线程造成死锁。tryLock 支持2种参数：

1. `tryLock()` - **可轮询获取锁**。如果成功，则返回 true；如果失败，则返回 false。也就是说，这个方法**无论成败都会立即返回**，获取不到锁（锁已被其他线程获取）时不会一直等待。

2. `tryLock(long, TimeUnit)` - **可定时获取锁**。和 `tryLock()` 类似，区别仅在于这个方法在**获取不到锁时会等待一定的时间**，在时间期限之内如果还获取不到锁，就返回 false。如果如果一开始拿到锁或者在等待期间内拿到了锁，则返回 true。

- 语法

```
public void execute() {
    try {
        if (lock.tryLock(2, TimeUnit.SECONDS)) {
            try {
                for (int i = 0; i < 3; i++) {
                    // 略...
                }
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " 获取锁失败");
        }
    } catch (InterruptedException e) {
        System.out.println(Thread.currentThread().getName() + " 获取锁超时");
        e.printStackTrace();
    }
}
```



### 3.1.3 `lockInterruptibly ` & `unlock`(中断未获得锁线程)

      **`Thread.interrupt`  只能中断`lockInterruptibly` 修饰的 `ReentrantLock` 。并且只能中断没有获得锁的线程，中断逻辑需要用户自行捕获实现。**

- 语法

```
public void execute() {
    try {
        lock.lockInterruptibly();

        for (int i = 0; i < 3; i++) {
            // 略...
        }
    } catch (InterruptedException e) {
        System.out.println(Thread.currentThread().getName() + "被中断");
        e.printStackTrace();
    } finally {
        lock.unlock();
    }
}
```

### 3.1.4  ReentrantLock 锁配合 Condition

### 3.1.5 ReentrantReadWriteLock(`RenntrantLock`共享锁实现)

- ReadWriteLock 接口定义

```
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
```

### 3.1.6 StampedLock （ReentrantReadWriteLock 的改进）

ReadWriteLock 支持两种模式：一种是读锁，一种是写锁。而 StampedLock 支持三种模式，分别是：**写锁**、**悲观读锁**和**乐观读**。其中，写锁、悲观读锁的语义和 ReadWriteLock 的写锁、读锁的语义非常类似，允许多个线程同时获取悲观读锁，但是只允许一个线程获取写锁，写锁和悲观读锁是互斥的。不同的是：StampedLock 里的写锁和悲观读锁加锁成功之后，都会返回一个 stamp；然后解锁的时候，需要传入这个 stamp。

> 注意这里，用的是“乐观读”这个词，而不是“乐观读锁”，是要提醒你，**乐观读这个操作是无锁的**，所以相比较 ReadWriteLock 的读锁，乐观读的性能更好一些。

StampedLock 的性能之所以比 ReadWriteLock 还要好，其关键是 **StampedLock 支持乐观读**的方式。

- ReadWriteLock 支持多个线程同时读，但是当多个线程同时读的时候，所有的写操作会被阻塞；
- 而 StampedLock 提供的乐观读，是允许一个线程获取写锁的，也就是说不是所有的写操作都被阻塞。

对于读多写少的场景 StampedLock 性能很好，简单的应用场景基本上可以替代 ReadWriteLock，但是**StampedLock 的功能仅仅是 ReadWriteLock 的子集**，在使用的时候，还是有几个地方需要注意一下。

- **StampedLock 不支持重入**
- **StampedLock 的悲观读锁、写锁都不支持条件变量（Condition不支持）**。
- 如果线程阻塞在 StampedLock 的 readLock() 或者 writeLock() 上时，此时调用该阻塞线程的 interrupt() 方法，会导致 CPU 飙升。**使用 StampedLock 一定不要调用中断操作，如果需要支持中断功能，一定使用可中断的悲观读锁 readLockInterruptibly() 和写锁 writeLockInterruptibly()**。

## 4. JUC-线程控制

     注意 JUC 线程控制方法，可以多个使用多个线程控制类一起使用。

### 4.1 CountDownLatch

> 用户传入一个值，`CountDownLatch` 维护一个计数器 `count`，当 `count` 为0 时候，唤醒 wait 的线程。wait 线程可以是多个。

```
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
```

### 4.2 CyclicBarrier

> 基于 `ReentrantLock` 和 `Condition` 实现。**可以让一组线程等待至某个状态之后再全部同时执行.**

### 4.3 Semaphore

> 字面意思为 **信号量**。`Semaphore` 用来控制某段代码块的并发数。

