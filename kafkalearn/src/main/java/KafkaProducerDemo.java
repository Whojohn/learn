import org.apache.kafka.clients.producer.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    private static <K, V> Producer<K, V> iniProducer(Map<String, Object> temp) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "test:9093");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        if (temp != null) temp.forEach(props::put);
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

    /**
     * 事务提交样例
     */
    public static void insertByTransaction() {
        Producer<String, String> producer = iniProducer(new HashMap<String, Object>() {{
            put("transactional.id", "my-transactional-id");
            put("max.block.ms", "10000");
            put("transaction.timeout.ms", "10000");

        }});

        producer.initTransactions();

        for (int i = 0; i < 100; i++) {

            try {
                producer.beginTransaction();

                ProducerRecord<String, String> record = new ProducerRecord<>("test", null, Integer.toString(i));
                producer.send(record, debugLog);
                // 模拟事务时间超期
                //                Thread.sleep(12000);
                // 关闭 broker 模拟服务端失败
                //                Thread.sleep(2000);

                // 注意不要逐条数据开启一个事务，否则性能会很差，这里只是演示
                producer.commitTransaction();
            } catch (Exception e) {
                producer.abortTransaction();
            }
        }



        producer.close();
    }

    /**
     * 拦截器使用 demo
     */
    public static void ProducerInterceptor() {

        Producer<String, String> producer = iniProducer(new HashMap<String, Object>() {{
            put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, new ArrayList<String>() {{
                add("MyKafkaProducerInterceptor");
            }});
        }});

        for (int i = 0; i < 100; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>("test", null, Integer.toString(i));
            // 异步插入
            producer.send(record);
        }
        producer.close();
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        KafkaProducerDemo.insertWithoutTransaction();
//        KafkaProducerDemo.insertByTransaction();
//        KafkaProducerDemo.ProducerInterceptor();
    }
}

