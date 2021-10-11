# Flink-2-架构

reference:

https://nightlies.apache.org/flink/flink-docs-master/zh/docs/concepts/flink-architecture/#taskmanagers



### 1.1 Flink 组件

> Flink 组件主要分为：Client , Jobmanager , TaskManager 。


![The processes involved in executing a Flink dataflow](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap2-flink-architecture.svg?raw=true)


#### 1.1.1 Cient

用户一般通过`client` 提交任务，如：./bin/flink。`client` 会生成 `StreamGraph` 转化为 `JobGraph`推送到 `JobManager`。

#### 1.1.2 JobManager

`JobManager`具有协调 `Flink` 分布式执行,如：它决定何时调度下一个 task（或一组 task）、对完成的 task 或执行失败做出反应、协调 checkpoint、并且协调从失败中恢复等等。这个进程由三个不同的组件组成：

- ResourceManager

      以`Slot`作为资源粒度，管理`Flink`的资源提供、回收、分配。并且针对不同的集群部署：k8s, yarn , standalone 有不同的具体实现。

- Dispatcher

            提供一个`Rest`接口用于接收`Client`提交任务，并且为每一个作业启动一个独立的`JobMaster`作为一个`job`的管理者。

- JobMaster

    **JobMaster** 负责管理**单个`JobGraph`的执行（单个job的运行）**。Flink 集群中可以同时运行多个作业，每个作业都有自己的 JobMaster。

#### 1.1.3 TaskManager

     TaskManager（也称为 worker），**单Taskmanager在一个`JVM`中以`Slot`的形式执行作业流的 task**，负责缓存和交换数据流。在 TaskManager 中资源调度的最小单位是 task *slot*。TaskManager 中 task slot 的数量表示并发处理 task 的数量。flink 默认具有slot 共享的优化：把一个job 的所有 task 合并到一个slot 中。

   每个 *task slot* 会平分托管内存，共享部分内存。例如，具有 3 个 slot 的 TaskManager，会将其托管内存 1/3 用于每个 slot。同一 JVM 中的 task 共享 TCP 连接（通过多路复用）和心跳信息。它们还可以共享数据集和数据结构，从而减少了每个 task 的开销。

#### 1.1.3.1 slot 共享与 operator chain

reference:

资源分配详解：http://wuchong.me/blog/2016/05/09/flink-internals-understanding-execution-resources/

数据交换方式 ：https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/operators/overview/#physical-partitioning

**区别：**

- operator chain:

     只能把能够串联的算子并且算子间数据交换方式为:`forward`的算子进行串联。不能串联或者数据交换方式是：reblance(reblance 是web ui 逻辑执行图显示，调用函数为shuffle，或者是Rescaling )，broadcast，用户自定义，则无法串联。会引发新的task，新的task 需要新的线程进行支持。

- slot 共享：

      如图2所示，同一job内`task`共享一个slot，即使`task`之间是无法算子链接，这意味着一个slot内线程数与并发度和`sub-task`个数（task线程数）有关。


- JobGraph图(只考虑job chain 不考虑 slot share)

![ExecutionGraph](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap2-tasks_chains.svg?raw=true)

 
- ExecutionGraph 图
- 没有slot 共享的物理执行图
![TaskManagers without shared Task Slots](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap2-tasks_slots.svg?raw=true)


- slot 共享物理执行图
![TaskManagers with shared Task Slots](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap2-slot_sharing.svg?raw=true)

