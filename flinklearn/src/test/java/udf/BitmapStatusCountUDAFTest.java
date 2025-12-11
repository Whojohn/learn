package udf;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * BitmapStatusCountUDAF 测试
 * 使用 datagen 生成测试数据
 */
public class BitmapStatusCountUDAFTest {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        // 注册 UDAF
        tableEnv.createTemporarySystemFunction("count_status_by_sequence", BitmapStatusCountUDAF.class);

        // 创建 datagen 源表，模拟订单状态数据
        tableEnv.executeSql(
            "CREATE TABLE orders (\n" +
            "    order_id STRING,\n" +
            "    order_status INT,\n" +
            "    proc_time AS PROCTIME()\n" +
            ") WITH (\n" +
            "    'connector' = 'datagen',\n" +
            "    'rows-per-second' = '10',\n" +
            "    'fields.order_id.kind' = 'random',\n" +
            "    'fields.order_id.length' = '5',\n" +
            "    'fields.order_status.kind' = 'random',\n" +
            "    'fields.order_status.min' = '1',\n" +
            "    'fields.order_status.max' = '5'\n" +
            ")"
        );

        // 测试查询：按时间窗口统计各状态订单数
        // all_order_status = [1,2,3,4,5] 表示状态顺序：1->2->3->4->5
        String sql = 
            "SELECT \n" +
            "    TUMBLE_START(proc_time, INTERVAL '10' SECOND) AS window_start,\n" +
            "    count_status_by_sequence(order_id, order_status, ARRAY[1,2,3,4,5]) AS status_counts\n" +
            "FROM orders\n" +
            "GROUP BY TUMBLE(proc_time, INTERVAL '10' SECOND)";

        System.out.println("执行 SQL:\n" + sql + "\n");
        
        TableResult result = tableEnv.executeSql(sql);
        result.print();
    }
}

