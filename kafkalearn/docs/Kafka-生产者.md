# Kakfa-生产者

> reference:
> <<深入理解Kafka：核心涉及与实践原理>>
>
> <<kafka 核心技术与实战>>
>
> https://kafka.apache.org/26/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html
>
> https://kafka.apache.org/26/javadoc/org/apache/kafka/clients/producer/ProducerInterceptor.html

注意
1. 一般情况下broker 处理的是由多条数据组合成的数据集(虽然一条数据对应offset加一)。
2. `Kafka`数据可靠性需要`acks`保障，0时候，无需任何`replace` 成功即标为成功。1时，必须保障`leader`的写入成功，`all`或者`-1`时，必须满足`min.insync.replicas`最小`in-sync`写入副本。


## 1. 生产者运行流程

![生产者运行流程](https://github.com/Whojohn/learn/blob/master/kafkalearn/docs/pic/kafka_%E7%94%9F%E4%BA%A7%E8%80%85%E8%BF%90%E8%A1%8C%E6%B5%81%E7%A8%8B.png?raw=true)




     如上图所示，`Kafka`中客户端生成者有两个主要线程一个是调用`send`前的**用户主线程**，还有一个是负责发送的`Sender`线程。然后数据按照以下组件依次进行流动

- ProducerRecord：将用户的数据以 key / value 的方式进行组织，且带有`Header`和`Timestamp`等特殊信息。多个`Producer`组成消息集合，消息集合有`v1`,`V2`两个版本，集群内部自动版本兼容，**版本兼容引发的转换会导致性能下降**。

  > `Timestamp`：支持配置：`CreateTime `(`Producer `端创建时间，或者是用户定义时间)和`LogAppendTime`（`Broker`写入到`Log Segment`时间）。当使用`CreateTime`时，`Kafka broker`只接受`max.message.time.difference.ms`和本地时间范围内的时间戳。
  >
  > Header：提供给用户用户指定特殊信息。

- KafkaProducer：线程安全`kafka topic`连接实例(单例需要自行实现，线程安全是`RecordAccumulator` 本质是`ConcurrentMap`)。
- ProducerInterceptor :  拦截器可以使用多个，可以用于规则过滤，统一修改信息，统计失败次数等。**该类不保证线程安全，用户必须保障线程安全(累加等有共享变量场合)**

>ProducerInterceptor 包括三个主要方法：
>
>```java
># 发送前执行
>public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record);
># 发送成功或者发送失败时候执行
>public void onAcknowledgement(RecordMetadata metadata, Exception exception);
># 拦截器退出时执行
>public void close();
>```



- Serializer：序列化类，负责。
- Partitioner ：假如`ProducerRecord`中指定了`partition`字段，则`Partitioner`失效。默认：`key`不为`null`进行`hash`计算（修改了 `Partition`数会导致映射不一致。）。 `Key`为`null`轮询发送到各个分区。

- RecordAccumulator：缓存消息供`Sender` 线程批量发送，减少单次网络传输的资源消耗，通过`buffer.memory`控制总缓存大小。单个`Sender`发送批次由`batch.size`和`linger.ms`控制，满足任意一个条件触发数据发送。**batch是一个特殊优化的缓存，利用数组对象复用，减少GC，！！！但是！！！能够使用batch的前提是一条数据必须小于batch.size，对于大于batch.size必须新建对象。因此，对于长文本，提高batch.size能够减少`GC`消耗。**数据按照分区存放在`双端队列中`，数据在这里表示为: <分区, Deque< ProducerBatch>> （Deque 就是 buffer.memory，ProducerBatch 就是 batch.size 控制）。**ProducerBatch 在开启压缩的情况下，会对序列化后的数据进行压缩。RecordAccumulator 数据存储底层是ConcurrentMap<TopicPartition, Deque<ProducerBatch>>,插入时候`synchronized`修饰`Deque`对象,因此保障了生产者插入的线程安全**


- Sender：负责转换`partition`到`broker`地址并发送数据。`Sender` 从 `RecordAccumulator` 中获取缓存的消息之后，会进一步将原本<分区, Deque< ProducerBatch>> 的保存形式转变成 <Node, List< ProducerBatch> 的形式，其中 Node 表示 Kafka 集群的 broker 节点(逻辑分区到物理连接的转换)。
- InFlightRequests：存放缓存了已经发出去但还没有收到响应的请求，并且提供`LeastLoadedNode`用于元数据定期更新。



## 2. 同步/异步写入&写入回调&事务写入

幂等vs事务

> 幂等解决了写入不重复的问题，事务解决了数据要么全部插入，要么没有插入(实际是插入，但是数据不可见)的问题，具体见**可靠性保障章节**。



- 异步不带回调demo  

```
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * reference:
 * https://kafka.apache.org/26/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html
 */


public class KafkaProducerDemo {
    private static final Logger logger = Logger.getGlobal();
    private static final Callback debugLog = (metadata, exception) -> {
        // 用户处理逻辑, 注意必须判定是否发生异常，不然会引发空指针异常
        if (exception == null) {
            logger.info(metadata.topic() + "   " + metadata.timestamp() + "   " + metadata.offset());
        } else {
            logger.info(exception.toString());
        }
    };

       /**
     * 连接初始化
     *
     * @param <K> key类型
     * @param <V> value 类型
     * @return 返回一个 Producer<k,v>对象
     */
    private static <K, V> Producer<K, V> iniProducer(Map<String, String> temp) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "test:9093");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        if (temp != null) temp.forEach((k, v) -> props.put(k, v));
        // 新建一个连接
        return new KafkaProducer<>(props);
    }

    /**
     * 异步插入，异步堵塞插入，异步带回调插入
     */
    public static void insertWithoutTransaction() throws ExecutionException, InterruptedException {
        // 新建一个连接
        Producer<String, String> producer = iniProducer(null);

        for (int i = 0; i < 100; i++) {
            // 每一个消息的发送必须通过new ProducerRecord<k,v > 的形式进行发送
            // ProducerRecord 必须包含: Topic Value;
            // ProducerRecord 中其他可选项为： key , partition , headers , timestamp
            ProducerRecord<String, String> record = new ProducerRecord<>("test", null, Integer.toString(i));

            // producer 支持多种传递语义： at most once ; at least once ; exactly once；语义的支持是通过幂等和事务实现的。
            // - 幂等相关
            // Kafka 2.6 版本, 集群默认配置为：enable.idempotence(开启幂等)
            // 注意开启幂等下 : retries 将会是无限大， ack: all , max.in.flight.requests.per.connection <=5（大于5会导致幂等失效）；
            // max.in.flight.requests.per.connection 为1 时候能保证输入顺序，但是性能差。
            // - 事务
            // 配置 transactional.id 将会自动启用事务和幂等特性，注意这里没有启用事务

            // 异步插入
            producer.send(record);
            // 异步堵塞插入(同步写入)
            producer.send(record).get();
            // 回调插入
            producer.send(record, debugLog);
        }
        producer.close();
    }
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        KafkaProducerDemo.insertWithoutTransaction();
        KafkaProducerDemo.insertByTransaction();
    }
}
```

- 同步(异步堵塞)

```
Future<RecordMetadata> send = producer.send(record);
// get是 java 中 fucture 的方法，会使得异步方法堵塞等待直到结果返回
send.get();
```

- 回调

```
 producer.send(record, new Callback() {
                // 假如发送过程中出现异常，exception 不会为空
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    // 用户处理逻辑
                    logger.info(metadata.toString());
                    logger.info(exception.getMessage());
                }
            });
```

- 事务开启

```
    public static void insertByTransaction() {
        Producer<String, String> producer = iniProducer(new HashMap<String, String>() {{
            put("transactional.id","my-transactional-id");
        }});

        producer.initTransactions();
        for (int i = 0; i < 100; i++) {

            ProducerRecord<String, String> record = new ProducerRecord<>("test", null, Integer.toString(i));
            producer.beginTransaction();
            producer.send(record, debugLog);
            // 注意不要逐条数据开启一个事务，否则性能会很差，这里只是演示
            producer.commitTransaction();
        }
        producer.close();
    }
```

## 3. 拦截器

- 拦截器定义

```
/**
 * 在每一条数据中添加本地ip
 * 统计累计成功失败次数
 */
public class MyKafkaInterceptor implements ProducerInterceptor<String, String> {


    AtomicLong succCou = new AtomicLong();
    AtomicLong failCou = new AtomicLong();

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        return new ProducerRecord(
                record.topic(), record.partition(), record.timestamp(), record.key(), System.currentTimeMillis() + "," + record.value().toString());
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            failCou.addAndGet(1);
        } else {
            succCou.addAndGet(1);
        }
        System.out.println("fail count" + failCou);
        System.out.println("success count" + succCou);
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
```

- 使用方法

```
 /**
     * 拦截器使用 demo
     */
    public static void ProducerInterceptor() {
        
        Producer<String, String> producer = iniProducer(new HashMap<String, Object>() {{
            put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, new ArrayList<String>() {{
                add("MyKafkaInterceptor");
            }});
        }});

        for (int i = 0; i < 100; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>("test", null, Integer.toString(i));
            // 异步插入
            producer.send(record);
        }
        producer.close();
}
```

## 4. 压缩

    压缩&解压流程：一般`Producer`负责压缩，`Broker`端保存数据，`Consumer`端负责压缩。

**注意事项：**

1. `Broker`默认没有配置压缩选项(default值为使用`Producer`提供的算法)。
2. 当`Producer`算法与`Broker`不一致，会导致`Broker`解压，再压缩，导致性能损失( Page Cache 失效)。 
3. `v1`与`v2`版本的消息集合，`v2`性能会更好。假如`V1`与`V2`共用，会引发版本转换导致性能下降。
4. 因为数据传输需要`CRC`校验完整性，校验**不需要解压**，校验没有使用`jni`方法，使用的是`Hadoop CRC32`java 实现(该实现比起`native`方法提高了在小文件下性能损耗)，配合`ByteBuffer`直接读取内存数据实现校验。

- 压缩声明方式：

> 未来 kafka 会引入 压缩等级配置的支持（3.2x 版本）：https://cwiki.apache.org/confluence/display/KAFKA/KIP-390%3A+Support+Compression+Level

compression.type:







