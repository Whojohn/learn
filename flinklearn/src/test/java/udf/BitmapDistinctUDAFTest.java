package udf;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.Test;

public class BitmapDistinctUDAFTest {


    @Test
    public void testBitmapDistinctUDAF() {
        System.out.println(System.currentTimeMillis());
        Configuration conf = new Configuration();
        conf.setInteger(RestOptions.PORT, 8082);
        conf.set(RestOptions.ENABLE_FLAMEGRAPH, true);
        conf.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("2g"));
//        mini-batch 可以减少序列化开销，提高20倍以上性能，针对该测试用例
        conf.setString("table.exec.mini-batch.enabled", "true");
        conf.setString("table.exec.mini-batch.allow-latency", "5s");
        conf.setString("table.exec.mini-batch.size", "2500000");
        conf.setString("table.optimizer.agg-phase-strategy", "TWO_PHASE");
        conf.setString("table.exec.state.changelog-mode.local-global", "true");
        conf.setString("classloader.parent-first-patterns.default","java");
//        conf.setString(SavepointConfigOptions.SAVEPOINT_PATH,
//                "file:///Users/whojohn/workspace/env/flink-1.17.2/data/savepoint-b83046-1471f3d37020");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(conf);
        env.setParallelism(1);
        env.enableCheckpointing(15000);
        env.setStateBackend(new HashMapStateBackend());
        env.getCheckpointConfig().setCheckpointStorage("file:///Users/whojohn/workspace/env/flink-1.17.2/data/checkpoint");
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);

        // Register the UDAF
        tEnv.createTemporarySystemFunction("bitmap_distinct", BitmapDistinctUDAF.class);

        // Use DataGen connector for testing
        tEnv.executeSql("CREATE TABLE sou (" +
                " id INT," +
                " category int" +
                ") WITH (" +
                " 'connector' = 'datagen'," +
                " 'rows-per-second' = '500000'," +
                " 'fields.category.min' = '1'," +
                " 'fields.category.max' = '1', " +
                " 'number-of-rows' = '1000000000' " +
                ")");

        tEnv.executeSql("CREATE TABLE sin (" +
                " category INT," +
                " distinct_cnt BIGINT" +
                ") WITH (" +
                " 'connector' = 'print'" +
                ")");

        String sql = "insert into sin SELECT category, bitmap_distinct(id) as distinct_cnt " +
                "FROM sou " +
                "GROUP BY category";
        tEnv.executeSql(sql).print();
        System.out.println(System.currentTimeMillis());
    }
}

