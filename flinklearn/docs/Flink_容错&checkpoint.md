# Flink_容错&checkpoint

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
  
- checkpoint 使用需要注意？
> 1. **RocksDb State Backend 默认是全量快照，会导致性能问题。**假如需要使用增量快照，需要额外配置：state.backend.incremental: true;
> 2. **RocksDb State Backend 全量模式性能比增量模式要好。**小状态，内存足够时候使用全量模式更好。
> 3. 单个`taskmanager`中`RocksDb`单个任务可用的`manager memory` = (总可用 manager memory/slot 个数)*slot 数。

- 常见的 RocksDb 配置？
```
state.backend.rocksdb.thread.num: 4
state.backend.rocksdb.predefined-options: SPINNING_DISK_OPTIMIZED
state.backend.rocksdb.writebuffer.count : 4
state.backend.rocksdb.writebuffer.number-to-merge : 2
state.backend.rocksdb.block.blocksize: 256KB
state.backend.rocksdb.compaction.level.use-dynamic-size: true
state.backend.incremental: true
state.backend.rocksdb.block.cache-size: 32MB
```
  

## 2. Checkpoint 原理

>        默认`Checkpoint`持久化是通过异步的方式实现的，以下主要讨论`exactly-once`数据一致性的保障。假如是`At least once`屏障不需要对齐。**注意，默认情况下`Checkpoint`持久化到`State Backend`是异步的，并且利用`Copy-on-Write `进一步减少阻止运算的可能。**

### 2.1 Checkpoint 原理:分布式快照

### 2.1.1 分布式快照Chandy-Lamport 算法

- Checkpoint 的完成(先不考虑图中并行度不一致的情况)
  ![数据流中的检查点障碍](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_容错&checkpoint-stream_barriers.svg?raw=true)


![数据流中的检查点Barrier](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_容错&checkpoint-stream_barriers.svg?raw=true)

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

![Unaligned Checkpoint](https://github.com/Whojohn/learn/blob/master/flinklearn/docs/pic/Flink_容错&checkpoint-unaligned-checkpoint.jpg?raw=true)


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

## 4. 端到端一致性(2PC)

> reference:
>
> https://zhuanlan.zhihu.com/p/94679136
>
> 
>
> Checkpoint 只能解决内部数据的准确性，要确保输出端也做到数据`exactly-once`的准确性，必须做到：输出源也支持幂等性操作。Flink 内部源通过`2PC`机制，只要外部源支持幂等，即可保障`exactly-once`操作。

### 4.1 Flink 中2pc 机制 & TwoPhaseCommitSinkFunction

> Flink 中输出源支持`exactly-once`必须继承`TwoPhaseCommitSinkFunction`类

- 2pc 机制

Phase 1: Pre-commit

1. `Flink`的`JobManager`从`source`开始注入`checkpoint barrier`以开启这次`snapshot`，`barrier`从`source`流向`sink`。

2. 每个进行`snapshot`的算子成功`snapshot`后,都会向`JobManager`发送`ACK`。

3. `sink`进行`snapshot`时, 向`JobManager`发送ACK的同时向`kafka`进行`pre-commit`(实际上只要当前批次都开启事务，这里不提交事务即可)。
4. 至此，`pre-commit`阶段完成。

Phase 2 : Commit

1. 当`JobManager`接收到所有算子的`ACK`后，`pre-commit`阶段完成，此时`checkpoint`也完成成功，`Jobmanager`向所有`Sink`算子发送`Commit`请求。

2. `Sink`接收到这个通知后, 就向kafka进行commit(Kafka中就是把这个checkpoint 范围内的批次事务，标记为提交)。

- 异常处理

1. 异常发生在`pre-commit`阶段

> 丢弃当前状态(`abort`)，系统恢复到最近的checkpoint，

2. 异常发生在`commit`阶段

> 此时系统已经完成`checkpoint`，将状态恢复至`checkpoint`，重复进行`commit`。**注意，commit必须成功，不然会一直发生重启，以保证数据正确。**



- TwoPhaseCommitSinkFunction 核心方法

```
	// 初始化一个事务
	protected abstract TXN beginTransaction() throws Exception;
    // 每一条数据的插入逻辑
    protected abstract void invoke(TXN transaction, IN value, Context context) throws Exception;
	// 第一阶段提交
	// 注意，数据的第一阶段提交一般插入逻辑都放入到invoke中，这里只是一个操作外部源连接的一个管理
	protected abstract void preCommit(TXN transaction) throws Exception;

	// 所有sink 第一阶段执行提交，执行第二阶段提交。假如发生异常，会调用 recoverAndCommit 方法直至成功
	protected abstract void commit(TXN transaction);
	// 默认 commit 异常恢复逻辑
    // commit 阶段异常，恢复会调用 recoverAndCommit 直至成功，因此第二阶段 commit 不能引发异常
    protected void recoverAndCommit(TXN transaction) {
        commit(transaction);
    }
	// 事务回滚
	protected abstract void abort(TXN transaction);
```

### 4.2 2pc 思考

- 2pc 为什么可靠

> 2pc 配合`checkpoint`解决了 `Flink` ,`Source`都不稳定的情况下，数据的正确性（不丢失，不多）。

- mysql 等不支持2pc 的如何保障

> 可以把2pc 阶段写统一外部源改为：先写外部文件，后写mysql。**这样做的缺点：假如mysql 插入失败，会导致任务一直卡在这个checkpoint 上不停重启。跟第一阶段失败不一样，改变了处理逻辑，也会卡住在这里。**



