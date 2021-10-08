import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaDownload {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = TestUtil.iniEnv(4);
        Properties kafkaProperties = new Properties();
        Map<String, String> config = new HashMap<String, String>() {{
            put("group.id", "test");
            put("security.protocol", "SASL_PLAINTEXT");
            put("sasl.mechanism", "PLAIN");
            put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }};


        config.keySet().forEach(e -> kafkaProperties.put(e, config.get(e)));
        env.addSource(new FlinkKafkaConsumer<>("facebook", new SimpleStringSchema(), kafkaProperties).setStartFromEarliest())
                .addSink(StreamingFileSink
                        .forRowFormat(new Path("hdfs://test:9000/test/facebook"), new SimpleStringEncoder<String>("UTF-8"))
                        .withRollingPolicy(
                                DefaultRollingPolicy.builder()
                                        .withRolloverInterval(TimeUnit.MINUTES.toMillis(2))
                                        .withInactivityInterval(TimeUnit.MINUTES.toMillis(2))
                                        .withMaxPartSize(1024 * 1024 * 1024)
                                        .build())
                        .build());
        env.execute();

    }
}
