# Flink-10-容错&checkpoint

## 1. 常见问题



> reference:
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/concepts/stateful-stream-processing/
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/learn-flink/fault_tolerance/
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/datastream/fault-tolerance/checkpointing/
>
> https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/ops/state/checkpoints/



- Flink 有什么容错机制(Checkpoint 作用)？

  > Flink 的容错机制底层是基于`stream replay`(流回放)和`checkpoint`实现的。`Checkpoint`定期触发算子`State`进行从内存对象到持久化保存，使得容错得以实现(假如使用 `rocksdb`对象是保存在 `rocksdb`中，而不是简单的内存中)。


- Checkpoint 失败时候重启机制(`Flink`任务失败重启机制)？

  > `none`, `off`, `disable`: 无任何重启机制 (没有checkpoint 时候的默认选项)
  >
  > `fixeddelay`, `fixed-delay`:  固定延迟 (默认`checkpoint`后策略，会一直以固定时间间隔重试，**没有重试上限**。该策略可配置重试上限)
  >
  > `failurerate`, `failure-rate`: 故障率重启 (当重启失败率达到一定值时，停止重启)
  >
  > `exponentialdelay`, `exponential-delay`:  延迟重启(一直重试重启，每一次失败会以指数增加重启等待时长)

- Checkpoint 底层（Checkpoint 与 State 关系，State 与数据类型关系）？

    分布式快照 -> `state`+`state backed`； **`State`  中也包含了数据类型信息。**


- 不同的状态后端对于`Checkpoint` (`State`)的处理不同(state )？ 

      `HashMapStateBackend ` 或 `EmbeddedRocksDBStateBackend`持久化都是基于`hdfs`等外部状态端，不同的是前者对象在内存中，后者是通过 `Rocksdb`存储在内存和本地磁盘中。 

- `Batch`模式有`Checkpoint`吗？或者其他容错机制？

  `Batch`模式只有一种容错机制就是整个容器完全重启。

## 2. Checkpoint 原理

>        默认`Checkpoint`持久化是通过异步的方式实现的，以下主要讨论`exactly-once`数据一致性的保障。假如是`At least once`屏障不需要对齐。**注意，默认情况下`Checkpoint`持久化到`State Backend`是异步的，并且利用`Copy-on-Write `进一步减少阻止运算的可能。**

### 2.1 Checkpoint 原理:分布式快照

### 2.1.1 分布式快照Chandy-Lamport 算法

- Checkpoint 的完成(先不考虑图中并行度不一致的情况)
![数据流中的检查点障碍](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Cap10-stream_barriers.svg?raw=true)


![数据流中的检查点Barrier](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Cap10-stream_barriers.svg?raw=true)

         如上图所示，`Checkpoint` 整体控制流程由`Jobmanager`触发和最终确认提交。基础步骤如下：

          1.  `Checkpoint`机制由`Jobmanger`通过数据源中插入`barrier`标识。
          2.  `TaskManger`中的算子接收到了`Checkpoint`后(`Barrier`标识)，假如使用了`State`，则将当前的状态`Flush`到`State backend`中，成功后通知`TaskManger`写入成功(假如使用`Jobmanager`是`Ha`会把这个这个信息写入到如：`Zookeeper`，`etcd`中)， 并且向后依次传递`Barrier`标识，让下游算子完成持久化的过程。
          3.  重复执行以上步骤，当`Jobmanger`收到所有`TaskManager`通知`State`写入`State Backend`，此时`Checkpoint`完成。

- Checkpoint 恢复

       `JobManger`找到最后一个完整的`Checkpoint`，把`Checkpoint`信息恢复到 `Jobmanger`中，`Jobmanger` 以这个信息重启任务，各个`Slot`按照`Jobmanger` 下发的信息信息，读取`State`恢复到本地，任务重启成功。

#### 2.1.2 并行度不一致&数据重分区&多数据源问题(Barrier 对齐机制)

- 并行度不一致 & 数据重分区 & 多数据源存在的问题

         `Checkpoint barrier` 是由数据源产生的，假如出现以上情况，会导致一个算子，接收的数据是由含有多个数据源`Barrier`，为了实现数据的`exactly-once ` 机制。必须保证所有`Barrier`恰好到来才执行`State`持久化，不然数据会多算。



![Aligning data streams at operators with multiple inputs](https://nightlies.apache.org/flink/flink-docs-release-1.14/fig/stream_aligning.svg)

- Barrier 对齐机制

        如上图，`state`持久化的操作必须等待`e`数据前的数据到来才可以操作。同时也必须停止`1`数据源的消费(假如`At least once` `1`数据源继续保持消费)，直至`e` 到来，这个过程就叫做`Barrier 对齐`。**注意，一般来说，假如下游不存在重新数据分区，对齐后的下游，没有对齐这个消耗。**

- Barrier 对齐机制缺陷

       Barrier 对齐必须停止某一路数据源的消费，会增加耗时。特别在存在数据倾斜时，系统吞吐会进一步降低。

## 2.2 Unaligned Checkpoint 原理

> reference:
>
> https://developer.aliyun.com/article/768710



> Flink 为了解决对齐间源相差大的问题，实现了`Unaligned Checkpoint`。

![Unaligned Checkpoint](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/cap10-unaligned-checkpoint.jpg?raw=true)


- Unaligned-Checkpoint 实现方式

1. 图 a: 输入 Channel 1 存在 3 个元素，其中 2 在 Barrier 前面；Channel 2 存在 4 个元素，其中 2、9、7在 Barrier 前面。输出 Channel 已存在结果数据 1。

2. 图 b: 算子优先处理输入 Channel 1 的 Barrier，开始本地快照记录自己的状态，并将 Barrier 插到输出 Channel 末端。

3. 图 c: 算子继续正常处理两个 Channel 的输入，输出 2、9。同时算子会将 Barrier 越过的数据（即输入 Channel 1 的 2 和输出 Channel 的 1）写入 Checkpoint，并将输入 Channel 2 后续早于 Barrier 的数据（即 2、9、7）持续写入 Checkpoint。

- Unaligned-Checkpoint 缺点

1. 由于要持久化额外的缓存数据，State Size 会有比较大的增长，磁盘负载会加重。
2. 随着 State Size 增长，作业恢复时间可能增长，运维管理难度增加。

## 3.Checkpoint 配置

```
// checkpoint 执行间隔
env.enableCheckpointing(1000);
// HashMapStateBackend\ EmbeddedRocksDBStateBackend
env.setStateBackend();

// checkpoint 模式
env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
// checkpoint 最小间隔
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
// checkpoint 超时时间
env.getCheckpointConfig().setCheckpointTimeout(60000);
// 最大checkpoint 失败容忍次数
env.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);
// checkpoint 并发数，大于1用于checkpoint 需要外部io 等耗时场合
env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);
// task 取消时候保留checkpoint
env.getCheckpointConfig().enableExternalizedCheckpoints(
    ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
// 使用 unaligned checkpoints
env.getCheckpointConfig().enableUnalignedCheckpoints();
// 配置持久化状态后端
env.getCheckpointConfig().setCheckpointStorage("hdfs:///my/checkpoint/dir")
// task 完成时候保留checkpoint
Configuration config = new Configuration();
config.set(ExecutionCheckpointingOptions.ENABLE_CHECKPOINTS_AFTER_TASKS_FINISH, true);
env.configure(config);
```

