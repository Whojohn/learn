# 幂等与事务

> reference:
>
> https://cwiki.apache.org/confluence/display/KAFKA/KIP-98+-+Exactly+Once+Delivery+and+Transactional+Messaging (producer 实现方式)
>
> https://docs.google.com/document/d/1Rlqizmk7QCDe8qAnVW5e5X8rGvn6m2DCR3JR2yqwVjc/edit#heading=h.hy1bo4rg9ud2   (consumer 事务实现方式)
>
> https://cwiki.apache.org/confluence/display/KAFKA/Transactional+Messaging+in+Kafka#TransactionalMessaginginKafka-Abortingatransaction (kafka 事务总结，错误处理逻辑已经是过时的，不准确，但是设计思路还是那样)
>
> http://www.heartthinkdo.com/?p=2040#43
>
> http://matt33.com/2018/11/04/kafka-transaction/#Producer-Fencing

**总结：**

1. 幂等只能解决网络异常等问题，引发重试导致的数据重复问题。
2. 事务能保证`Exactly once`，事务依赖于: 幂等(关闭幂等，事务会失败)。
3. 幂等实现：``TransactionCoordinator`生成PID(Producer 唯一标识)，对比`PID`的`Sequence Number`实现。**注意 PID 由`Producer`生成，每个`Prodcuer`实例生成的`Pid`不唯一。**
4. 事务实现：**生产者实现：**依赖`TransactionCoordinator` 协调事务管理，配合用户提供固定的`TransactionnalId`配合`epoch`控制单实例，`__transaction_state` 记录事务信息(`TransactionCoordinator` 和 `Leader`切换时提供恢复状态)，`LSO`控制事务机制下的最后可见数据，`Abort Transaction Index` 写入`abort范围数据`实现。**消费者实现** ：依赖于`LSO`，`Abort Transaction Index`实现。**注意区分 PID 和 ``TransactionnalId`。只有`TransactionnalId`才具有唯一性，才能实现生产者奔溃后，重启恢复上次的事务。`PID`在`Kafka`中只能解决幂等，但是`Kafka`中事务必须要求开启幂等。**
5. 事务的状态： 对于外部`consumer`来说只有**`begin/end/abort`** 三种状态。内部来说，是`2pc`控制的。单个`TransactionId`同时只能有一个实例，假如多个实例，只有最后一个实例能够实例成功，`epoch`是用来控制实例并发，每一个新实例都会引发`epoch`加一。
6. **生产者使用事务必须注意：`transaction.timeout.ms`期间内必须提交一次事务，不然会导致实例会话过期，无法进行任何操作。`transaction.timeout.ms` < `transaction.max.timeout.ms` **。`max.block.ms`可以控制`commit`等超时时长。
7. 事务失败处理规则(**注意TransactionId不能改变**)：

> >      一般事务操作的流程是： commit 异常时(broker 异常引发的超时)： begin -> send -> commit -> abort ； commit 没有异常时： begin -> send -> abort;  假如是 `producer` 超时：即一个事务`transaction.timeout.ms`时间内无法提交，必须新建实例，`abort`旧事务(`abort`可选)。
>
> 1. `Producer`发送 BeginTransaction 时超时或错误响应：生产者只需使用相同`TransactionId`重试。
> 2. `Producer`发送数据时的`Broker`异常：  一般重试`commit`即可。
> 3. `Producer`发送数据时，单次**提交间隔**超过`transaction.timeout.ms`：必须结束该处理过程，销毁实例。新建实例，然后`abort`，再次执行逻辑。
> 4. 生产者发送 CommitTransaction(TxId) 时超时或错误响应：一般重试`commit`即可。
> 5. 网络断开时时生产者发送失败：如果协调器能够检测到网络的关闭（回应 BeginTransactionRequest 时发现网络断开），那么它可以主动中止事务。否则事务将在超时期限后中止。
> 6. 协调器故障 （用户无需处理）：当协调器移动到另一个代理时（即，日志分区的领导移动）。协调器将从最后一个检查点的硬件扫描日志。如果在 PREPARE_COMMIT 或 PREPARE_ABORT 中有任何事务，新协调器将重做 COMMIT 和 ABORT。请注意，协调器宕机时正在进行的事务不一定需要中止——即，生产者可以将其 CommitTransactionRequest 发送给新的协调器。

## 幂等

**幂等操作 vs Kafka中的幂等**

幂等操作：对于同一个接口，同一份数据的请求，无论被是否被重复执行，当执行完成最终数据都是一致的。比如：同一份交易数据，最终的总额是确定且唯一(`Exactly once`语义)。

kafka中的幂等：kafka 中的幂等只能保障一个`Topic`单`Partition`中的**单次消息不重复，不丢失。无法做到一组数据的不重复，不丢失，且满足要么全部成功，要么全部不写入(exactly once语义)**。比如没有事务，当数据处理一半时，幂等只能保证前面的数据写入没有重复，但是不能保障**数据要么全部写入成功，要么不写入。**

### 幂等解决的问题

生产者在生产过程中，会因为网络问题，重复发送数据，引发`Partition`中存放的数据重复。幂等解决了因为网络等以外导致重试引发的数据重复问题。

### 幂等的实现方式

为了实现Producer的幂等性，Kafka引入了`Producer ID`（即`PID`）和`Sequence Number`。

- PID:  每个新的`Producer`在初始化的时候会被分配一个唯一的`PID`，每一次实例化得到的`PID`不唯一。
- Sequence Number:  对于每个`PID`，该`Producer`发送数据的每个`<Topic, Partition>`都对应一个从`0`开始单调递增的`Sequence Number`。

**幂等实现机制**

`Broker`中记录当前的`PID`对应的`Seq ID`，消息写入执行以下判定：

1. 只有`Consumer Seq ID`写入等于`Broker Seq ID`+1 才允许写入。
2. 当接受到`Consumer Seq ID`比当前记录要小的信息，不写入。
3. 当`Consumer Seq ID` > `Broker Seq ID`+1，**抛出`OutOfOrderSequenceException`异常**，会引发整个事务的异常。

## 事务

### 事务解决的问题

多分区的写入满足原子性，要么成功，要么失败，不存在部分写入的情况。

### 事务实现的方式

为了实现事务，`Kafka`引入`TransactionCoordinator`， `__transaction_state`， `LSO`， `epoch`：

**TransactionCoordinator**：用于处理事务逻辑，每个生产者都会指派给特定的`TransactionCoordinator `，事务逻辑和`PID`由`TransactionCoordinator`控制。

**__transaction_state**: 存放事务信息相关的信息。

**TransactionnalId**：用户指定，事务的唯一标识，由用户产生。因此，不同`Producer`建立的事务标识是唯一的。

**epoch**：自增数字，用于标识单个事务实例。多个实例使用相同的`TransactionnalId`作为事务标识，`epoch`是标识当前哪个实例有效(多实例下，`epoch`最大的才能工作，其他都会退出)。(报错是：`XXXFencingException`，都是`epoch`异常)

**LSO**: Broker在缓存中维护了所有处于运行状态的事务对应的`offsets`,`LSO`的值就是这些`offsets`中最小值-1。这样在`LSO`之前数据都是已经`commit`或者`abort`的数据，只有这些数据才对`Consumer`可见，即`consumer`读取数据只能读取到`LSO`的位置。**注意 LSO 为内存对象，由生成者生成，计算方式如下**：



**事务状态**

![offset与事务状态表](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_LSO_transaction_commit_status.png?raw=true)


**通过事务状态计算而得的 LSO**

![LSO表](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_LSO_transaction_commit_status.png?raw=true)





**事务实现机制(对于生产者而言)**

#### 生产者实现方式

![生产者事务流程](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transtion_producer_work_flow.png?raw=true)


1. 找到`TransactionCoordinator `。
2. `Producer`获取`PID`，并且维护一个**自增的`epoch`**。注意`PID`是必须的，因此，开启事务，默认也必须开启幂等，否则无法获取`PID`。
3. `transactionID` , `PID`  ,  `producer_epoch` ， `topic`等信息被`TransactionCoordinator`写入到`__transaction_state`中。
4. 本地开启事务，注意这里不会与`TransactionCoordinator`交互。
5. 执行`send` 发送操作：假如是第一次发送。`__transaction_state`，发送`TransactionalId PID Epoch [Topic [Partition]]`，事务状态设置为`begin`。发送数据到对应的`partition`中，发送`TransactionalId PID Epoch ConsumerGroupID` 到 `TransactionCoordinator`中。
6. 事务提交时候，发送`CoorinadorEpoch PID Epoch Marker [Topic [Partition]]`给`TransactionCoordinator`。`TransactionCoordinator`负责修改事务信息。





**TransactionCoordinator 路由方式(生产者)**

> 因为 `TransactionCoordinator `用于控制事务，所以生产者必须要先确定`TransactionCoordinator `的位置，类似`GroupCoordinator`的控制逻辑。

公式： **HASH**(`coordinator_key`（`transactionId`) % `__transaction_state Partition个数`  =  目标 `Partition` ，然后利用找到`目标Partition`所在的`Leader`即为`TransactionCoordinator `所在的机器。



#### 消费者实现方式

> `LSO` 控制 Consumer 在事务中允许读取的最后一条数据， `Aborted Transaction Index ` 通过标记区间数据是`abort`，`consumer` 端自行过滤这个范围的数据(基于`pagecache` 的`zero copy`机制，必须客户端实现过滤，解压)

**LSO**: Broker在缓存中维护了所有处于运行状态的事务对应的`offsets`,`LSO`的值就是这些`offsets`中最小值-1。这样在`LSO`之前数据都是已经`commit`或者`abort`的数据，只有这些数据才对`Consumer`可见，即`consumer`读取数据只能读取到`LSO`的位置。 (解决事务尚为提交(abort  commit)，前数据对于开启了事务的`consumer`来说不可见)。 **LSO为内存对象，并且由生成者维护，消费者只进行读取**

**Aborted Transaction Index (xxx.txnindex)**： 记录`abort` 的事务，用于`consumer`读取数据时过滤事务，**假如没有`abort`事务，不会生成该文件。**



- **消费者实现方式**

**1.** 根据`LSO`确定读取数据的范围，丢弃大于`LSO`范围的数据。(如下图)

   

**Kafka 中 Log 数据**

![LOG 中数据](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_log_satus.png?raw=true)


**LSO状态**

![LSO](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_LSO_satus_in_log.png?raw=true)


**抛弃大于 LSO 的数据**

![抛弃大于 LSO 的数据](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_consumer_read_by_LSO.png?raw=true)

**2.** 拉取`Aborted Transaction Index `文件，过滤丢弃的消息。如下图(假如丢弃 2 和 4)：

**Aborted Transaction Index 文件**

![abort 文件](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_abort_index_file.png?raw=true)

**按照abort 文件抛弃后数据**

![抛弃abort的数据](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transaction_abort_index_file.png?raw=true)



### 总结
![事务消费和生产例子](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_transtion_with_consumer_producer.png?raw=true)

