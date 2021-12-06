import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * - Kafka 生产者拦截器
 * 功能：
 * 在每一条数据中添加时间戳
 * 统计累计成功失败次数
 */
public class MyKafkaProducerInterceptor implements ProducerInterceptor<String, String> {


    AtomicLong succCou = new AtomicLong();
    AtomicLong failCou = new AtomicLong();

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        return new ProducerRecord<>(
                record.topic(), record.partition(), record.timestamp(), record.key(), System.currentTimeMillis() + "," + record.value());
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
