# Java-基础-15-并发-关键技术详解

## 1. 锁类型

reference :

https://tech.meituan.com/2018/11/15/java-lock.html

### 1.1 悲观锁 vs 乐观锁

- 悲观锁

      任何操作都需要获取锁，锁获取后会堵塞其他获取锁的线程。java 中悲观锁有 `synchronized` 和`ReentrantLock`。 `ReentrantLock`是 JDK 基于 AQS 实现的，  `synchronized`  是 jvm 实现，1.6后通过引入锁的升级提高性能(不再是单纯的 monitor 实现)。

- 乐观锁

  先操作，然后对比内存对象，假如变量没有变化，则操作成功，否则读取新变量按照用户定义执行操作(可以一直重试，也可以有限重试等)。Java 中乐观锁有 CAS。

### 1.2 可重入锁 vs 不可重入锁

- 可重入锁

      一个线程内，假如已经获取成功一个锁，那么该线程后续可以多次获取这个锁。但是其他线程依旧不能获取该锁。这样做的目的避免，一个线程内多次获取锁，导致自身死锁。

- 不可重入锁

  与上相反

### 1.3 公平锁 vs 非公平锁

- 公平锁

  按照申请锁的顺序进行锁的分配。

- 非公平锁

  允许锁的申请争抢。虽然非公平锁也有等待队列，但是，假如当前有活动的线程，申请锁可以先尝试能否获取，假如此时锁刚好被释放，申请成功(无需从队列中唤醒线程，减少唤醒开销)。

### 1.4 自旋锁 vs 非自旋锁 vs 适应性自旋锁

- 自旋锁

      线程的堵塞唤醒都会引发 cpu 上下文切换，为了减少切换的开销。对于需要堵塞的线程，自旋转可以让 cpu 等待一定次数(非堵塞)，重复获取锁。当然，这种等待需要消耗 cpu 时间，等待期间不能有其他操作。

- 适应性自旋锁

      历史上自旋等待都失败达到一定值，或者某些其他可以优化自旋的场景，对自旋锁的优化。

### 1.5 无锁 VS 偏向锁 VS 轻量级锁 VS 重量级锁 (synchronized 的优化)

**具体内容见`synchronized`底层,!!!注意sync是从偏向锁向上转化。(除非是编译器优化，消除锁，不然不会有无锁。)!!!**

### 1.6 独享锁 VS 共享锁

- 独享锁(排他锁、写锁)

      锁只能被一个线程独占，排他锁该线程可以修改，读取数据。

- 共享锁(读锁)

      锁可以被多个线程占有，但是共享锁只能读，不能修改数据。

## 2. synchronized

     `synchronized` 是可重入、非公平锁、独享的悲观锁。**构造方法不能使用 synchronized 关键字修饰，**因为构造方法本身就属于线程安全的，不存在同步的构造方法一说。需要公平、悲观锁使用`ReentrantLock`。

## 2.1 使用方式

- 修饰的方式

1. 修饰实例方法 

> 锁的对象是某个对象实例。

2. 修饰静态方法

> 锁的对象是当前类的对象。

3. 修饰代码块

>  假如代码块 `synchronized` 括号内填入的是类，锁的对象是类。假如是实例对象，则是某个对象的实例。

- 配合使用的关键字：`wait()`和`notify()` \ `notifyAll()`

  wait 会引发 Momitor 进入 wait 区域(该线程进入到**Waiting**状态)，notify()`/`notifyAll()可以唤醒其他线程。

- demo

```
class SynchronizedStatic implements Runnable {
    static int temp = 0;

    /**
     * 2个线程下不同实例对象， temp 输出并不是 200000 , 证明 synchronized 变量修饰类非静态方法时，只锁定一个实例对象的
     */
    public synchronized static void addSyncOnStaticObjectInstance() {

        for (int a = 0; a < 1000000; a++) {
            SynchronizedStatic.temp += 1;
        }
    }

    @Override
    public void run() {
        addSyncOnStaticObjectInstance();
    }
}

public class SynchronizedDemo implements Runnable {
    static int temp = 0;

    /**
     * 2个线程下不同实例对象， temp 输出并不是 200000 , 证明 synchronized 变量修饰类非静态方法时，只锁定一个实例对象的
     */
    public synchronized void addSyncOnObjectInstance() {

        for (int a = 0; a < 1000000; a++) {
            SynchronizedDemo.temp += 1;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        System.out.println("synchronized 修饰非静态方法");
        System.out.println("不同类实例调用，并发不安全");
        Thread t1 = new Thread(new SynchronizedDemo());
        Thread t2 = new Thread(new SynchronizedDemo());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(SynchronizedDemo.temp);//非安全锁
        SynchronizedDemo.temp = 0;

        System.out.println("synchronized 修饰非静态方法");
        System.out.println("同类实例调用,并发安全");
        Runnable s = new SynchronizedDemo();
        t1 = new Thread(s);
        t2 = new Thread(s);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(SynchronizedDemo.temp);//非安全锁
        SynchronizedDemo.temp = 0;

        System.out.println("synchronized 修饰静态方法");
        System.out.println("不同类实例调用，因为方法为类方法，所以是并发安全");
        t1 = new Thread(new SynchronizedStatic());
        t2 = new Thread(new SynchronizedStatic());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(SynchronizedStatic.temp);//非安全锁
        SynchronizedStatic.temp = 0;
    }

    public void run() {
        this.addSyncOnObjectInstance();
    }
}

```

## 2.2 synchronized 底层原理

### 2.2.1 Monitor(机制)

>  reference:
>
>  https://kongzheng1993.github.io/2020/04/17/kongzheng1993-Java_Monitor/

        **注意，Monitor 更多的是强调一种机制， Java 中 Monitor 机制使用 Monitor 对象实现。**

- Monitor 机制简述

当一个线程进入monitor是有三个时期（如下图）；

![MonitorObject.png](https://segmentfault.com/img/remote/1460000016417020)

**1. 进入monitor，此时是被分配在entry set 中，等待lock的拥有者释放锁；**

**2. 当线程获得lock就会成为lock的拥有者（仅有一个线程可以拥有），此时处于 the owner 区域；**

**3. 当释放lock就会进入wait set (显式调用 wait 进入)，但是处于wait set 中的线程还是有机会获得lock的拥有权。当线程结束，也可以直接释放锁，退出以上循环过程。**



- Java 中 Monitor 实现

>        Monitor 是线程私有的数据结构(对象头指向该数据结构)，每一个线程都有一个可用monitor record列表，同时还有一个全局的可用列表。每一个被锁住的对象都会和一个monitor关联（对象头的MarkWord中的LockWord指向monitor的起始地址），同时monitor中有一个Owner字段存放拥有该锁的线程的唯一标识，表示该锁被这个线程占用。

Monitor 底层源码实现如下：

```
ObjectMonitor() {
    _header       = NULL;
    _count        = 0; // 记录个数
    _waiters      = 0,
    _recursions   = 0;
    _object       = NULL;
    _owner        = NULL;
    _WaitSet      = NULL; // 处于wait状态的线程，会被加入到_WaitSet
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;
    FreeNext      = NULL ;
    _EntryList    = NULL ; // 处于等待锁block状态的线程，会被加入到该列表
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
  }
```

### 2.2.2 Java 中对象的实现

      `类`在`Java`中以 `Class` 各个唯一的实例 `Class` 标识；在 Java 中，对象以 `HotSpot`实现为例，对象在内存中包含:对象头、实例数据和对齐填充;对象头有两大部分：Mark Word（标记字段）、Klass Pointer（类型指针），如下图所示(64位置jvm）；

![img](https://raw.githubusercontent.com/dunwu/images/dev/snap/20200629191250.png)



- Mark Word（标记字段）

    包含了哈希码(HashCode)、GC分代年龄、锁状态标志、线程持有的锁、偏向线程ID、偏向时间戳等信息。(这一部分就是对应 `synchronized `的实现数据结构，包括`synchronized` 四种锁状态的标识 )

- Klass Pointer（类型指针）

    该对象指向它的**类元数据**的指针，虚拟机通过这个指针来确定该对象是哪个类的实例。

### 2.2.3 无锁 VS 偏向锁 VS 轻量级锁 VS 重量级锁 (synchronized 的膨胀锁)

> reference:
>
> https://segmentfault.com/a/1190000022415751

- Monitor 机制的缺陷

     `Monitor` 的实现完全是依靠操作系统内部的互斥锁(Mutex Lock)，因为需要进行用户态到内核态的切换，所以性能不佳。

- 如何优化？

Java 1.6 引入了偏向锁和轻量级锁，从而让 `synchronized` 拥有了四个状态( Java 中锁状态)：

1. **无锁状态（unlocked）**

2. **偏向锁状态（biasble）**

3. **轻量级锁状态（lightweight locked）**

4. **重量级锁状态（inflated）**

![img](https://image-static.segmentfault.com/201/210/2012106475-03aedb3c9278ab11)

    当 JVM 检测到不同的竞争状况时，会自动切换到适合的锁实现，`synchronized`在`JVM 8` 默认起始是偏向锁。

**无锁**

    无锁，没有任何锁。只有偏向锁释放时候，对象才能转变为无锁或者是轻量锁。无锁意味着此刻没有任何线程访问该变量(因为默认`synchronized`会把变量变为偏向锁)。

**偏向锁**(适合单线程场景)

      偏向锁是指一段同步代码一直被一个线程所访问，那么该线程会自动获取锁，降低获取锁的代价。

       在大多数情况下，锁总是由同一线程多次获得，不存在多线程竞争，所以出现了偏向锁。其目标就是在只有一个线程执行同步代码块时能够提高性能。

        当一个线程访问同步代码块并获取锁时，会在Mark Word里存储锁偏向的线程ID。在线程进入和退出同步块时不再通过CAS操作来加锁和解锁，而是检测Mark Word里是否存储着指向当前线程的偏向锁。引入偏向锁是为了在无多线程竞争的情况下尽量减少不必要的轻量级锁执行路径，因为轻量级锁的获取及释放依赖多次CAS原子指令，而偏向锁只需要在置换ThreadID的时候依赖一次CAS原子指令即可。

        偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，线程不会主动释放偏向锁。偏向锁的撤销，需要等待全局安全点（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态。撤销偏向锁后恢复到无锁（标志位为“01”）或轻量级锁（标志位为“00”）的状态。

       偏向锁在JDK 6及以后的JVM里是默认启用的。可以通过JVM参数关闭偏向锁：-XX:-UseBiasedLocking=false，关闭之后程序默认会进入轻量级锁状态。

**轻量级锁**(适合追求响应时间，缺点是自旋的消耗，高并发下的冲突)

        是指当锁是偏向锁的时候，被另外的线程所访问，偏向锁就会升级为轻量级锁，其他线程会通过自旋的形式尝试获取锁，不会阻塞，从而提高性能。

        在代码进入同步块的时候，如果同步对象锁状态为无锁状态（锁标志位为“01”状态，是否为偏向锁为“0”），虚拟机首先将在当前线程的栈帧中建立一个名为锁记录（Lock Record）的空间，用于存储锁对象目前的Mark Word的拷贝，然后拷贝对象头中的Mark Word复制到锁记录中。

       拷贝成功后，虚拟机将使用CAS操作尝试将对象的Mark Word更新为指向Lock Record的指针，并将Lock Record里的owner指针指向对象的Mark Word。

       如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象Mark Word的锁标志位设置为“00”，表示此对象处于轻量级锁定状态。

      如果轻量级锁的更新操作失败了，虚拟机首先会检查对象的Mark Word是否指向当前线程的栈帧，如果是就说明当前线程已经拥有了这个对象的锁，那就可以直接进入同步块继续执行，否则说明多个线程竞争锁。

     **若当前只有一个等待线程，则该线程通过自旋进行等待。但当自旋超过一定的次数，或者一个线程在持有锁，一个在自旋，又有第三个来访时，轻量级锁升级为重量级锁。**

**重量级锁**(适合同步代码执行时间长，高并发下冲突)

     升级为重量级锁时，锁标志的状态值变为“10”，此时Mark Word中存储的是指向重量级锁的指针，此时等待锁的线程都会进入阻塞状态。

   ### 2.2.4 其他锁优化手段

- **锁消除**

  在编译时检测出不可能存在竞争的共享数据的锁进行消除。

- **锁粗化**

      在编译时如果发现几个相邻的同步块使用的是同一个锁实例，那么 JIT 编译器将会把这几个同步块合并为一个大的同步块。

- **自旋锁**（轻量锁底层）



## 3. volatile

### 3.1 volatile 解决的问题

    多核cpu 缓存与内存不一致，会导致并发下数据读取的异常。jvm 利用 volatile 操作，将数值的改变刷新倒内存中，并且保证其他核心读取的到最新的变量。

- vloatile 可以解决的问题
  - **线程可见性** - 保证了不同线程对这个变量进行操作时的可见性，即一个线程修改了某个共享变量，另外一个线程能读到这个修改的值。
  - **禁止指令重排序** 
  - **注意！！！volatile不保证原子性**

### 3.2 volatile 的原理

     内存屏障：

1. 实现变量的一致。将cpu缓存的值修改刷新到内存中，并且使该变量在其他cpu中的缓存的值失效。
2. 静止指令重排列。



## 4. CAS

- 什么是 CAS

​	利用 compare and set 的原理（比较并交换）实现的乐观锁。Java 一般使用 JUC 包内置的 cas 实现：AtomicInteger、AtomicLong、AtomicBoolean 、AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray 、AtomicReference 等。

- CAS 缺点

1. 高并发下大量自旋等待消耗cpu。

   2. 被 CAS 保护的代码块变量，假如操作耗时，将会引发长时间其他线程的自旋等待。
2. ABA 问题。**如果一个变量初次读取的时候是 A 值，它的值被改成了 B，后来又被改回为 A，那 CAS 操作就会误认为它从来没有被改变过**。(可以通过**`AtomicStampedReference`** 类进行解决)

- CAS 适用场景

  读多，写少。或者是单一写，高并发场合。

- LongAddr 改进高并发性能

  LongAddr 将单次修改操作，变为单次执行多个线程修改操作。

## 5. ThreadLocal 

- ThreadLocal 使用场景

     链路跟踪，线程自带变量如线程名字等。

- ThreadLocal 使用

  **ThreadLocal 在线程逻辑结束时必须显示调用 remove 方式，防止内存泄漏**

  ```
  ThreadLocal<String> threadLocal = new ThreadLocal();
  try {
      threadLocal.set("xxx");
      // ...
      String xxx = threadLocal.get();
      ///
  } finally {
      threadLocal.remove();
  }
  ```

  

  

  

