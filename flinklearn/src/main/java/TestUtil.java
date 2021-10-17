import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;

import java.util.Properties;

public class TestUtil {


    public static StreamExecutionEnvironment iniEnv(int parall) {
        // 解决 hadoop simple auth
        Properties properties = System.getProperties();
        properties.setProperty("HADOOP_USER_NAME", "hadoop");

        Configuration conf = new Configuration();
        conf.setBoolean("rest.flamegraph.enabled", true);
        conf.setInteger(RestOptions.PORT, 8082);

//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(parall, conf);

        env.enableCheckpointing(10000);
        // 新版api 标识了 memory 状态量会被移除的信息，改用以下方式进行调用
        env.setStateBackend(new HashMapStateBackend());
//        env.getCheckpointConfig().setCheckpointStorage(new FileSystemCheckpointStorage("hdfs://test:9000/flink/test"));
        return env;
    }

    public static TypeInformation<Row> getReturnType() {
        String[] names = new String[]{"eve_time", "message"};
        TypeInformation[] types =
                new TypeInformation[]{Types.SQL_TIMESTAMP, Types.STRING};
        return Types.ROW_NAMED(names, types);
    }

}
