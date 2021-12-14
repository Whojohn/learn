import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

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
        consumer.close();
    }

    public static void manualCommitConsumer() throws InterruptedException {
        Consumer<String, String> consumer = iniConsumer(new HashMap<String, Object>() {{
            put("group.id", "test2");
            put("auto.offset.reset", "earliest");
            put("enable.auto.commit", "false");
            put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,"30000");
        }});
        consumer.subscribe(Collections.singletonList("test"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

        for (ConsumerRecord<String, String> record : records) {
            System.out.println(record.offset() + ": " + record.value());
            try {
                consumer.commitSync(new HashMap<TopicPartition, OffsetAndMetadata>() {{
                    put(new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1));
                }});
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }

        consumer.close();
    }

    public static void manualCommitExactlyOnceConsumer() throws InterruptedException {
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

    public static void main(String[] args) throws InterruptedException {
//        KafkaConsumerDemo.autoCommitConsumer();
//        KafkaConsumerDemo.manualCommitConsumer();
        KafkaConsumerDemo.manualCommitExactlyOnceConsumer();
    }
}
