# Java-JVM-总览

> reference:
>
> https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html

## 1. 什么是 jvm

     JVM类似物理机，拥有独立指令集，内存管理。 JVM通过抽象操作系统和CPU结构，提供了一种与平台无关的代码执行方法，即与特殊的实现方法、主机硬件、主机操作系统无关运行环境。

## 2. JVM 包含了几个部分

![img](https://img-blog.csdn.net/20170610165140237?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvYWlqaXVkdQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

> ps 这个部分十分混乱，oracle jvm 标准划分比较混乱
>
> reference:
>
> https://www.freecodecamp.org/news/jvm-tutorial-java-virtual-machine-architecture-explained-for-beginners/

- ClassLoader（类加载器）

> - 包含以下部分
>
> 1. 三种类加载器：Bootstarp 、Extension 、Application ClassLoader。
> 2. 加载顺序：双亲委派。
> 3. 自定义加载器：SPI 等。
> 4. 类加载流: load link ini

- Runtime Data Area

      jvm 规范中只讲述了必须包含的部分，没有强制要求如何实现，如：方法区放在堆还是直接内存上。！！！注意 jvm 内存和 java 内存模型不是一样的东西，内存模型是共享内存的并发模型，java 内存模型主要跟volatile 实现有关！！！并且jvm 的内存屏障与 hotspot 的写屏障没有任何关系。！！！

> - 包含以下部分
>
> 1. pc register 
>
> 2. jvm stack
>
> 3. native method stack
>
> 4. heap
>
> 5. method area
>
> 6. Run-time Constant Pool(常量池)

- Execution Engine（执行引擎）

> - 包含以下部分
>
> 1. 解释器： 快速解释字节码，但执行却很慢。 解释器的缺点就是,当一个方法被调用多次，每次都需要重新解释。
> 2. jit 编译器：jvm 发现热点代码，会通过 jit 编译器把热点代码编译成本地机器码，防止多次调用解释器，引发性能问题。
> 3. Garbage Collector （垃圾回收器）：cms、g1 、zgc 等具体实现。



- Native Interface（本地库接口）

> **JNI** 会与**本地方法库**进行交互并提供执行引擎所需的本地库

- 本地方法库

  >  它是一个执行引擎所需的本地库的集合。

## 3. JVM 具体实现 - hotspot

> reference:
>
> http://openjdk.java.net/groups/hotspot/docs/HotSpotGlossary.html

### 3.1 Execution Engine(执行引擎相关)

- jit 编译手段：
  1. c1   (client 默认方法)
  2. c2（server 默认方式，比c1 更加优化）

### 3.2 GC 相关( hotspot 特有)

- **block start table**：块起始表
- **card table**：卡表，解决对象相互引用，Minor GC时也必须扫描**完整**老年代(防止老年代持有年轻代对象)，消耗过大的问题。**卡表**的具体策略是将老年代的空间分成大小为512B的若干张卡（card）。卡表本身是单字节数组，数组中的每个元素对应着一张卡，当发生老年代引用新生代时，虚拟机将该卡对应的卡表元素设置为适当的值。**卡表还有另外的作用，标识并发标记阶段哪些块被修改过**(因为 cms 并发标识其他线程也在同时修改对象，需要通过卡表，记录那些对象在并发阶段被修改了。)。
- **write barrier**: **hotspot 写屏障对一个对象引用进行写操作（即引用赋值）之前或之后执行特定操作。实现了标识并发标记阶段被修改的对象。**
- **GC map（oopmap）**：OopMap 记录了栈中那些对象是引用对象。它的主要目的是在 Java 堆栈上找到 GC 根，并在对象在堆内移动时更新引用。
- **young generation**(g1不区分)：年轻代
- **old generation**(g1不区分)：老年代
- **eden**
- **survivor spce** 
- 内存泄露
- 内存溢出
- 分区担保
- 动态老年代
- cms 浮动垃圾

### 3.3 对象相关

#### 3.3.1 对象创建流程

- 对象的创建流程

  1. 类加载检测
  2. 分配内存
  3. 初始化零值
  4. 设置对象头
  5. 执行 init 方法

- 分配内存细节

> 预先分配：TLAB，每个线程中都有预先分配 TLAB 内存，分配对象内存先尝试分配TLAB空间，失败走并发分配。TLAB 只能存在于一个GC周期中，GC后，会把数据移出 TLAB区域。
>
> 并发分配安全：cas+失败重试

#### 3.3.2 对象的内存布局

- 对象内存布局

1. 对象头
2. 实例数据
3. 对齐填充

#### 3.3.3 对象的访问与定位

- 访问定位方式

1. 使用句柄
2. 直接指针

#### 3.3.4 对象的引用方式
1. 强引用：对象只有在没有引用的情况下才能背gc。
2. 软引用：当内存不足时，尝试回收软引用对象，一般用于缓存。
3. 弱引用：下一次gc必须会回收的对象。
4. 虚引用：一般用于监听系统gc情况。

#### 3.3.4 对象的死亡

- finalize

- 二次标记

#### 3.3.5 java 内存指针压缩

- 为什么需要指针压缩？
    JVM 运行在64位时候，对象的指针会比32位时候数值更大，消耗更多的内存空间，cpu 能缓存的指针也会变少。因此jvm利用压缩指针的方式，以32位表达64位的空间，可以减少这些消耗。
- 为什么jvm 大小不能超过32G？
    1. 当jvm内存大小超过32G，64位系统下，默认指针压缩算法失效，只能以64位保存指针信息。
    2. cms 等gc 算法在jvm 超过32G下表现更差。





