# Java-JVM-hotsopt-运行数据区

> reference:
>
> https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/Java%E5%86%85%E5%AD%98%E5%8C%BA%E5%9F%9F.md
>
> https://github.com/dunwu/javacore/blob/master/docs/jvm/jvm-memory.md
>
> **周志明《深入理解 Java 虚拟机》**

## 1. Java 内存区域 (运行时数据区)

## 1.1 总览

> 注意！！！jmm 是 java 内存模型，说的是 volatile 相关的问题，与jvm 没有必然联系！！！https://en.wikipedia.org/wiki/Java_memory_model

![img](https://img-blog.csdn.net/20170610165140237?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvYWlqaXVkdQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

- jvm 内存主要区域

1. 栈(内存不共享区域)

> pc（程序计数器）：指向下一条需要指向的命令。**分支、循环、跳转、异常处理、线程恢复等功能都需要依赖这个计数器来完成。**不会出现`OutOfMemoryError`区域。
>
> jvm stack:  每个方法的执行都会产生栈帧，栈帧存放了：**编译期可知局部变量表、操作数栈、动态链接、方法出口**。方法的调用开始到完结对应栈帧在 `jvm stack` 中入栈出栈的过程。
>
> native stack: **本地方法栈为 Native 方法服务**。本地方法并不是用 Java 实现的，而是由 C 语言实现的。

2. 堆(内存共享区域，注意 G1 ZGC 划分不再是以下的划分方式。以下是cms的划分方式，分代划分。 )

> - Yong Generation：年轻代 默认占总比率为：1：3，由`-XX:NewRatio=3`参数控制。 Eden 和 Survivor 的比例为 8:1；注意年轻代`GC`的时候只在乎老年代是否引用了年轻代，导致无法回收对象这种情况。
>
> Eden：大部分情况，对象都会首先在 Eden 区域分配。
>
> TLAB：`Eden`中为每一个线程都独立分配了一个`TLAB`，只有当`TLAB`分配失败，才会去`Eden`区申请空间，使得内存分配的并发冲突降低。TLAB
> 每一轮 `GC` 都会被清除数据。
>
> Survivor：`Eden` 与 `Survivor` 比率是4：1，两者构成标记复制算法的具体实现。
>
>
> - Old Generation：老年代，用于存放`大对象直接放入老年代`，`多轮gc后仍然存在的对象`，`空间分配担保`等场景。

3. 方法区(内存共享区域)

> 特别的，hotspot 把方法区的实现拆分为：内存和堆2部分构成。
>
> - 常量池(存放在内存)：
> - 非常量池：虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据

4. 直接内存

> native 方法执行的内存区域，还有`hotspot`方法区非常量池存放的实现。

## 2 垃圾相关

### 2.1 判定对象是否能被回收

- 引用计数：当一个对象增加一个引用时候，计数器加一，该算法需要额外的算法才能解决循环引用的问题。(**Python就是引用计数，一样解决了循环引用的问题。**)
- 可达性分析算法(jvm采用的方法)：以 `GC Roots` 作为起点搜索，所有可达的对象都标记为存活。**注意，hotspot 有老年代，存在跨代引用问题，老年代引用了年轻代的对象，为了减少消耗，引入卡表，避免扫描整个老年代。**

>  - `GC Roots定义`：
>  1. `jvm stack`中引用的对象
>  2. 本地方法`stack`中`jni`引用的对象
>  3. 方法区中，类静态属性引用的对象
>  4. 方法区中，常量引用的对象

### 2.2 垃圾收集算法

> 当经过`对象回收判定`算法的标识过可以回收的内存区域，下一步就是通过垃圾回收算法，回收这些空间。

#### 2.2.1 标记-清除

![img](https://raw.githubusercontent.com/dunwu/images/dev/cs/java/javacore/jvm/jvm-gc-mark-sweep.jpg)

#### 2.2.2 标记-整理

![img](https://raw.githubusercontent.com/dunwu/images/dev/cs/java/javacore/jvm/jvm-gc-mark-compact.jpg)

#### 2.2.3 复制(Eden,Survivor的存在的意义)

https://raw.githubusercontent.com/dunwu/images/dev/cs/java/javacore/jvm/jvm-gc-copying.jpg

### 2.3 垃圾收集器


