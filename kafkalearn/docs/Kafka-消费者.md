# 消费者

reference:

> https://kafka.apache.org/26/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html

注意：

1. `KafkaConsumer`不是一个线程安全的对象，内部利用`AtomicLong`进行并发

   

### 消费者运行流程

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

1. updateAssignmentMetadataIfNeeded： 负责初始化 `Coordinator`，检测`Reblance`，初始化各个分区的`Offset`信息，记录`Group`消费位置，假如初始化没有`Group`消费信息，`auto.offset.reset`获取。
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

## 自动提交 offset &手动提交 offset & Kafka 消费语义

Kafka 消费者根据不同使用方式，可以分为： `exactly one `， `at-least-once`

### 自动提交 offset （**at-least-once**）

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

### 手动提交 offset

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
        }
        // 提交当前消费的 offset
        consumer.commitSync();
        // consumer.commitSync(new HashMap<>()); 可以通过传入各个分区的 offset 数，控制提交 offset
        consumer.close();
    }
```

## Consumer 分区订阅方式

       Kafka 支持三种订阅方式: `AUTO_TOPICS(指定topic，自动分区)`，`AUTO_PATTERN（正则匹配topic，自动分区）`，`USER_ASSIGNED`(用户定义topic和分区方式)；用户可以通过传入分区指定方式，控制消费者读取的分区。自定义分区不受`Coordinator`控制，即`Reblance`无效。

```
public void subscribe(Collection<String> topics)
public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener)
public void subscribe(Pattern pattern, ConsumerRebalanceListener listener)
public void subscribe(Pattern pattern)
```

## Consumer isolation.level

isolation.level： READ_COMMITTED(读已经提交) ；READ_UNCOMMITTED （读未提交）





