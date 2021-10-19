package state_test;

import env.TestUtil;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StateTTLTest extends ProcessFunction<Tuple2<String, Long>, Tuple2<String, Long>> {
    private transient ValueState<Tuple2<String, Long>> eachElement;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ValueStateDescriptor source = new ValueStateDescriptor("post_id", Types.TUPLE(Types.STRING, Types.LONG));
        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.seconds(60 * 60 * 2))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.ReturnExpiredIfNotCleanedUp)
                .cleanupIncrementally(100, false)
                .build();
        source.enableTimeToLive(ttlConfig);


        this.eachElement = getRuntimeContext().getState(source);
    }

    @Override
    public void processElement(Tuple2<String, Long> value, Context ctx, Collector<Tuple2<String, Long>> out) throws Exception {
        Tuple2<String, Long> temp = eachElement.value() == null ? new Tuple2<String, Long>() {{
            f0 = value.f0;
            f1 = 0L;
        }} : eachElement.value();
        temp.f1 = temp.f1 + 1;
        eachElement.clear();
        eachElement.update(temp);
        out.collect(temp);
    }


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = TestUtil.iniEnv(1);
        Properties kafkaProperties = new Properties();
        Map<String, String> config = new HashMap<String, String>() {{

            put("security.protocol", "SASL_PLAINTEXT");
            put("sasl.mechanism", "PLAIN");

            put("group.id", "test");
            put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }};


        config.keySet().forEach(e -> kafkaProperties.put(e, config.get(e)));
        env.addSource(new FlinkKafkaConsumer<>("news", new SimpleStringSchema(), kafkaProperties).setStartFromEarliest())
                .map(e -> {
                    Tuple2<String, Long> temp = new Tuple2<>();
                    temp.f1 = 0L;

                    try {
                        temp.f0 = new ObjectMapper().readTree(e).get("data").get("post_id").asText();
                    } catch (Exception e1) {
                        temp.f0 = "";
                    }
                    return temp;
                }).returns(Types.TUPLE(Types.STRING, Types.LONG))
                .keyBy(e -> e.f0)
                .process(new StateTTLTest());

        env.execute();
    }


}
