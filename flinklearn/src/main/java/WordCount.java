import env.TestUtil;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.StringValue;
import org.codehaus.jackson.map.ObjectMapper;

public class WordCount {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        // 访问http://localhost:8082 可以看到Flink Web UI
        conf.setInteger(RestOptions.PORT, 8082);
        // 创建本地执行环境，并行度为2
        StreamExecutionEnvironment env = TestUtil.iniEnv(2);


        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        env.setParallelism(1);
        env.readTextFile("hdfs://test:9000/paralli/news2016zh_train.json")
                .map(new MapFunction<String, Tuple2<StringValue, Integer>>() {
                    @Override
                    public Tuple2<StringValue, Integer> map(String value) throws Exception {
                        new ObjectMapper().readTree(value).get("");

                        return new Tuple2<StringValue, Integer>(new StringValue("a"),
                                new Integer(1));
                    }
                })
                .keyBy(0).reduce(new ReduceFunction<Tuple2<StringValue, Integer>>() {
            @Override
            public Tuple2<StringValue, Integer> reduce(Tuple2<StringValue, Integer> value1, Tuple2<StringValue, Integer> value2) throws Exception {
                return new Tuple2(value1.f0, value1.f1 + value2.f1);
            }
        })
                .print();
        env.execute();
    }
}
