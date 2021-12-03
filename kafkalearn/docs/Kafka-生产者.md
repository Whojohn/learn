# Kakfa-生产者

> reference:
> <<深入理解Kafka：核心涉及与实践原理>>
>
> https://kafka.apache.org/26/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html

## 生产者模型

![生产者客户端整理架构](https://img.luozhiyun.com/blog16949dd5a85b5fdf.png)



​     如上图所示，`Kafka`中客户端生成者有两个主要线程一个是调用`send`前的**用户主线程**，还有一个是负责发送的`Sender`线程。

- ProducerRecord

- KafkaProducer
- RecordAccumulator

- Serializer
- Partitioner 
- Sender



## 同步/异步写入&写入回调&事务写入

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
    private static <K, V> Producer<K, V> iniProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "test:9093");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 新建一个连接
        return new KafkaProducer<>(props);
    }

    /**
     * 异步插入，异步堵塞插入，异步带回调插入
     */
    public static void insertWithoutTransaction() throws ExecutionException, InterruptedException {
        // 新建一个连接
        Producer<String, String> producer = iniProducer();

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
 /**
     * 事务提交样例
     */
    public static void insertByTransaction() {
        Producer<String, String> producer = iniProducer();


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

## 拦截器

## 序列化器

## 分区器

## 消息累加器

