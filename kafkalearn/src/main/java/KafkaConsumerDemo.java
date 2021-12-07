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
        consumer.close();
    }

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
        consumer.commitSync();
        consumer.close();
    }

    public static void main(String[] args) {
        // KafkaConsumerDemo.autoCommitConsumer();
        KafkaConsumerDemo.manualCommitConsumer();
    }
}
