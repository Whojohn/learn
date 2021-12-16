# Kafka Server 端详解

> reference:
>
> https://matt33.com/tags/kafka/
>
> 《深入理解 Kafka》
>
> https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Client-side+Assignment+Proposal
>
> 

**内部模块如下所示：**

- Broker 模型总览

![Broker内部模型](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_server_arch.svg?raw=true)

- Server 端知识点

![Kafka Server 端知识点](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_server_knowledge_point.svg?raw=true)



- 术语回顾

| **名词**                       | **解释**                                                     |
| ------------------------------ | ------------------------------------------------------------ |
| Broker                         | 又称 Kafka Server，是最基础的角色                            |
| Controller                     | 一个 Broker 中活跃着 ZK 元数据管理服务的角色                 |
| Consumer Group Coordinator     | 一个 Broker 中活跃着消费组元数据管理服务的角色               |
| Consumer Group Leader/Follower | 是消费者内部角色，Leader 负责分配组内消费者成员消费的目标分区 ID |
| Record                         | 用户消息内容，可包含 Key、Value 与消息头                     |
| Topic                          | 相当于数据库的一张表，索引关键字是 Offset，指向用户消息内容 Record |
| Compacted Topic                | 一种特殊的 Topic 类型，可以丢弃相同 Key 字段的历史版本（Key 不能为空） |
| Leader Replica                 | 是 Topic 的某一分区主副本，消费者生产的数据首先写到主副本    |
| InSync Replicas (ISRs)         | 是 Topic 的某一分区的活跃的处于同步状态的主从副本集合        |
| Log End Offset (LEO)           | 某一 Broker 中的某一 Topic 的末端日志偏移量                  |
| High Watermark (HW)            | HW 表示 ISR 集合同步的进度，ISRs 的最小 LEO，也就是木桶的最短板 |
| Leader Epoch                   | 标记主副本的 Epoch，与 HW 保障了数据一致性                   |



## 1. Controller

- 注意

1. 任何 Broker 都可以成为 Controller
2. **一个集群同时只能有一个 Controller**
3. Controller 控制的元信息基本放入到`zk`中

### 1.1 作用

1. 主题管理
2. partition leader 的选举
3. 集群 meta 信息的维护
4. broker 上下线（zk watch控制）
5. isr 信息的主动通知 broker

### 1. 2 Controller 选主流程

流程：

1. 检测`ZooKeeper`中是否存在`/Controller`节点，不存在，建立该节点当选为`Controller `。 (竞争)
2. `/controller_epoch ` 用于记录当前`Controller`的成功选举代数(每一次成功选举加一)。任何与`Controller`的交互都必须使用`controller_epoch `去标识，以辨识请求是否合法。小于该值，都会失效，认为是历史`Controller`下的交互。

### 1.3 Topic Partition Leader 选主

- 选主触发的条件

1. leader 异常 (zk 节点无法保持)
2. broker 下线 (主动告知 broker 下线，释放对应的 `Leader zk`节点 ，重新选主)
3. 副本调整时触发
4. 最优 leader 选举

- 选主的方式

1. isr 中选取第一个节点，当选为 `Leader`，更新 `zk`信息，与`Leader`进行交互。
2. 假如允许脏节点，取一个脏节点

## 2. Coordinator

### 2.1 TransactionCoordinator

> 用于控制`Kafka`写入事务，详细见`kafka`事务写入流程。

### 2.2 GroupCoordinator

> 对于非指定订阅消费(`subscribe`订阅)，都是通过`GroupCoordinator`配合`Consumer leader`控制消费组内的消费。

## 3. 网络& 处理模型(1+N+M )

![Kafka 网络&处理模型](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_server_process_model.png?raw=true)

`Kafka` 的`Server`端采用上图的模型进行处理，其中角色的作用如下:

1. 1 个 Acceptor 线程，负责监听 Socket 新的连接请求，注册了 `OP_ACCEPT` 事件，将新的连接按照 round robin 方式交给对应的 Processor 线程处理；
2. N 个 Processor 线程，其中每个 Processor 都有自己的 selector，它会向 Acceptor 分配的 SocketChannel 注册相应的 `OP_READ` 事件，N 的大小由 `num.networker.threads` 决定；
3. M 个 KafkaRequestHandler 线程处理请求，并将处理的结果返回给 Processor 线程对应的 response queue 中，由 Processor 将处理的结果返回给相应的请求发送者，M 的大小由 `num.io.threads` 来决定。



## 4. ReplicaManager

### 4. 1 作用

1. 控制分区副本同步
2. 对外提供 Log 的读写服务（如: 保证写入ack）
3. 周期性检查 isr 是否正常工作
4. 对于 partition leader ，记录所有副本的 LEO,生成 partition HW

### 4. 2 副本同步机制

> 只有`Follower`才会启动同步线程，同步线程是通过定期拉取实现，参数： `replica.fetch.wait.max.ms`控制。同步的线程并不是一个`Partition`一个线程，是共享`num.replica.fetchers`线程。

- 同步机制启动流程

1. 确保自身不在本地记录的`Leader Partition`列表中。
2. 确保自身`Partition`为`Follower`状态。
3. 假如存在与自身同步线程(拉取自身信息的线程)，关闭所有线程。
4. 对本地日志进行截断到`HW`，并进行`Checkpoint`操作。
5. 与当前的`Leader`进行同步。

### 4.3 副本同步异常处理

> 当发生 `Leader`的重新选举，`Broker`的重新上线时，都可能产生副本同步异常。

1. 假如当前本地（id：1）的副本现在是 leader，其 LEO 假设为1000，而另一个在 isr 中的副本（id：2）其 LEO 为800，此时出现网络抖动，id 为1 的机器掉线后又上线了，但是此时副本的 leader 实际上已经变成了 2，而2的 LEO 为800，这时候1启动副本同步线程去2上拉取数据，希望从 offset=1000 的地方开始拉取，但是2上最大的 offset 才是800，这种情况该如何处理呢？

> 如果 fetch offset 超过 leader 的 offset，这时候副本应该是回溯到 leader 的 LEO 位置（超过这个值的数据删除），然后再去进行副本同步，当然这种解决方案其实是无法保证 leader 与 follower 数据的完全一致，再次发生 leader 切换时，可能会导致数据的可见性不一致，但既然用户允许了脏选举的发生，其实我们是可以认为用户是可以接收这种情况发生的；

2. 假设一个 replica （id：1）其 LEO 是10，它已经掉线好几天，这个 partition leader 的 offset 范围是 [100, 800]，那么 1 重启启动时，它希望从 offset=10 的地方开始拉取数据时，这时候发生了 OutOfRange，不过跟上面不同的是这里是小于了 leader offset 的范围，这种情况又该怎么处理？

> 清空本地的数据，因为本地的数据都已经过期了，然后从 leader 的最小 offset 位置开始拉取数据。

### 4.4 ISR 处理

> `ISR`是由`Leader`控制的，`ISR` 在 `ReplicaManager`有两种检测方式。一种是周期检测，还有一种是`Follower`同步`Leader`时检测（通过`replica.lag.time.max.ms`检测）。

- Follower 拉取检测 & 更新 ISR 机制

1. 对于`Leader`而言，所有`Follower`同步时，会返回自身的`LEO`。因此，`Leader`维护所有的副本的`LEO`，并且计算该`Partition`的`HW`。
2. 对于`Follower`而言，他只有自身的`LEO`信息，没有其他`Follower`的`LEO`信息。

### 4. 5 HW 

> HW 由`Leader`生成并同步到`Follower`中，HW会定期放入到`replication-offset-checkpoint`文件中，每个`Broker`都会有这个文件。

- HW 更新

1. `Leader`通过计算`ISR`中`Follower`的最小`LEO`，这个`LEO`为`HW`。
2. `HW`控制了`Consumer`数据可见性。

### 4.6 读取

> 详细见 Consumer

### 4.7 写入

> 详细见 Producer 

## 5. LogManager

### 5.1 作用

> 负责物理存储的实现

**注意**
LogManager 只负责Log管理工作，不负责数据的写入&读取。写入读取是ReplicaManager 负责。

### 5.2 日志文件结构

#### 5.2.1 segment

> 每一个 segment 由: log 文件 ，timeindex文件，index 文件构成。

#### 5.2.2 abort index(txnindex)

> 用于事务支持，记录了abort 的事务

#### 5.2.3 leader-epoch-checkpoint （leader 特有）

```
 0 //版本号
 1 //下面的记录行数
 29 2485991681 #leader epoch ，可以看到有两位值（epoch，offset）。 
 // epoch表示leader的版本号，从0开始，当leader变更过1次时epoch就会+1
 // offset则对应于该epoch版本的leader写入第一条消息的位移。可以理解为用户可以消费到的最早数据位移。与kafka.tools.GetOffsetShell --time -2获取到的值是一致的。
```
### 5.3 日志的管理

#### 5.3.1 磁盘写入

`segment`的写入是由`log.flush.interval.messages`和`log.flush.interval.ms`控制的。

#### 清理

`segment`清理有两种：一种是删除，另一种是合并。通过`log.retention.bytes`，`log.retention.hours`控制。
