# 消费者

>  reference:
>
>  https://kafka.apache.org/26/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html

![kafka架构](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_consumer_知识点.svg?raw=true)


注意：
1. `KafkaConsumer`不是一个线程安全的对象，内部利用`AtomicLong`进行并发，`kafka` 内部提供`consumer.wakeup()`，用户要并发使用`KafkaConsumer`必须调用该方法。

   

## 1. 消费者运行流程

- subscribe 调用阶段

**处理工作：**

1. 该阶段主要负责按照`Kafka`的消费者的初始化，**注意只有调用poll方法才会正式建立连接。**
2. 会初始化`Kafka`消费者信息：`事务等级`；
3. 初始化`SubscriptionState`信息消费订阅模式(`AUTO_TOPICS(指定topic，自动分区)`，`AUTO_PATTERN（正则匹配topic，自动分区）`，`USER_ASSIGNED`(用户定义topic和分区方式))。



- poll 调用阶段

> reference:
>
> https://greedypirate.github.io/2020/03/10/Kafka%E6%B6%88%E8%B4%B9%E8%80%85-%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90(%E4%B8%8A)/#poll%E6%96%B9%E6%B3%95

**处理工作：**

1. updateAssignmentMetadataIfNeeded： 负责初始化 `Coordinator`，检测`Reblance`，初始化各个分区的`Offset`信息，记录`Group`消费位置，更新可读`offset`。假如初始化没有`Group`消费信息，`auto.offset.reset`获取。
2. 拉取消息：尝试从本地缓存拉取信息，没有则从远程定期尝试拉取。
3. 提前发起异步的下一轮次的数据获取。
4. 假如使用了消费者拦截器，使用拦截器。
5. 返回数据。

**核心代码**：

```
public ConsumerRecords<K, V> poll(final Duration timeout) {
    return poll(timeout.toMillis(), true);
}
private ConsumerRecords<K, V> poll(final long timeoutMs, final boolean includeMetadataInTimeout) {
    // 检测线程安全，标识当前线程正在消费，防止并发
    acquireAndEnsureOpen();
    try {
        if (timeoutMs < 0) throw new IllegalArgumentException("Timeout must not be negative");

        if (this.subscriptions.hasNoSubscriptionOrUserAssignment()) {
            throw new IllegalStateException("Consumer is not subscribed to any topics or assigned any partitions");
        }

        // poll for new data until the timeout expires
        // 记录消耗的时间，防止超时
        long elapsedTime = 0L;
        do {

            client.maybeTriggerWakeup();

            final long metadataEnd;
            // 新版本的poll是true，就是说是否要把更新Metadata的时间，也算在poll的超时时间内
            if (includeMetadataInTimeout) {
                final long metadataStart = time.milliseconds(); // SystemTime
                /**
                 * 初始化Coordinator，初次rebalance，初始化每个分区的last consumed position
                 * 什么情况下返回false：
                 * 1. coordinator unknown
                 * 2. rebalance失败(长时间拿不到响应结果，发生不可重试的异常)
                 * 3. 获取不到分区的last consumed position (fetch offset)
                 */
                if (!updateAssignmentMetadataIfNeeded(remainingTimeAtLeastZero(timeoutMs, elapsedTime))) {
                    // coordinator不可用或者...
                    return ConsumerRecords.empty();
                }
                metadataEnd = time.milliseconds();
                elapsedTime += metadataEnd - metadataStart; // += (metadataEnd - metadataStart)
            } else {
                // 老版本的超时时间？
                while (!updateAssignmentMetadataIfNeeded(Long.MAX_VALUE)) {
                    log.warn("Still waiting for metadata");
                }
                metadataEnd = time.milliseconds();
            }

            final Map<TopicPartition, List<ConsumerRecord<K, V>>> records = pollForFetches(remainingTimeAtLeastZero(timeoutMs, elapsedTime));

            if (!records.isEmpty()) {
                /**
                 * 立即开始下一轮请求，和用户处理消息并行
                 */
                if (fetcher.sendFetches() > 0 || client.hasPendingRequests()) {
                    client.pollNoWakeup();
                }
                // 拦截器处理后的消息才交给用户
                return this.interceptors.onConsume(new ConsumerRecords<>(records));
            }
            // records为空
            final long fetchEnd = time.milliseconds();
            // 总的消耗时间 fetchEnd - metadataEnd是真正用来发fetch请求的所消耗的时间
            elapsedTime += fetchEnd - metadataEnd;

        } while (elapsedTime < timeoutMs);

        return ConsumerRecords.empty();
    } finally {
    // 
        release();
    }
}
```



   

![poll方法](https://pic.downk.cc/item/5ea58054c2a9a83be509e638.png)

## 2. Consumer 分区订阅方式

       Kafka 支持三种订阅方式: `AUTO_TOPICS(指定topic，自动分区)`，`AUTO_PATTERN（正则匹配topic，自动分区）`，`USER_ASSIGNED`(用户定义topic和分区方式)；用户可以通过传入分区指定方式，控制消费者读取的分区。自定义分区不受`Coordinator`控制，即`Reblance`无效。

     **只有自定义分区才能保障消费 `Exactly-once`，但是自定义分区，分区的增加等处理逻辑都需要用户实现。**

```
// AUTO_TOPICS
public void subscribe(Collection<String> topics)
// AUTO_PATTERN
public void subscribe(Pattern pattern)
// USER_ASSIGNED
void assign(Collection<TopicPartition> partitions)
```

## 3. 事务 isolation.level

isolation.level： READ_COMMITTED(读已经提交) ；READ_UNCOMMITTED （读未提交）



## 4. 消费语义

Kafka 消费者根据不同使用方式，可以分为： `exactly one `， `at-least-once`

### 4.1 自动提交 offset （**At-most-once**）

> 最不靠谱的使用方式
>
> 假如数据被拉取，但是没有处理(只是放入到内存)，但是后台 offset 在`Poll`的时候已经`Commit`了，此时消费者异常退出，丢失内存中的数据。
>
> 假如组平衡，消息会被重复消费。

```
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaConsumerDemo {
    /**
     * 连接初始化
     *
     * @param <K> key类型
     * @param <V> value 类型
     * @return 返回一个 Producer<k,v>对象
     */
    private static <K, V> KafkaConsumer<K, V> iniConsumer(Map<String, Object> temp) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "test:9093");
        props.put("acks", "all");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        if (temp != null) temp.forEach(props::put);
        // 新建一个连接
        return new KafkaConsumer<>(props);
    }

    public static void autoCommitConsumer() {
        Consumer<String, String> consumer = iniConsumer(new HashMap<String, Object>() {{
            put("group.id", "test2");
            put("auto.offset.reset", "earliest");

        }});
        consumer.subscribe(Collections.singletonList("test"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, String> record : records) {
            System.out.println(record.offset() + ": " + record.value());
        }
    }

    public static void main(String[] args) {
        KafkaConsumerDemo.autoCommitConsumer();
    }
}
```

### 4.2 手动提交 offset(**At-least-once**)

> 手动控制当且仅当数据处理完成后才提交对应的`Offset`，保障了数据至少被消费一次。注意由于`Subscribe`不是自定义，可能存在消费完成，因为消费组`Reblance`提交`Offset`失败(只能放弃提交)，重新进入组，导致消费重复的现象。

```
public static void manualCommitConsumer() {
        Consumer<String, String> consumer = iniConsumer(new HashMap<String, Object>() {{
            put("group.id", "test1");
            put("auto.offset.reset", "earliest");
            put("enable.auto.commit", "false");
        }});
        consumer.subscribe(Collections.singletonList("test"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

        for (ConsumerRecord<String, String> record : records) {
            System.out.println(record.offset() + ": " + record.value());
            consumer.commitSync(new HashMap<TopicPartition, OffsetAndMetadata>() {{
                put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1));
            }});
        }
        consumer.close();
}
```

**订阅是自动分区，存在缺陷，会引发退出异常**

```
Exception in thread "main" org.apache.kafka.clients.consumer.CommitFailedException: Offset commit cannot be completed since the consumer is not part of an active group for auto partition assignment; it is likely that the consumer was kicked out of the group.

```

### 4. 3 手动提交 offset + 用户定义订阅 + Read commit （Exactly-once）

>      注意：**真实的 Exactly-once 必需处理端自己保存 offset (用于奔溃时恢复)**，自己确保崩溃时候内部处理不出现内部的数据重复消费，或者重复消费具有幂等性 。一般情况下，`Ecactly-once`还会配合事务，进一步保障读取的正确性(这里跟 Exactly-once 没有关系，但是假如读未提交，用户多次重试提交，最终提交成功，业务层面上数据是出现了重复的，`Consumer`层面事务就是避免重复，实现业务层面的`Exactly-once`)。
>
>     下面是简化版本的 Exactly-once 不考虑处理端奔溃恢复等情况，首次启动写死 offset。

```
public static void manualCommitExactlyOnceConsumer() {
        Consumer<String, String> consumer = iniConsumer(new HashMap<String, Object>() {{
            put("group.id", "test1");
            put("auto.offset.reset", "earliest");
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }});
        consumer.assign(new ArrayList<TopicPartition>() {{
            add(new TopicPartition("test", 0));
        }});
        consumer.seek(new TopicPartition("test", 0), 10l);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));


        for (ConsumerRecord<String, String> record : records) {
            System.out.println(record.offset() + ": " + record.value());
            consumer.commitSync(new HashMap<TopicPartition, OffsetAndMetadata>() {{
                put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1));
            }});
        }

        consumer.close();
}
```

##  5. 心跳 & Poll 

> reference:
>
> https://chrzaszcz.dev/2019/06/kafka-heartbeat-thread/
>
> https://cwiki.apache.org/confluence/display/KAFKA/KIP-62%3A+Allow+consumer+to+send+heartbeats+from+a+background+thread

**注意：**

1. **心跳只有使用非用户订阅分区才会出现(使用`subscribe`时候才会出现)。**
2. **心跳默认是一个后台定时启动线程，与消费没有任何关系，心跳不存在，说明`Consumer`必定异常。**
3. **心跳只是能用于检测`Consumer`实例与`coordinator`是否同时正常快速检查机制，利用Poll 检测时效性太低。心跳不存在（Consumer实例退出），必定引发重平衡。如下图所示。**
4. **`Consumer`与 `Coordinator`之间还能利用心跳进行`Reblance`的控制，退出消费组等消息发送。**
5. **哪怕心跳正常，`Poll`频率没法维持一样会导致重平衡。**（我们一般遇到重平衡更多的是这种情况）

![kafka 消费者和消费者，poll与心跳模型](https://chrzaszcz.dev/img/2019-06-kafka-heartbeat-thread/heartbeat-new-way.png)

- 心跳相关配置

heartbeat.interval.ms : 心跳线程定期发送时间间隙，一般为 `session.timeout.ms`的三分之一。

session.timeout.ms：心跳超时时间。

- poll 相关配置

max.poll.interval.ms:  拉取间隙超时时间，假如超出这个时间，没有 `Poll` 行为，触发`Reblance`

max.poll.records: 每次拉取条数（过大的拉取数，会导致拉取间隙超时，因为消费速度过慢，但是拉取频率低于超时拉取间隙）。



## 6. 消费组

> reference :
>
> http://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/Kafka%E6%A0%B8%E5%BF%83%E6%8A%80%E6%9C%AF%E4%B8%8E%E5%AE%9E%E6%88%98/17%20%20%E6%B6%88%E8%B4%B9%E8%80%85%E7%BB%84%E9%87%8D%E5%B9%B3%E8%A1%A1%E8%83%BD%E9%81%BF%E5%85%8D%E5%90%97%EF%BC%9F.md
>
> https://dunwu.github.io/bigdata-tutorial/kafka/Kafka%E6%B6%88%E8%B4%B9%E8%80%85.html#_1-1-%E6%B6%88%E8%B4%B9%E8%80%85

- 名词解释

1. coordinator (GroupCoordinator ) : 消费组中消费者与`Broker` 中协调者进行沟通，沟通者为`Coordinator`。**他负责 `Reblance`，组成员管理，`Kafka 2`中`Consumer offset`保存等问题。**

2. consumer leader： 

3. consumer follow：
4. **__consumer_offsets ： 一个特殊的`Topic`，`Kafka 2`用于存放`Consumer offset`。格式：`<Group ID，主题名，分区号 >`。**

### 6.1 分区分配模式(只有使用 subscribe 订阅才会发生)

RangeAssignor（默认值） 

RoundRobinAssignor 

 StickyAssignor

### 6.2 Reblance

>  reference :
>
> https://dunwu.github.io/bigdata-tutorial/kafka/Kafka%E6%B6%88%E8%B4%B9%E8%80%85.html#_1-%E6%B6%88%E8%B4%B9%E8%80%85%E7%AE%80%E4%BB%8B   

   分区的消费者从一个消费者转移到另一个消费者，这样的行为被称为**分区再均衡（Rebalance）**。**Rebalance 实现了消费者群组的高可用性和伸缩性**。任何新增节点，节点退出，节点异常，分区数增加，消费者新增消费`Topic`等都会引发`Reblance`。

- Reblance 流程

1. 选举 `consumer leader`（第一个加入 Consumer Group 的消费者）
2. 消费者通过向被指派为群组协调器（Coordinator）的 Broker 定期发送心跳来维持它们和群组的从属关系以及它们对分区的所有权。

3. `consumer leader` 从`coordinator `那里获取群组的活跃成员列表，并负责给每一个消费者分配分区。将分配结果发送到`coordinator`。
4. `coordinator`把信息发送给消费者。每个消费者只能看到自己的分配信息，只有`consumer leader`知道所有消费者的分配信息。



- coordinator 的路由（consumer 如何找到 coordinator，定位 coordinator 能够查看对应 topic 的消费日志）

1. 第 1 步：确定由位移主题的哪个分区来保存该 Group 数据：`partitionId=Math.abs(groupId.hashCode() % offsetsTopicPartitionCount)`。
2. 第 2 步：找出该分区 Leader 副本所在的 Broker，该 Broker 即为对应的 Coordinator。

> 如： group.id = "test-group" -> hashCode = 627841412 -> __consumer_offsets分区数统计 ->  627841412 %__ consumer_offsets分区数统计 = 12;  则 `__consumer_offsets` 第12个分区所在的`Leader`为`coordinator `。

### 6.3 Kafka 如何存放 offset

- kafka 1 

offset 存放于`Zookeeper`中

- kafka 2

offset 存放于`__consumer_offsets topic` 中

### 6.4  Offset 提交方式

- 异步提交

不会进行重试，因为异步可能发生提交乱序问题，用户必须自行解决乱序发送问题

- 同步提交

默认会自动进行重试